package com.riversoft.api.http;

import java.util.Collections;
import java.util.Map;

public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;
    private final Map<String, Object> details;

    public ApiException(int status, String code, String message) {
        this(status, code, message, Collections.<String, Object>emptyMap());
    }

    public ApiException(int status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Collections.<String, Object>emptyMap() : details;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
