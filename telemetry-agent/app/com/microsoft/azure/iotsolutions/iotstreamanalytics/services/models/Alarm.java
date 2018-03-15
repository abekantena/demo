// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import org.joda.time.DateTime;

public final class Alarm {
    private final String id;
    private final DateTime dateCreated;
    private final DateTime dateModified;
    private final long messageReceivedTime;
    private final String description;
    private final String deviceId;
    private final String status;
    private final String ruleId;
    private final String ruleSeverity;
    private final String ruleDescription;

    public Alarm(
        final String id,
        final DateTime dateCreated,
        final DateTime dateModified,
        final long messageReceivedTime,
        final String description,
        final String deviceId,
        final String status,
        final String ruleId,
        final String ruleSeverity,
        final String ruleDescription
    ) {
        this.id = id;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.messageReceivedTime = messageReceivedTime;
        this.description = description;
        this.deviceId = deviceId;
        this.status = status;
        this.ruleId = ruleId;
        this.ruleSeverity = ruleSeverity;
        this.ruleDescription = ruleDescription;
    }

    public String getId() {
        return this.id;
    }

    public DateTime getDateCreated() {
        return this.dateCreated;
    }

    public DateTime getDateModified() {
        return this.dateModified;
    }

    public long getMessageReceivedTime() {
        return this.messageReceivedTime;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getStatus() {
        return this.status;
    }

    public String getRuleId() {
        return this.ruleId;
    }

    public String getRuleSeverity() {
        return this.ruleSeverity;
    }

    public String getRuleDescription() {
        return this.ruleDescription;
    }
}
