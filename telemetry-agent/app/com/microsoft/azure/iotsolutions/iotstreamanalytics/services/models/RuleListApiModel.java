// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Model used by the Device Telemetry service when returning Rules
 * see: https://github.com/Azure/device-telemetry-java/wiki
 */
public class RuleListApiModel {

    private ArrayList<RuleApiModel> items;

    public RuleListApiModel() {
        this.items = new ArrayList<>();
    }

    @JsonProperty("Items")
    public ArrayList<RuleApiModel> getItems() {
        return this.items;
    }

    private void setItems(ArrayList<RuleApiModel> items) {
        this.items = items;
    }
}
