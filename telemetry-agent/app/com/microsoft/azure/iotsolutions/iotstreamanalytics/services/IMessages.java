// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iot.iothubreact.MessageFromDevice;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;

@ImplementedBy(Messages.class)
public interface IMessages {
    void process(MessageFromDevice m) throws ExternalDependencyException;
    void refreshLogic();
}
