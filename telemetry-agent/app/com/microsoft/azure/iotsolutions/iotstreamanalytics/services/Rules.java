// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.Inject;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RuleApiModel;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RuleListApiModel;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static play.mvc.Http.Status.OK;

public class Rules implements IRules {

    private static final Logger.ALogger log = Logger.of(Rules.class);

    private final WSClient wsClient;
    private final String rulesUrl;

    @Inject
    public Rules(
        final WSClient wsClient,
        final IServicesConfig config) {
        this.wsClient = wsClient;
        this.rulesUrl = config.getMonitoringRulesUrl() + "/rules";
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

    @Override
    public CompletionStage<ArrayList<RuleApiModel>> getAllAsync() {

        return this.prepareRequest("")
            .get()
            .handleAsync((result, error) -> {
                if (error != null) {
                    log.error("Request to {} failed: {}",
                        this.rulesUrl, error.getMessage());
                    throw new CompletionException(
                        new ExternalDependencyException(error.getMessage()));
                }

                if (result.getStatus() != OK) {
                    log.error("Request to {} failed with status code {}",
                        this.rulesUrl, result.getStatus());
                    throw new CompletionException(
                        new ExternalDependencyException("Unable to load monitoring rules"));
                }

                try {
                    ArrayList<RuleApiModel> rules = Json
                        .fromJson(Json.parse(result.getBody()), RuleListApiModel.class)
                        .getItems();

                    // Filter out the disabled rules
                    ArrayList<RuleApiModel> list = new ArrayList<>();
                    for (RuleApiModel rule : rules) {
                        if (rule.getEnabled()) list.add(rule);
                    }

                    return list;
                } catch (Exception e) {
                    log.error("Could not parse result from Key Value Storage: {}",
                        e.getMessage());
                    throw new CompletionException(
                        new ExternalDependencyException(
                            "Could not parse result from Key Value Storage"));
                }
            });
    }

    private WSRequest prepareRequest(String path) {
        return wsClient.url(this.rulesUrl + path)
            .addHeader("Accept", "application/json")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("User-Agent", "Telemetry Agent " + this.getClass().getTypeName())
            .setRequestTimeout(Duration.of(10, ChronoUnit.SECONDS));
    }
}
