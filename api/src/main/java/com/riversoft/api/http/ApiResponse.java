package com.riversoft.api.http;

public class ApiResponse {

    private final boolean success;
    private final Object data;
    private final ApiError error;

    private ApiResponse(boolean success, Object data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(true, data, null);
    }

    public static ApiResponse error(ApiError error) {
        return new ApiResponse(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
