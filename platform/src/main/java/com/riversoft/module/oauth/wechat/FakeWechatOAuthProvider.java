package com.riversoft.module.oauth.wechat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.riversoft.platform.SessionManager;

public class FakeWechatOAuthProvider implements WechatOAuthProvider {
    private static final String DEFAULT_FAKE_CODE = "fake-admin";

    private final LoginSessionWriter loginSessionWriter;

    public FakeWechatOAuthProvider() {
        this(new LoginSessionWriter() {
            public void doUserLogin(HttpServletRequest request, String userId) {
                SessionManager.doUserLogin(request, userId);
            }
        });
    }

    FakeWechatOAuthProvider(LoginSessionWriter loginSessionWriter) {
        this.loginSessionWriter = loginSessionWriter;
    }

    public String buildAuthorizationUrl(String wechatType, String wechatKey, String wechatScope, String callbackUrl) {
        String separator = callbackUrl.indexOf('?') >= 0 ? "&" : "?";
        return callbackUrl + separator + "code=" + urlEncode(fakeCode());
    }

    public String loginByCode(HttpServletRequest request, String wechatType, String wechatKey, String wechatScope,
            String code) {
        String userId = userIdForCode(code);
        loginSessionWriter.doUserLogin(request, userId);
        return userId;
    }

    private String userIdForCode(String code) {
        if ("fake-admin".equals(code)) {
            return "admin";
        }
        if ("fake-user-no-pri".equals(code)) {
            return "oauth_no_pri";
        }
        if ("fake-invalid".equals(code)) {
            throw new RuntimeException("fake wechat login failed");
        }
        throw new RuntimeException("unsupported fake wechat code");
    }

    private String fakeCode() {
        String envValue = StringUtils.trimToNull(System.getenv("BPMT_OAUTH_WECHAT_FAKE_CODE"));
        if (envValue != null) {
            return envValue;
        }
        String propertyValue = StringUtils.trimToNull(System.getProperty("bpmt.oauth.wechat.fake.code"));
        return propertyValue == null ? DEFAULT_FAKE_CODE : propertyValue;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported.", e);
        }
    }

    interface LoginSessionWriter {
        void doUserLogin(HttpServletRequest request, String userId);
    }
}
