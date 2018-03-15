// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.iot.iothubreact.MessageFromDevice;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;
import play.Logger;
import play.libs.Json;
import scala.Option;

import java.util.Iterator;
import java.util.Map;

public class DeviceMessageParser implements IDeviceMessageParser {

    private static final Logger.ALogger log = Logger.of(DeviceMessageParser.class);

    @Override
    public RawMessage messageToRawMessage(MessageFromDevice m) {
        String schema = m.messageSchema();
        String contentType = m.contentType();
        String content = m.contentAsString();

        RawMessage result = new RawMessage();

        result.setId(m.deviceId() + ";" + m.received().toEpochMilli())
            .setDeviceId(m.deviceId())
            .setSchema(schema)
            .setCreateTime(m.created().toEpochMilli())
            .setReceivedTime(m.received().toEpochMilli());

        // Save all the message properties from the message header
        m.properties().forEach(
            (k, v) -> result.setStringFromProperties(k, v));

        // Save all the message properties from the message payload
        if (contentType.toLowerCase().contains("json")
            || schema.toLowerCase().contains("json")
            || (contentType.isEmpty() && content.contains("{"))) {
            JsonNode data = this.tryExtractFromJson(content);

            if (data == null) {
                // Log as an error, including stream information
                log.error("Invalid JSON, partition:{}, offset:{}, msgId:{}, device:{}, msgTime:{}, msg:{}",
                    getPartitionNumber(m), m.offset(), m.messageId(),
                    m.deviceId(), m.received(), content);
            } else if (data.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    JsonNode value = field.getValue();
                    if (value.isNumber()) {
                        result.setNumberFromPayload(field.getKey(), field.getValue().asDouble());
                    } else if (value.isBoolean()) {
                        result.setBooleanFromPayload(field.getKey(), field.getValue().asBoolean());
                    } else {
                        result.setStringFromPayload(field.getKey(), field.getValue().asText(""));
                    }
                }
            } else if (data.isNumber()) {
                result.setNumberFromPayload("value", data.asDouble());
            } else if (data.isBoolean()) {
                result.setBooleanFromPayload("value", data.asBoolean());
            } else if (data.isTextual()) {
                result.setStringFromPayload("value", data.asText());
            }
        }

        return result;
    }

    private JsonNode tryExtractFromJson(String content) {
        try {
            return Json.parse(content);
        } catch (Throwable t) {
            return null;
        }
    }

    private int getPartitionNumber(MessageFromDevice m) {
        Option<Object> partitionNumber = m.runtimeInfo().partitionInfo().partitionNumber();
        return partitionNumber.isEmpty() ? -1 : (int) partitionNumber.get();
    }
}
