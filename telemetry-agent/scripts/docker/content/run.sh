#!/usr/bin/env bash

cd /app/

bin/telemetry-agent & \
    bin/telemetry-agent -main com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.Main && \
    fg
