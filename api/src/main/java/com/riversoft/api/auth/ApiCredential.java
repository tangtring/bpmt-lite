package com.riversoft.api.auth;

public class ApiCredential {

    private final String appKey;
    private final String appSecret;
    private final String actAs;

    public ApiCredential(String appKey, String appSecret, String actAs) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.actAs = actAs;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getActAs() {
        return actAs;
    }
}
