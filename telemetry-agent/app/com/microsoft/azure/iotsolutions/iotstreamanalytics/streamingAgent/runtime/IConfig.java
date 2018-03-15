// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.runtime;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;

@ImplementedBy(Config.class)
public interface IConfig {

    /**
     * Number of partitions that can be streamed independently
     */
    int getStreamPartitionsCount();

    /**
     * Service layer configuration
     */
    IServicesConfig getServicesConfig();
}
