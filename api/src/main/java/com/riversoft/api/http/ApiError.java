package com.riversoft.api.http;

import java.util.Map;

public class ApiError {

    private String code;
    private String message;
    private Map<String, Object> details;
    private String requestId;

    public ApiError() {
    }

    public ApiError(String code, String message, Map<String, Object> details, String requestId) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getRequestId() {
        return requestId;
    }
}
