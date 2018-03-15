// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import com.microsoft.azure.documentdb.Document;

import java.util.Map;

import static com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessagePart.*;

public class RawMessage {

    private final String MESSAGE_ID = "id";
    private final String DEVICE_ID = "device.id";
    private final String MESSAGE_SCHEMA = "device.msg.schema";
    private final String MESSAGE_CREATED = "device.msg.created";
    private final String MESSAGE_RECEIVED = "device.msg.received";

    private final String MSG_PROPERTIES_PREFIX = "metadata.";
    private final String MSG_PAYLOAD_PREFIX = "data.";

    private final RawMessagePart reservedData;
    private final RawMessagePart msgPropsData;
    private final RawMessagePart msgPayloadData;

    public RawMessage() {
        this.reservedData = new RawMessagePart();
        this.msgPropsData = new RawMessagePart();
        this.msgPayloadData = new RawMessagePart();
    }

    // RESERVED FIELDS

    public RawMessage setId(String v) {
        this.reservedData.setString(MESSAGE_ID, v);
        return this;
    }

    public String getId() {
        return this.reservedData.getString(MESSAGE_ID);
    }

    public RawMessage setDeviceId(String v) {
        this.reservedData.setString(DEVICE_ID, v);
        return this;
    }

    public String getDeviceId() {
        return this.reservedData.getString(DEVICE_ID);
    }

    public RawMessage setSchema(String v) {
        this.reservedData.setString(MESSAGE_SCHEMA, v);
        return this;
    }

    public String getSchema() {
        return this.reservedData.getString(MESSAGE_SCHEMA);
    }

    public RawMessage setCreateTime(long v) {
        this.reservedData.setNumber(MESSAGE_CREATED, v);
        return this;
    }

    public long getCreateTime() {
        return this.reservedData.getNumber(MESSAGE_CREATED).longValue();
    }

    public RawMessage setReceivedTime(long v) {
        this.reservedData.setNumber(MESSAGE_RECEIVED, v);
        return this;
    }

    public long getReceivedTime() {
        return this.reservedData.getNumber(MESSAGE_RECEIVED).longValue();
    }

    // MESSAGE PROPERTIES FIELDS

    public RawMessage setNumberFromProperties(String k, int v) {
        this.msgPropsData.setNumber(k, (double) v);
        return this;
    }

    public RawMessage setNumberFromProperties(String k, long v) {
        this.msgPropsData.setNumber(k, (double) v);
        return this;
    }

    public RawMessage setNumberFromProperties(String k, double v) {
        this.msgPropsData.setNumber(k, v);
        return this;
    }

    public RawMessage setStringFromProperties(String k, String v) {
        this.msgPropsData.setString(k, v);
        return this;
    }

    public RawMessage setBooleanFromProperties(String k, Boolean v) {
        this.msgPropsData.setBoolean(k, v);
        return this;
    }

    // MESSAGE PAYLOAD FIELDS

    public RawMessage setNumberFromPayload(String k, int v) {
        this.msgPayloadData.setNumber(k, (double) v);
        return this;
    }

    public RawMessage setNumberFromPayload(String k, long v) {
        this.msgPayloadData.setNumber(k, (double) v);
        return this;
    }

    public RawMessage setNumberFromPayload(String k, double v) {
        this.msgPayloadData.setNumber(k, v);
        return this;
    }

    public long getNumberFromPayload(String k) {
        return this.msgPayloadData.getNumber(k).longValue();
    }

    public RawMessage setStringFromPayload(String k, String v) {
        this.msgPayloadData.setString(k, v);
        return this;
    }

    public String getStringFromPayload(String k) {
        return this.msgPayloadData.getString(k);
    }

    public RawMessage setBooleanFromPayload(String k, Boolean v) {
        this.msgPayloadData.setBoolean(k, v);
        return this;
    }

    public Boolean getBooleanFromPayload(String k) {
        return this.msgPayloadData.getBoolean(k);
    }

    public boolean hasPayloadField(String k) {
        return this.msgPayloadData.has(k);
    }

    public String typeOfPayloadField(String k) {
        if (!this.msgPayloadData.has(k)) return null;
        return this.msgPayloadData.getKeys().get(k);
    }

    public Boolean payloadIsNumber(String k) {
        return this.msgPayloadData.isNumber(k);
    }

    public Boolean payloadIsString(String k) {
        return this.msgPayloadData.isString(k);
    }

    public Boolean payloadIsBoolean(String k) {
        return this.msgPayloadData.isBoolean(k);
    }

    // SUPPORT DOCUMENT DB

    public Document toDocument() {
        Document document = new Document();

        document.set("doc.schema", "d2cmessage");
        document.set("doc.schemaVersion", 1);

        document.setId(this.getId());
        document.set(DEVICE_ID, this.getDeviceId());
        document.set(MESSAGE_SCHEMA, this.getSchema());
        document.set(MESSAGE_CREATED, this.getCreateTime());
        document.set(MESSAGE_RECEIVED, this.getReceivedTime());

        // Mesage properties
        for (Map.Entry<String, String> k : this.msgPropsData.getKeys().entrySet()) {
            String key = MSG_PROPERTIES_PREFIX + k.getKey();
            switch (k.getValue()) {
                case STRING:
                    document.set(key, this.msgPropsData.getString(k.getKey()));
                    break;
                case NUMBER:
                    document.set(key, this.msgPropsData.getNumber(k.getKey()));
                    break;
                case BOOLEAN:
                    document.set(key, this.msgPropsData.getBoolean(k.getKey()));
                    break;
            }
        }

        // Message payload
        for (Map.Entry<String, String> k : this.msgPayloadData.getKeys().entrySet()) {
            String key = MSG_PAYLOAD_PREFIX + k.getKey();
            switch (k.getValue()) {
                case STRING:
                    document.set(key, this.msgPayloadData.getString(k.getKey()));
                    break;
                case NUMBER:
                    document.set(key, this.msgPayloadData.getNumber(k.getKey()));
                    break;
                case BOOLEAN:
                    document.set(key, this.msgPayloadData.getBoolean(k.getKey()));
                    break;
            }
        }

        return document;
    }
}
