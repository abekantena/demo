// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class DeviceListApiModel {

    private ArrayList<DeviceApiModel> items;
    private String continuationToken;

    public DeviceListApiModel(){
        this.items = new ArrayList<>();
    }

    @JsonProperty("Items")
    public ArrayList<DeviceApiModel> getItems() {
        return this.items;
    }

    public void setItems(ArrayList<DeviceApiModel> items) {
        this.items = items;
    }

    @JsonProperty("ContinuationToken")
    public String getContinuationToken() {
        return this.continuationToken;
    }

    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }
}
