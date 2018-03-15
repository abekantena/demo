// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent;

import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.runtime.IConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.Logger;
import play.api.libs.ws.WSConfigParser;
import play.api.libs.ws.ahc.*;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.AhcWSClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

public class Module extends AbstractModule {

    private static final Logger.ALogger log = Logger.of(Module.class);

    ActorSystem system = ActorSystem.create("streamingAgent");

    @Override
    public void configure() {
        // Note: this method should be empty
        // Try to use use JIT binding and @ImplementedBy instead
    }

    @Provides
    ActorSystem provideActorSystem() {
        return system;
    }

    @Provides
    Materializer provideMaterializer() {

        final Function<Throwable, Supervision.Directive> decider = e -> {
            log.error(e.getMessage(), e);
            e.printStackTrace();

            // kill the service
            // TODO: proper error handling and recovery
            System.exit(1);
            return Supervision.stop();
        };

        Materializer streamMaterializer = ActorMaterializer.create(
            ActorMaterializerSettings
                .create(system)
                .withSupervisionStrategy(decider),
            system);

        return streamMaterializer;
    }

    @Provides
    IServicesConfig provideIServicesConfig(IConfig config) {
        log.info("Messages db: {}", config.getServicesConfig().getMessagesStorageConfig().getDocumentDbDatabase());
        log.info("Messages collection: {}", config.getServicesConfig().getMessagesStorageConfig().getDocumentDbCollection());
        return config.getServicesConfig();
    }

    @Provides
    WSClient provideWSClient(Materializer materializer) {

        // WSClient doesn't work for the streaming agent, failing with
        // "No implementation for play.libs.ws.WSClient was bound"
        // so we create the client manually.

        /**
         * Note: If you create a WSClient manually then you must call
         * client.close() to clean it up when you’ve finished with it. Each
         * client creates its own thread pool. If you fail to close the client
         * or if you create too many clients then you will run out of threads
         * or file handles - you’ll get errors like “Unable to create new
         * native thread” or “too many open files” as the underlying resources
         * are consumed.
         */

        // See: https://www.playframework.com/documentation/2.6.x/JavaWS

        // Read in config file from application.conf
        Config conf = ConfigFactory.load();
        WSConfigParser parser = new WSConfigParser(conf, ClassLoader.getSystemClassLoader());
        AhcWSClientConfig clientConf = AhcWSClientConfigFactory.forClientConfig(parser.parse());

        // Start up asynchttpclient
        final DefaultAsyncHttpClientConfig asyncHttpClientConfig = new AhcConfigBuilder(clientConf).configure().build();
        final DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);

        return new AhcWSClient(asyncHttpClient, materializer);
    }
}
