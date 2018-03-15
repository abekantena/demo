// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.streaming;

import com.google.inject.ImplementedBy;

@ImplementedBy(Stream.class)
public interface IStream {
    void Run();
}
