// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.Inject;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.Alarm;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RuleApiModel;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.StorageConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletionException;

// TODO: decouple the class from DocumentDb:
//  * use a generic storage interface, e.g. allow using Cassandra
//  * wrap exception in generic storage exceptions
// https://github.com/Azure/telemetry-agent-java/issues/20
// TODO: resilience to errors and throttling
public class Alarms implements IAlarms {

    private static final Logger.ALogger log = Logger.of(Alarms.class);

    private final DocumentClient docDbConnection;
    private final String docDbDatabase;
    private final String docDbCollection;
    private final int docDbRUs;
    private final RequestOptions docDbOptions;
    private final IRules rules;
    private final IRulesEvaluation rulesEvaluation;

    private ArrayList<RuleApiModel> monitoringRules;

    private final String DOC_SCHEMA_KEY = "doc.schema";
    private final String DOC_SCHEMA_VALUE = "alarm";

    private final String DOC_SCHEMA_VERSION_KEY = "doc.schemaVersion";
    private final int DOC_SCHEMA_VERSION_VALUE = 1;

    private final String CREATED_KEY = "created";
    private final String MODIFIED_KEY = "modified";
    private final String DESCRIPTION_KEY = "description";
    private final String STATUS_KEY = "status";
    private final String DEVICE_ID_KEY = "device.id";
    private final String MESSAGE_RECEIVED_TIME = "device.msg.received";

    private final String RULE_ID_KEY = "rule.id";
    private final String RULE_SEVERITY_KEY = "rule.severity";
    private final String RULE_DESCRIPTION_KEY = "rule.description";

    // TODO: https://github.com/Azure/telemetry-agent-java/issues/34
    private final String NEW_ALARM_STATUS = "open";

    @Inject
    public Alarms(
        IRules rules,
        IRulesEvaluation rulesEvaluation,
        IServicesConfig config,
        IStorageClient storageClient) throws DocumentClientException {

        this.rules = rules;
        this.rulesEvaluation = rulesEvaluation;
        final StorageConfig storageConfig = config.getAlarmsStorageConfig();

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

        this.monitoringRules = new ArrayList<>();
        this.loadAllRules();
    }

    @Override
    public void process(RawMessage message)
        throws ExternalDependencyException {

        for (RuleApiModel rule : this.monitoringRules) {
            log.debug("Evaluating rule {} for device {}", rule.getDescription(), message.getDeviceId());
            IRulesEvaluation.RulesEvaluationResult result = this.rulesEvaluation.evaluate(rule, message);
            if (result.match) {
                log.info("Alarm! " + result.message);
                this.createAlarm(rule, message, result.message);
            }
        }
    }

    @Override
    public void reloadRules() {
        this.loadAllRules();
    }

    private void createAlarm(
        RuleApiModel rule,
        RawMessage deviceMessage,
        String alarmDescription) {

        String alarmId = UUID.randomUUID().toString();
        DateTime created = DateTime.now(DateTimeZone.UTC);

        Alarm alarm = new Alarm(
            alarmId,
            created,
            created,
            deviceMessage.getReceivedTime(),
            alarmDescription,
            deviceMessage.getDeviceId(),
            NEW_ALARM_STATUS,
            rule.getId(),
            rule.getSeverity(),
            rule.getDescription());

        this.saveAlarm(alarm);
    }

    private void loadAllRules() {

        log.debug("Loading rules...");
        this.monitoringRules.clear();

        this.rules.getAllAsync()
            .handle(
                (result, error) -> {
                    if (error != null) {
                        log.error("Unable to load monitoring rules");
                    } else {
                        this.monitoringRules = result;
                        log.info("Monitoring rules loaded: {} rules", result.size());
                    }

                    return true;
                })
            .toCompletableFuture()
            .join();
    }

    private void saveAlarm(Alarm alarm) {
        Document doc = this.alarmToDocument(alarm);

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
        } catch (Exception e) {
            log.error("Error while writing alarm: " + doc.toJson(), e);
        }
    }

    private Document alarmToDocument(Alarm alarm) {

        Document document = new Document();

        // TODO: make inserts idempotent, e.g. gen Id from msg details
        document.setId(UUID.randomUUID().toString());

        document.set(DOC_SCHEMA_KEY, DOC_SCHEMA_VALUE);
        document.set(DOC_SCHEMA_VERSION_KEY, DOC_SCHEMA_VERSION_VALUE);
        document.set(CREATED_KEY, alarm.getDateCreated().getMillis());
        document.set(MODIFIED_KEY, alarm.getDateModified().getMillis());
        document.set(STATUS_KEY, alarm.getStatus());
        document.set(DESCRIPTION_KEY, alarm.getDescription());
        document.set(RULE_SEVERITY_KEY, alarm.getRuleSeverity());
        document.set(RULE_DESCRIPTION_KEY, alarm.getRuleDescription());

        // The logic used to generate the alarm (future proofing for ML)
        document.set("logic", "1Rule-1Device-1Message");
        document.set(RULE_ID_KEY, alarm.getRuleId());
        document.set(DEVICE_ID_KEY, alarm.getDeviceId());
        document.set(MESSAGE_RECEIVED_TIME, alarm.getMessageReceivedTime());

        return document;
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
        requestOptions.setConsistencyLevel(ConsistencyLevel.Eventual);
        return requestOptions;
    }
}
