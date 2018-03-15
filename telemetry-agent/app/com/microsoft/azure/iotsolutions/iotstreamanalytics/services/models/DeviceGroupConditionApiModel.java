// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class DeviceGroupConditionApiModel {
    private String key;
    private String operator;
    private String value;

    @JsonProperty("Key")
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @JsonProperty("Operator")
    public String getOperator() {
        return this.operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @JsonProperty("Value")
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
