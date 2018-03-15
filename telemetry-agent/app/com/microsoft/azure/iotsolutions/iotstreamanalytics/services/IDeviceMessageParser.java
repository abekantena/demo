// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iot.iothubreact.MessageFromDevice;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;

@ImplementedBy(DeviceMessageParser.class)
public interface IDeviceMessageParser {
    RawMessage messageToRawMessage(MessageFromDevice m);
}
