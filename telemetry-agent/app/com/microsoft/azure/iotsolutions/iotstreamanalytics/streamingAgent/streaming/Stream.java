// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.streaming;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.microsoft.azure.iot.iothubreact.MessageFromDevice;
import com.microsoft.azure.iot.iothubreact.SourceOptions;
import com.microsoft.azure.iot.iothubreact.javadsl.IoTHub;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.IMessages;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.runtime.IConfig;
import org.joda.time.DateTime;
import play.Logger;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Stream implements IStream {

    private static final Logger.ALogger log = Logger.of(Stream.class);

    /**
     * Every 60 seconds, reload the processing logic (e.g. reload rules)
     */
    private static final int REFRESH_LOGIC_FREQUENCY = 60;

    /**
     * Every 30 seconds log the throughput
     */
    private static final int LOG_THROUGHPUT_FREQUENCY = 30;

    /**
     * Streaming options: use the checkpoint if available,
     * otherwise start streaming from 24 hours in the past
     */
    private final int StartFrom = 24;

    /**
     * Akka streams materializer
     */
    private final Materializer streamMaterializer;

    /**
     * Akka actor system used here for scheduling
     */
    private final ActorSystem system;

    /**
     * IoT Hub partitions
     */
    private final int partitionsCount;

    /**
     * The logic used to process each message
     */
    private IMessages messagesProcessor;

    /**
     * Azure IoT Hub (reactive) instance
     */
    private final IoTHub hub;

    /**
     * Akka stream source
     */
    private Source<MessageFromDevice, NotUsed> source;
    private ArrayList<Source<MessageFromDevice, NotUsed>> sources;

    public long throughputPreviousTime = 0;
    public long throughputPreviousTotal = 0;
    public long throughputTotal = 0;

    @Inject
    public Stream(
        ActorSystem system,
        Materializer streamMaterializer,
        IConfig config,
        IMessages messagesProcessor) {
        this.system = system;
        this.streamMaterializer = streamMaterializer;
        this.messagesProcessor = messagesProcessor;
        this.hub = new IoTHub();
        this.partitionsCount = config.getStreamPartitionsCount();
        this.sources = new ArrayList<>();
    }

    @Override
    public void Run() {

        log.info("Starting stream");

        for (int p = 0; p < this.partitionsCount; p++) {
            Source<MessageFromDevice, NotUsed> source = this.hub.source(this.getStreamOptions(p));
            this.sources.add(source);

            source
                .via(processingFlow())
                .to(this.hub.checkpointSink())
                .run(this.streamMaterializer);
        }

        // Every 60 seconds, reload the processing logic (e.g. reload rules)
        this.system.scheduler().schedule(
            new FiniteDuration(REFRESH_LOGIC_FREQUENCY, TimeUnit.SECONDS),
            new FiniteDuration(REFRESH_LOGIC_FREQUENCY, TimeUnit.SECONDS),
            () -> this.messagesProcessor.refreshLogic(),
            this.system.dispatcher());

        // Every 30 seconds log the throughput
        if (log.isInfoEnabled()) {
            this.system.scheduler().schedule(
                new FiniteDuration(LOG_THROUGHPUT_FREQUENCY, TimeUnit.SECONDS),
                new FiniteDuration(LOG_THROUGHPUT_FREQUENCY, TimeUnit.SECONDS),
                () -> logThroughput(this),
                this.system.dispatcher());
        }
    }

    private SourceOptions getStreamOptions(int partition) {

        Instant startTimeIfNoCheckpoint = Instant.now().minus(this.StartFrom, ChronoUnit.HOURS);
        return new SourceOptions()
            .partitions(new int[]{partition})
            .fromCheckpoint(startTimeIfNoCheckpoint);
    }

    private Flow<MessageFromDevice, MessageFromDevice, NotUsed> processingFlow() {

        return Flow.of(MessageFromDevice.class).map(m -> {
            try {
                this.messagesProcessor.process(m);
            } catch (ExternalDependencyException e) {
                log.error(
                    "Error while processing message offset {} for device {}: {}",
                    m.offset(),
                    m.deviceId(),
                    e);
                // TODO: stop the stream, require user input?
            } catch (Exception e) {
                log.error(
                    "Error while processing message offset {} for device {}: {}",
                    m.offset(),
                    m.deviceId(),
                    e);
                // TODO: fix ConcurrentModificationException
                //       https://github.com/Azure/telemetry-agent-java/issues/55
            }

            this.throughputTotal++;
            return m;
        });
    }

    private void logThroughput(Stream self) {

        long currTime = DateTime.now().getMillis();
        if (self.throughputPreviousTime == 0) {
            self.throughputPreviousTime = currTime;
            self.throughputPreviousTotal = self.throughputTotal;
            return;
        }

        double countDelta = self.throughputTotal - self.throughputPreviousTotal;
        double timeDelta = currTime - self.throughputPreviousTime;
        double throughput = (countDelta / timeDelta) * 1000;

        log.info("Throughput: {} msgs/sec - {} messages in the last {} seconds ",
            (int) throughput, (int) countDelta, (int) (timeDelta / 1000));

        self.throughputPreviousTotal = self.throughputTotal;
        self.throughputPreviousTime = currTime;
    }
}
