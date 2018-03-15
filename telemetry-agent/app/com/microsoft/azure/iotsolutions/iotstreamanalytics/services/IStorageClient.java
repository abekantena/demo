// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ConfigurationException;

@ImplementedBy(StorageClient.class)
public interface IStorageClient {
    DocumentClient getDocumentClient() throws ConfigurationException;

    Boolean ping();
}
