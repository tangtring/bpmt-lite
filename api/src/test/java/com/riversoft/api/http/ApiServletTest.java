package com.riversoft.api.http;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class ApiServletTest {

    @Test
    public void successResponseSerializesSuccessTrue() {
        String json = ApiJson.toJson(ApiResponse.success(Collections.singletonMap("name", "RV_TEST")));

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"name\":\"RV_TEST\""));
    }

    @Test
    public void errorResponseSerializesStableCode() {
        ApiError error = new ApiError(
                "DYNAMIC_TABLE_ALREADY_EXISTS",
                "表已存在",
                Collections.<String, Object>emptyMap(),
                "req-1");

        String json = ApiJson.toJson(ApiResponse.error(error));

        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"code\":\"DYNAMIC_TABLE_ALREADY_EXISTS\""));
        assertTrue(json.contains("\"requestId\":\"req-1\""));
    }
}
