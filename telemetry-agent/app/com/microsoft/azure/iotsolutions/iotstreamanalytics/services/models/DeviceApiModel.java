// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceApiModel {
    private String id;

    @JsonProperty("Id")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
