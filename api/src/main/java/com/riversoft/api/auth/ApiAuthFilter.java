package com.riversoft.api.auth;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.riversoft.api.context.ApiUserContext;
import com.riversoft.api.http.ApiError;
import com.riversoft.api.http.ApiException;
import com.riversoft.api.http.ApiJson;
import com.riversoft.api.http.ApiResponse;

public class ApiAuthFilter implements Filter {

    static final long ALLOWED_CLOCK_SKEW_SECONDS = 300L;

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestId = UUID.randomUUID().toString();

        try {
            ApiCredential credential = ApiCredentials.fromEnvironment();
            authenticate(httpRequest, credential);
            ApiUserContext.init(httpRequest, credential.getActAs());
            chain.doFilter(request, response);
        } catch (ApiException e) {
            writeError(httpResponse, e, requestId);
        }
    }

    public void destroy() {
    }

    private void authenticate(HttpServletRequest request, ApiCredential credential) {
        String appKey = header(request, "X-BPMT-App-Key");
        String timestamp = header(request, "X-BPMT-Timestamp");
        String nonce = header(request, "X-BPMT-Nonce");
        String signature = header(request, "X-BPMT-Signature");

        if (!credential.getAppKey().equals(appKey)) {
            throw new ApiException(401, "INVALID_APP_KEY", "无效的 API appKey。");
        }

        validateTimestamp(timestamp);
        if (isBlank(nonce) || isBlank(signature)) {
            throw new ApiException(401, "INVALID_SIGNATURE", "API 请求签名无效。");
        }

        String path = normalizePath(request);
        String query = HmacSignature.normalizeQuery(request.getParameterMap());
        String bodyHash = HmacSignature.sha256Hex(new byte[0]);
        String canonical = HmacSignature.canonical(request.getMethod(), path, query, timestamp, nonce, bodyHash);
        String expected = HmacSignature.sign(credential.getAppSecret(), canonical);

        if (!HmacSignature.constantTimeEquals(expected, signature)) {
            throw new ApiException(401, "INVALID_SIGNATURE", "API 请求签名无效。");
        }
    }

    private void validateTimestamp(String timestamp) {
        if (isBlank(timestamp)) {
            throw new ApiException(401, "INVALID_TIMESTAMP", "API 请求时间戳无效。");
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new ApiException(401, "INVALID_TIMESTAMP", "API 请求时间戳无效。");
        }

        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - requestTime) > ALLOWED_CLOCK_SKEW_SECONDS) {
            throw new ApiException(401, "SIGNATURE_EXPIRED", "API 请求签名已过期。");
        }
    }

    private String normalizePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (requestUri != null && contextPath != null && contextPath.length() > 0 && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri == null ? "" : requestUri;
    }

    private void writeError(HttpServletResponse response, ApiException exception, String requestId) throws IOException {
        response.setStatus(exception.getStatus());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        ApiError error = new ApiError(
                exception.getCode(),
                exception.getMessage(),
                exception.getDetails() == null ? Collections.<String, Object>emptyMap() : exception.getDetails(),
                requestId);
        response.getWriter().write(ApiJson.toJson(ApiResponse.error(error)));
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
