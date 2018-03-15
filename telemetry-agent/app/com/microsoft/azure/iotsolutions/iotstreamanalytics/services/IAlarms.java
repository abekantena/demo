// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;

@ImplementedBy(Alarms.class)
public interface IAlarms {
    void process(RawMessage message) throws ExternalDependencyException;
    void reloadRules();
}
