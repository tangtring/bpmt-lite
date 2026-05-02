package com.riversoft.api.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

public class ApiRequest {

    private final HttpServletRequest request;
    private final byte[] body;

    public ApiRequest(HttpServletRequest request) {
        this.request = request;
        this.body = readBody(request);
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public String getPathInfo() {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? "" : pathInfo;
    }

    private static byte[] readBody(HttpServletRequest request) {
        try {
            InputStream input = request.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new ApiException(400, "REQUEST_BODY_READ_FAILED", "请求 body 读取失败。");
        }
    }
}
