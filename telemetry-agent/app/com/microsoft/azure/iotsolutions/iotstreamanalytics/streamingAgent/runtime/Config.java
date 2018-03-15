// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.runtime;

import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.*;
import com.typesafe.config.ConfigFactory;

public class Config implements IConfig {

    // Namespace applied to all the custom configuration settings
    private final String Namespace = "com.microsoft.azure.iotsolutions.";

    // Settings about this application
    private final String ApplicationKey = Namespace + "telemetry-agent.";

    private final String monitoringRulesUrlKey = ApplicationKey + "monitoringRulesUrl";
    private final String deviceGroupsUrlKey = ApplicationKey + "deviceGroupsUrl";
    private final String devicesUrlKey = ApplicationKey + "devicesUrl";
    private final String partitionsCountKey = ApplicationKey + "streamPartitions";

    private final String messagesStorageTypeKey = ApplicationKey + "messages.storageType";
    private final String messagesDocDbConnStringKey = ApplicationKey + "messages.documentDb.connString";
    private final String messagesDocDbDatabaseKey = ApplicationKey + "messages.documentDb.database";
    private final String messagesDocDbCollectionKey = ApplicationKey + "messages.documentDb.collection";
    private final String messagesDocDbRUsKey = ApplicationKey + "messages.documentDb.RUs";

    private final String alarmsStorageTypeKey = ApplicationKey + "alarms.storageType";
    private final String alarmsDocDbConnStringKey = ApplicationKey + "alarms.documentDb.connString";
    private final String alarmsDocDbDatabaseKey = ApplicationKey + "alarms.documentDb.database";
    private final String alarmsDocDbCollectionKey = ApplicationKey + "alarms.documentDb.collection";
    private final String alarmsDocDbRUsKey = ApplicationKey + "alarms.documentDb.RUs";

    private IServicesConfig servicesConfig;
    private com.typesafe.config.Config data;
    private int partitionsCount;

    public Config() {
        this.data = ConfigFactory.load();
        this.partitionsCount = this.data.getInt(partitionsCountKey);
    }

    /**
     * Number of partitions that can be streamed independently
     */
    public int getStreamPartitionsCount() {
        return this.partitionsCount;
    }

    /**
     * Service layer configuration
     */
    @Override
    public IServicesConfig getServicesConfig() {

        if (this.servicesConfig != null) return this.servicesConfig;

        StorageConfig messagesConfig = new StorageConfig(
            this.data.getString(messagesStorageTypeKey).toLowerCase(),
            this.data.getString(messagesDocDbConnStringKey),
            this.data.getString(messagesDocDbDatabaseKey),
            this.data.getString(messagesDocDbCollectionKey),
            this.data.getInt(messagesDocDbRUsKey));

        StorageConfig alarmsConfig = new StorageConfig(
            this.data.getString(alarmsStorageTypeKey).toLowerCase(),
            this.data.getString(alarmsDocDbConnStringKey),
            this.data.getString(alarmsDocDbDatabaseKey),
            this.data.getString(alarmsDocDbCollectionKey),
            this.data.getInt(alarmsDocDbRUsKey));

        this.servicesConfig = new ServicesConfig(
            this.data.getString(monitoringRulesUrlKey),
            this.data.getString(deviceGroupsUrlKey),
            this.data.getString(devicesUrlKey),
            messagesConfig,
            alarmsConfig);

        return this.servicesConfig;
    }
}
