// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.DeviceGroupConditionApiModel;

import java.util.ArrayList;
import java.util.Set;

@ImplementedBy(Devices.class)
public interface IDevices {
    Set<String> getList(ArrayList<DeviceGroupConditionApiModel> conditions)
        throws ExternalDependencyException;
}
