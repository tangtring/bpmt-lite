package com.riversoft.api.http;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class ApiJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ApiJson() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static <T> T fromJson(InputStream input, Class<T> type) {
        try {
            return MAPPER.readValue(input, type);
        } catch (IOException e) {
            throw new ApiException(400, "INVALID_JSON", "请求 JSON 无法解析。");
        }
    }
}
