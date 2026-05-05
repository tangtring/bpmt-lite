package com.riversoft.module.oauth.wechat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.riversoft.module.thirdpart.ThirdpartService;

public class OAuthWechatLoginServiceTest {

    @Test
    public void skipsWhenRequestIsNotFromWechat() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = request();

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.SKIP, result.getStatus());
        assertEquals(0, provider.authorizationCalls);
        assertEquals(0, provider.loginCalls);
    }

    @Test
    public void skipsWhenWechatLoginIsDisabled() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        Map<String, Object> thirdpart = agentThirdpart();
        thirdpart.put("wechatLoginEnabled", Integer.valueOf(0));

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), thirdpart);

        assertEquals(OAuthWechatLoginStatus.SKIP, result.getStatus());
        assertEquals(0, provider.authorizationCalls);
        assertEquals(0, provider.loginCalls);
    }

    @Test
    public void redirectsAgentWhenCodeIsMissing() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        provider.authorizationUrl = "https://wechat.example/oauth";
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        request.setServerName("127.0.0.1");
        request.setServerPort(18080);
        request.setQueryString("client_id=client-a");

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.REDIRECT, result.getStatus());
        assertEquals("https://wechat.example/oauth", result.getRedirectUrl());
        assertEquals("agent", provider.wechatType);
        assertEquals("corp-agent", provider.wechatKey);
        assertNull(provider.wechatScope);
        assertEquals("http://127.0.0.1:18080/oauth/authorize?client_id=client-a", provider.callbackUrl);
    }

    @Test
    public void logsInAgentWhenCodeIsPresent() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        provider.userId = "admin";
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        request.setParameter("code", "secret-code");

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.LOGGED_IN, result.getStatus());
        assertEquals("admin", result.getUserId());
        assertEquals(0, provider.authorizationCalls);
        assertEquals(1, provider.loginCalls);
        assertEquals("secret-code", provider.code);
    }

    @Test
    public void returnsErrorForInvalidConfig() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        Map<String, Object> thirdpart = agentThirdpart();
        thirdpart.put("wechatType", "");

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), thirdpart);

        assertEquals(OAuthWechatLoginStatus.ERROR, result.getStatus());
        assertEquals("wechat_config_invalid", result.getReason());
        assertEquals(0, provider.authorizationCalls);
        assertEquals(0, provider.loginCalls);
    }

    @Test
    public void returnsConfigErrorWhenProviderCannotBuildAuthorizationUrl() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        provider.authorizationFailure = new OAuthWechatConfigException("WxMp配置不存在.");
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.ERROR, result.getStatus());
        assertEquals("wechat_config_invalid", result.getReason());
        assertEquals(1, provider.authorizationCalls);
        assertEquals(0, provider.loginCalls);
    }

    @Test
    public void returnsErrorWhenProviderCannotLogin() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        provider.loginFailure = new RuntimeException("provider down");
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        request.setParameter("code", "secret-code");

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.ERROR, result.getStatus());
        assertEquals("wechat_login_failed", result.getReason());
        assertEquals(1, provider.loginCalls);
    }

    @Test
    public void returnsErrorWhenProviderReturnsBlankUserId() {
        TestWechatOAuthProvider provider = new TestWechatOAuthProvider();
        provider.userId = " ";
        OAuthWechatLoginService service = new OAuthWechatLoginService(provider);
        MockHttpServletRequest request = wechatRequest();
        request.setParameter("code", "secret-code");

        OAuthWechatLoginResult result = service.handle(request, new MockHttpServletResponse(), agentThirdpart());

        assertEquals(OAuthWechatLoginStatus.ERROR, result.getStatus());
        assertEquals("wechat_login_failed", result.getReason());
        assertEquals(1, provider.loginCalls);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/authorize");
        request.setRequestURI("/oauth/authorize");
        request.setServerName("localhost");
        request.setServerPort(80);
        return request;
    }

    private MockHttpServletRequest wechatRequest() {
        MockHttpServletRequest request = request();
        request.addHeader("user-agent", "Mozilla/5.0 MicroMessenger");
        return request;
    }

    private Map<String, Object> agentThirdpart() {
        Map<String, Object> thirdpart = new HashMap<String, Object>();
        thirdpart.put("thirdpartKey", "demo-app");
        thirdpart.put("clientId", "client-a");
        thirdpart.put("wechatLoginEnabled", Integer.valueOf(1));
        thirdpart.put("wechatType", ThirdpartService.WECHAT_TYPE_AGENT);
        thirdpart.put("wechatKey", "corp-agent");
        thirdpart.put("wechatScope", ThirdpartService.WECHAT_SCOPE_USERINFO);
        return thirdpart;
    }

    private static class TestWechatOAuthProvider implements WechatOAuthProvider {
        private String authorizationUrl = "https://wechat.example/oauth";
        private String userId = "admin";
        private RuntimeException authorizationFailure;
        private RuntimeException loginFailure;
        private int authorizationCalls;
        private int loginCalls;
        private String wechatType;
        private String wechatKey;
        private String wechatScope;
        private String callbackUrl;
        private String code;

        public String buildAuthorizationUrl(String wechatType, String wechatKey, String wechatScope,
                String callbackUrl) {
            authorizationCalls++;
            this.wechatType = wechatType;
            this.wechatKey = wechatKey;
            this.wechatScope = wechatScope;
            this.callbackUrl = callbackUrl;
            if (authorizationFailure != null) {
                throw authorizationFailure;
            }
            return authorizationUrl;
        }

        public String loginByCode(javax.servlet.http.HttpServletRequest request, String wechatType, String wechatKey,
                String wechatScope, String code) {
            loginCalls++;
            this.wechatType = wechatType;
            this.wechatKey = wechatKey;
            this.wechatScope = wechatScope;
            this.code = code;
            if (loginFailure != null) {
                throw loginFailure;
            }
            return userId;
        }
    }
}
