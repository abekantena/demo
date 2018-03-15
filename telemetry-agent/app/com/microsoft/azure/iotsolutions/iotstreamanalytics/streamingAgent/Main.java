// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ConfigurationException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.runtime.Uptime;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.streaming.Stream;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.Logger;

import java.io.IOException;

public class Main {
    private static final Logger.ALogger log = Logger.of(Main.class);

    static Injector injector = Guice.createInjector(new Module());

    public static void main(String[] args) throws ConfigurationException, IOException {
        checkConfiguration();
        printBootstrapInfo();

        injector.getInstance(Stream.class).Run();
    }

    private static void printBootstrapInfo() {
        Config conf = ConfigFactory.load();

        String hubName = conf.getString("iothub-react.connection.hubName");
        String hubEndpoint = conf.getString("iothub-react.connection.hubEndpoint");
        String hubPartitions = conf.getString("iothub-react.connection.hubPartitions");
        String cpBackendType = conf.getString("iothub-react.checkpointing.storage.backendType");
        String cpNamespace = conf.getString("iothub-react.checkpointing.storage.namespace");
        String cpFrequency = conf.getString("iothub-react.checkpointing.frequency");

        log.info("Streaming agent started, ProcessId {}", Uptime.getProcessId());
        log.info("IoT Hub name: {}", hubName);
        log.info("IoT Hub endpoint: {}", hubEndpoint);
        log.info("IoT Hub partitions: {}", hubPartitions);
        log.info("Checkpointing storage: {}", cpBackendType);
        log.info("Checkpointing namespace: {}", cpNamespace);
        log.info("Checkpointing min frequency: {}", cpFrequency);
    }

    private static void checkConfiguration() throws ConfigurationException {
        Config conf = ConfigFactory.load();

        String hubName = conf.getString("iothub-react.connection.hubName");
        String hubEndpoint = conf.getString("iothub-react.connection.hubEndpoint");
        String hubPartitions = conf.getString("iothub-react.connection.hubPartitions");
        String cpBackendType = conf.getString("iothub-react.checkpointing.storage.backendType");
        String cpNamespace = conf.getString("iothub-react.checkpointing.storage.namespace");

        if (hubName.isEmpty()) {
            throw new ConfigurationException("Azure IoT Hub name not found in the configuration.");
        }

        if (hubEndpoint.isEmpty()) {
            throw new ConfigurationException("Azure IoT Hub endpoint not found in the configuration.");
        }

        if (hubPartitions.isEmpty()) {
            throw new ConfigurationException("Azure IoT Hub partition number not found in the configuration.");
        }

        if (cpNamespace.isEmpty()) {
            throw new ConfigurationException("Checkpointing namespace not found in the configuration.");
        }

        if (cpBackendType.toLowerCase() == "cosmosdbsql") {
            String cs = conf.getString("iothub-react.checkpointing.storage.cosmosdbsql.connString");
            if (cs.isEmpty()) {
                throw new ConfigurationException("CosmosDb connection string not found in the configuration.");
            }
        }
    }
}
