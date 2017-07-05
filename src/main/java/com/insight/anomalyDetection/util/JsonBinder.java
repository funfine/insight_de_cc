package com.insight.anomalyDetection.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yfang on 7/2/2017.
 * This is to parse the JSON object.
 */

public enum JsonBinder {
    INSTANCE;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static TypeReference<Map<String,String>> typeRef
            = new TypeReference<Map<String,String>>() {};

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Map<String, String> readValue(String input) throws IOException {
        HashMap<String, String> jsonMap = objectMapper.readValue(input, typeRef);
        return jsonMap;
    }
}
