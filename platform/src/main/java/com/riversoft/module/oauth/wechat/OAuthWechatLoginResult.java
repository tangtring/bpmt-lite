package com.riversoft.module.oauth.wechat;

public final class OAuthWechatLoginResult {
    private final OAuthWechatLoginStatus status;
    private final String redirectUrl;
    private final String userId;
    private final String reason;
    private final String message;

    private OAuthWechatLoginResult(OAuthWechatLoginStatus status, String redirectUrl, String userId, String reason,
            String message) {
        this.status = status;
        this.redirectUrl = redirectUrl;
        this.userId = userId;
        this.reason = reason;
        this.message = message;
    }

    public static OAuthWechatLoginResult skip() {
        return new OAuthWechatLoginResult(OAuthWechatLoginStatus.SKIP, null, null, null, null);
    }

    public static OAuthWechatLoginResult redirect(String redirectUrl) {
        return new OAuthWechatLoginResult(OAuthWechatLoginStatus.REDIRECT, redirectUrl, null, null, null);
    }

    public static OAuthWechatLoginResult loggedIn(String userId) {
        return new OAuthWechatLoginResult(OAuthWechatLoginStatus.LOGGED_IN, null, userId, null, null);
    }

    public static OAuthWechatLoginResult error(String reason, String message) {
        return new OAuthWechatLoginResult(OAuthWechatLoginStatus.ERROR, null, null, reason, message);
    }

    public OAuthWechatLoginStatus getStatus() {
        return status;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }
}
