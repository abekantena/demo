// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model used by the Device Telemetry service when returning Rules
 * see: https://github.com/Azure/device-telemetry-java/wiki
 */
public final class ConditionApiModel {

    private String field;
    private String operator;
    private String value;

    @JsonProperty("Field")
    public String getField() {
        return this.field;
    }

    private void setField(String field) {
        this.field = field;
    }

    @JsonProperty("Operator")
    public String getOperator() {
        return this.operator;
    }

    private void setOperator(String operator) {
        this.operator = operator;
    }

    @JsonProperty("Value")
    public String getValue() {
        return this.value;
    }

    private void setValue(String value) {
        this.value = value;
    }
}
