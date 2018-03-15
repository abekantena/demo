// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageConfig {

    private final String storageType;
    private final String documentDbConnString;
    private final String documentDbDatabase;
    private final String documentDbCollection;
    private final int documentDbRUs;

    public StorageConfig(
        String storageType,
        String documentDbConnString,
        String documentDbDatabase,
        String documentDbCollection,
        int documentDbRUs) {
        this.storageType = storageType;
        this.documentDbConnString = documentDbConnString;
        this.documentDbDatabase = documentDbDatabase;
        this.documentDbCollection = documentDbCollection;
        this.documentDbRUs = documentDbRUs;
    }

    public String getStorageType() {
        return this.storageType;
    }

    public String getDocumentDbConnString() {
        return this.documentDbConnString;
    }

    public String getDocumentDbDatabase() {
        return this.documentDbDatabase;
    }

    public String getDocumentDbCollection() {
        return this.documentDbCollection;
    }

    public int getDocumentDbRUs() {
        return this.documentDbRUs;
    }

    public String getDocumentDbUri() {
        Pattern pattern = Pattern.compile(".*AccountEndpoint=(.*);.*");
        Matcher matcher = pattern.matcher(this.getDocumentDbConnString());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "https://ENDPOINT-NOT-FOUND.documents.azure.com:443/";
        }
    }

    public String getDocumentDbKey() {
        Pattern pattern = Pattern.compile(".*AccountKey=(.*);");
        Matcher matcher = pattern.matcher(this.getDocumentDbConnString());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }
}
