package com.riversoft.module.oauth.wechat;

import javax.servlet.http.HttpServletRequest;

public interface WechatOAuthProvider {
    String buildAuthorizationUrl(String wechatType, String wechatKey, String wechatScope, String callbackUrl);

    String loginByCode(HttpServletRequest request, String wechatType, String wechatKey, String wechatScope, String code);
}

class OAuthWechatConfigException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    OAuthWechatConfigException(String message) {
        super(message);
    }
}
