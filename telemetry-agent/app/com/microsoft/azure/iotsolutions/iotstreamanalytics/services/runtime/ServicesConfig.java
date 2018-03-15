// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime;

public class ServicesConfig implements IServicesConfig {

    private final String monitoringRulesUrl;
    private final String deviceGroupsUrl;
    private final String devicesUrl;
    private final StorageConfig messagesConfig;
    private final StorageConfig alarmsConfig;

    public ServicesConfig(
        final String monitoringRulesUrl,
        final String deviceGroupsUrl,
        final String devicesUrl,
        StorageConfig messagesConfig,
        StorageConfig alarmsConfig) {
        this.monitoringRulesUrl = monitoringRulesUrl;
        this.deviceGroupsUrl = deviceGroupsUrl;
        this.devicesUrl = devicesUrl;
        this.messagesConfig = messagesConfig;
        this.alarmsConfig = alarmsConfig;
    }

    public String getMonitoringRulesUrl() {
        return this.monitoringRulesUrl;
    }

    public String getDeviceGroupsUrl() {
        return this.deviceGroupsUrl;
    }

    public String getDevicesUrl() {
        return this.devicesUrl;
    }

    public StorageConfig getMessagesStorageConfig() {
        return this.messagesConfig;
    }

    public StorageConfig getAlarmsStorageConfig() {
        return this.alarmsConfig;
    }
}
