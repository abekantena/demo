// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Model used by the config service when returning Rules
 * see: https://github.com/Azure/pcs-config-java/wiki
 */
public class DeviceGroupApiModel {

    private String id;
    private String displayName;
    private ArrayList<DeviceGroupConditionApiModel> conditions;

    public DeviceGroupApiModel() {
        this.conditions = new ArrayList<>();
    }

    @JsonProperty("Id")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("DisplayName")
    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("Conditions")
    public ArrayList<DeviceGroupConditionApiModel> getConditions() {
        return this.conditions;
    }

    public void setConditions(ArrayList<DeviceGroupConditionApiModel> conditions) {
        this.conditions = conditions;
    }
}
