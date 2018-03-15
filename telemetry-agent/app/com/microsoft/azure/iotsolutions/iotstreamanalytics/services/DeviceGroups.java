// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.fasterxml.jackson.databind.*;
import com.google.inject.Inject;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.DeviceGroupApiModel;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static play.mvc.Http.Status.OK;

public class DeviceGroups implements IDeviceGroups {

    private static final Logger.ALogger log = Logger.of(DeviceGroups.class);

    private final WSClient wsClient;
    private final String url;
    private final IDevices devices;
    private final ObjectMapper jsonMapper;

    @Inject
    public DeviceGroups(
        final WSClient wsClient,
        final IDevices devices,
        final IServicesConfig config) {

        this.wsClient = wsClient;
        this.devices = devices;
        this.url = config.getDeviceGroupsUrl() + "/devicegroups";

        // Required for case insensitive JSON parsing
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.jsonMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        this.jsonMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
    }

    @Override
    public void finalize() {

        /**
         * Note: If you create a WSClient manually then you must call
         * client.close() to clean it up when you’ve finished with it. Each
         * client creates its own thread pool. If you fail to close the client
         * or if you create too many clients then you will run out of threads
         * or file handles - you’ll get errors like “Unable to create new
         * native thread” or “too many open files” as the underlying resources
         * are consumed.
         */

        try {
            this.wsClient.close();
        } catch (Exception e) {
            // ignore, no need to log, the client might have been closed
            // or disposed already
        }
    }

    public Set<String> getDevices(String groupId)
        throws ExternalDependencyException {

        try {
            DeviceGroupApiModel group = this.getDeviceGroup(groupId);
            Set<String> list = this.devices.getList(group.getConditions());
            log.debug("Group {} loaded, {} devices found", groupId, list.size());
            return list;
        } catch (InterruptedException e) {
            throw new ExternalDependencyException("Operation interrupted", e);
        } catch (Exception e) {
            throw new ExternalDependencyException("Unable to get list of devices", e);
        }
    }

    private DeviceGroupApiModel getDeviceGroup(String groupId)
        throws ExecutionException, InterruptedException {

        String url = this.url + "/" + groupId;
        return this.prepareRequest(url)
            .get()
            .toCompletableFuture()
            .handleAsync((result, error) -> {
                if (error != null) {
                    log.error("Request to {} failed: {}",
                        url, error.getMessage());
                    throw new CompletionException(
                        new ExternalDependencyException(error.getMessage()));
                }

                if (result.getStatus() != OK) {
                    log.error("Request to {} failed with status code {}",
                        url, result.getStatus());
                    throw new CompletionException(
                        new ExternalDependencyException("Unable to load device groups"));
                }

                try {
                    String body = result.getBody();
                    DeviceGroupApiModel deviceGroup = this.jsonMapper.readValue(body, DeviceGroupApiModel.class);
                    return deviceGroup;
                } catch (Exception e) {
                    log.error("Could not parse result from {}: {}",
                        url, e.getMessage());
                    throw new CompletionException(
                        new ExternalDependencyException(
                            "Could not parse result from " + url));
                }
            }).get();
    }

    private WSRequest prepareRequest(String url) {
        return wsClient.url(url)
            .addHeader("Accept", "application/json")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("User-Agent", "Telemetry Agent " + this.getClass().getTypeName())
            .setRequestTimeout(Duration.of(10, ChronoUnit.SECONDS));
    }
}
