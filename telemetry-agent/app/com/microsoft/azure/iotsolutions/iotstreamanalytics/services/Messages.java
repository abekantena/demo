// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.Inject;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.iot.iothubreact.MessageFromDevice;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.StorageConfig;
import play.Logger;

import java.util.ArrayList;
import java.util.concurrent.CompletionException;

// TODO: decouple the class from DocumentDb:
//  * use a generic storage interface, e.g. allow using Cassandra
//  * wrap exception in generic storage exceptions
// https://github.com/Azure/telemetry-agent-java/issues/20
// TODO: resilience to errors and throttling
public class Messages implements IMessages {

    private static final Logger.ALogger log = Logger.of(Messages.class);

    private final IDeviceMessageParser messageParser;
    private final IAlarms alarms;

    private final DocumentClient docDbConnection;
    private final String docDbDatabase;
    private final String docDbCollection;
    private final int docDbRUs;
    private final RequestOptions docDbOptions;

    @Inject
    public Messages(
        IServicesConfig config,
        IStorageClient storageClient,
        IDeviceMessageParser messageParser,
        IAlarms alarms) throws DocumentClientException {

        final StorageConfig storageConfig = config.getMessagesStorageConfig();

        this.messageParser = messageParser;
        this.alarms = alarms;

        try {
            this.docDbConnection = storageClient.getDocumentClient();
        } catch (Exception e) {
            log.error("Could not connect to DocumentClient");
            throw new CompletionException(e);
        }

        this.docDbDatabase = storageConfig.getDocumentDbDatabase();
        this.docDbCollection = storageConfig.getDocumentDbCollection();
        this.docDbRUs = storageConfig.getDocumentDbRUs();
        this.docDbOptions = this.getDocDbOptions();

        this.createDatabaseIfNotExists();
        this.createCollectionIfNotExists();
    }

    @Override
    public void process(MessageFromDevice m)
        throws ExternalDependencyException {

        log.debug("Saving message...");
        RawMessage msg = this.saveMessage(m);

        log.debug("Analyzing message...");
        this.alarms.process(msg);
    }

    @Override
    public void refreshLogic() {
        this.alarms.reloadRules();
    }

    private RawMessage saveMessage(MessageFromDevice m) {

        RawMessage message = this.messageParser.messageToRawMessage(m);
        Document doc = message.toDocument();

        String collectionLink = String.format("/dbs/%s/colls/%s",
            this.docDbDatabase, this.docDbCollection);
        try {
            /**
             * create document then request resource to close http stream
             *
             * The ResourceResponse implements AutoCloseable so the close()
             * method has been deprecated. However, the service floods the http
             * stream with requests before they can auto-close, so we can
             * call an API that will close the request, one API that calls close
             * is getResource.
             *
             * See getResource() method in the DocumentClient project
             * at https://github.com/Azure/azure-documentdb-java/
             */
            this.docDbConnection.createDocument(
                collectionLink,
                doc,
                this.docDbOptions,
                true)
                .getResource();

        } catch (DocumentClientException e) {
            // if the status is 409, it means the message has already been written
            // to storage, so we can ignore the conflict error. Log all other errors.
            if (e.getStatusCode() != 409) {
                log.error("Error while writing message", e);
                // TODO: fix, otherwise message gets lost. When the service
                // fails to write the message to storage, it should either retry
                // or stop processing the following messages. The current behavior
                // of logging the error and moving on, means that in case of
                // error, the message has not been stored.
                // see https://github.com/Azure/telemetry-agent-java/issues/35
            }
        } catch (Exception e) {
            log.error("Error while writing message", e);
            // TODO: fix, otherwise message gets lost.
            // see https://github.com/Azure/telemetry-agent-java/issues/35
        }

        return message;
    }

    private void createDatabaseIfNotExists()
        throws DocumentClientException {
        String databaseLink = String.format("/dbs/%s", this.docDbDatabase);
        try {
            this.docDbConnection.readDatabase(databaseLink, this.docDbOptions);
        } catch (DocumentClientException e) {
            if (e.getStatusCode() != 404) {
                log.error("Error while getting DocumentDb database", e);
                throw e;
            }

            this.createDatabase();
        } catch (Exception e) {
            log.error("Error while getting DocumentDb database", e);
            throw e;
        }
    }

    private void createCollectionIfNotExists()
        throws DocumentClientException {

        String collectionLink = String.format("/dbs/%s/colls/%s",
            this.docDbDatabase, this.docDbCollection);

        try {
            this.docDbConnection.readCollection(collectionLink, this.docDbOptions);
            return;
        } catch (DocumentClientException e) {
            if (e.getStatusCode() != 404) {
                log.error("Error while getting DocumentDb collection", e);
                throw e;
            }

            this.createCollection();
        } catch (Exception e) {
            log.error("Error while getting DocumentDb collection", e);
            throw e;
        }
    }

    private void createDatabase() throws DocumentClientException {
        try {
            log.info("Creating DocumentDb database: {}", this.docDbDatabase);
            Database database = new Database();
            database.setId(this.docDbDatabase);
            this.docDbConnection.createDatabase(database, this.docDbOptions);
        } catch (Exception e) {
            log.error("Error while creating DocumentDb database", e);
            throw e;
        }
    }

    private void createCollection() throws DocumentClientException {
        try {
            log.info("Creating DocumentDb collection: {}", this.docDbCollection);

            DocumentCollection info = new DocumentCollection();
            info.setId(this.docDbCollection);

            RangeIndex index = Index.Range(DataType.String, -1);
            IndexingPolicy indexing = new IndexingPolicy(new Index[]{index});
            indexing.setIndexingMode(IndexingMode.Consistent);
            //indexing.setIncludedPaths(new ArrayList<IncludedPath>() {{
            //    add(new IncludedPath("/device/id"));
            //    add(new IncludedPath("/doc/schema"));
            //    add(new IncludedPath("/doc/schemaVersion"));
            //    add(new IncludedPath("/device/msg/schema"));
            //    add(new IncludedPath("/device/msg/created"));
            //    add(new IncludedPath("/device/msg/received"));
            //}});
            info.setIndexingPolicy(indexing);

            PartitionKeyDefinition partitionKeyDefinition = new PartitionKeyDefinition();
            partitionKeyDefinition.setPaths(new ArrayList<String>() {{
                add("/id");
            }});
            info.setPartitionKey(partitionKeyDefinition);

            String databaseLink = String.format("/dbs/%s", this.docDbDatabase);

            this.docDbConnection.createCollection(databaseLink, info, this.docDbOptions);
        } catch (DocumentClientException e) {
            if (e.getStatusCode() != 409) {
                log.error("Error while getting DocumentDb collection", e);
                throw e;
            }

            log.warn("Another process already created the collection", e);
        } catch (Exception e) {
            log.error("Error while creating DocumentDb collection", e);
            throw e;
        }
    }

    private RequestOptions getDocDbOptions() {
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setOfferThroughput(this.docDbRUs);
        // For telemetry patterns, the Eventual consistency is ok
        requestOptions.setConsistencyLevel(ConsistencyLevel.Eventual);
        return requestOptions;
    }
}
