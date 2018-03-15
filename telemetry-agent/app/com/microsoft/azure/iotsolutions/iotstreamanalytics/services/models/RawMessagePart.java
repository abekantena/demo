// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models;

import java.util.HashMap;

public class RawMessagePart {

    public static final String NUMBER = "NUMBER";
    public static final String STRING = "STRING";
    public static final String BOOLEAN = "BOOLEAN";

    private final HashMap<String, String> keys;
    private final HashMap<String, Double> numbers;
    private final HashMap<String, String> strings;
    private final HashMap<String, Boolean> booleans;

    public RawMessagePart() {
        this.keys = new HashMap<>();
        this.numbers = new HashMap<>();
        this.strings = new HashMap<>();
        this.booleans = new HashMap<>();
    }

    public RawMessagePart setNumber(String k, double v) {
        this.keys.put(k, NUMBER);
        this.numbers.put(k, v);
        return this;
    }

    public RawMessagePart setNumber(String k, int v) {
        this.keys.put(k, NUMBER);
        this.numbers.put(k, (double) v);
        return this;
    }

    public RawMessagePart setNumber(String k, long v) {
        this.keys.put(k, NUMBER);
        this.numbers.put(k, (double) v);
        return this;
    }

    public Double getNumber(String k) {
        return this.numbers.get(k);
    }

    public RawMessagePart setString(String k, String v) {
        this.keys.put(k, STRING);
        this.strings.put(k, v);
        return this;
    }

    public String getString(String k) {
        return this.strings.get(k);
    }

    public RawMessagePart setBoolean(String k, Boolean v) {
        this.keys.put(k, BOOLEAN);
        this.booleans.put(k, v);
        return this;
    }

    public Boolean getBoolean(String k) {
        return this.booleans.get(k);
    }

    public HashMap<String, String> getKeys(){
        return this.keys;
    }

    public boolean has(String k) {
        return this.keys.containsKey(k);
    }

    public String typeOf(String k) {
        if (!this.has(k)) return null;
        return this.keys.get(k);
    }

    public boolean isNumber(String k) {
        return this.typeOf(k) == NUMBER;
    }

    public boolean isString(String k) {
        return this.typeOf(k) == STRING;
    }

    public boolean isBoolean(String k) {
        return this.typeOf(k) == BOOLEAN;
    }
}
