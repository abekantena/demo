// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;

import java.util.Set;

@ImplementedBy(DeviceGroups.class)
public interface IDeviceGroups {
    Set<String> getDevices(String groupId) throws ExternalDependencyException;
}
