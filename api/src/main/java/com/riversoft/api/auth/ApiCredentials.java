package com.riversoft.api.auth;

import com.riversoft.api.context.ApiUserContext;
import com.riversoft.api.http.ApiException;

public final class ApiCredentials {

    private static final String ENV_APP_KEY = "BPMT_API_APP_KEY";
    private static final String ENV_APP_SECRET = "BPMT_API_APP_SECRET";
    private static final String ENV_ACT_AS = "BPMT_API_ACT_AS";

    private ApiCredentials() {
    }

    public static ApiCredential fromEnvironment() {
        String appKey = trim(System.getenv(ENV_APP_KEY));
        String appSecret = trim(System.getenv(ENV_APP_SECRET));
        if (isBlank(appKey) || isBlank(appSecret)) {
            throw new ApiException(401, "API_CREDENTIALS_NOT_CONFIGURED", "API appKey 或 appSecret 未配置。");
        }

        return new ApiCredential(appKey, appSecret, ApiUserContext.resolveActAs(System.getenv(ENV_ACT_AS)));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.length() == 0;
    }
}
