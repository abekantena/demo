// Copyright (c) Microsoft. All rights reserved

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.Inject;
import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ConfigurationException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.StorageConfig;
import play.Logger;

public class StorageClient implements IStorageClient {

    private static final Logger.ALogger log = Logger.of(StorageClient.class);

    private final String documentDbUri;
    private final String documentDbKey;
    private final int docDbRUs;
    private final RequestOptions docDbOptions;

    private static DocumentClient docDbConnection = null;

    @Inject
    public StorageClient(final IServicesConfig config) throws Exception {

        final StorageConfig storageConfig = config.getMessagesStorageConfig();

        this.documentDbUri = storageConfig.getDocumentDbUri();
        this.documentDbKey = storageConfig.getDocumentDbKey();
        this.docDbRUs = storageConfig.getDocumentDbRUs();
        this.docDbOptions = this.getDocDbOptions();

        this.docDbConnection = getDocumentClient();
    }

    // returns existing document client, creates document client if null
    public DocumentClient getDocumentClient() throws ConfigurationException {

        if (this.docDbConnection == null) {
            ConnectionPolicy connectionPolicy = new ConnectionPolicy();
            connectionPolicy.setConnectionMode(ConnectionMode.DirectHttps);
            connectionPolicy.setMaxPoolSize(1000);

            this.docDbConnection = new DocumentClient(
                this.documentDbUri,
                this.documentDbKey,
                connectionPolicy,
                ConsistencyLevel.Eventual);

            if (this.docDbConnection == null) {
                log.error("Could not connect to DocumentClient");
                throw new ConfigurationException("Could not connect to DocumentClient");
            }
        }

        return this.docDbConnection;
    }

    @Override
    // returns true is storage connection is healthy, false otherwise
    public Boolean ping() {
        if (this.docDbConnection != null) {

            String link = String.format("/dbs/test");
            try {
                this.docDbConnection.readDocument(link, docDbOptions);
            } catch (DocumentClientException e) {
                log.error("Could not reach storage service" +
                    "Check connection string", e);
                return false;
            }

        }
        return true;
    }

    private RequestOptions getDocDbOptions() {
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setOfferThroughput(this.docDbRUs);
        requestOptions.setConsistencyLevel(ConsistencyLevel.Eventual);
        return requestOptions;
    }
}
