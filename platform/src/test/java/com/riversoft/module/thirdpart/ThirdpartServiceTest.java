package com.riversoft.module.thirdpart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.riversoft.platform.po.CmPri;

public class ThirdpartServiceTest {

    @Test
    public void matchesConfiguredRedirectUriExactly() {
        assertTrue(ThirdpartService.isAllowedRedirectUri("http://127.0.0.1/demo/callback",
                "http://127.0.0.1/demo/callback"));
        assertTrue(ThirdpartService.isAllowedRedirectUri("http://a/cb\nhttp://b/cb", "http://b/cb"));
        assertFalse(ThirdpartService.isAllowedRedirectUri("http://a/cb", " http://a/cb"));
        assertFalse(ThirdpartService.isAllowedRedirectUri("http://a/cb", "http://a/cb "));
        assertFalse(ThirdpartService.isAllowedRedirectUri("http://a/cb", "http://a/cb/extra"));
        assertFalse(ThirdpartService.isAllowedRedirectUri("http://a/cb", "javascript:alert(1)"));
    }

    @Test
    public void hashesAndMatchesClientSecret() {
        assertTrue(ThirdpartService.matchesSecret(ThirdpartService.hashSecret("secret"), "secret"));
    }

    @Test
    public void preparesThirdpartPermissionCatalog() {
        CmPri pri = new CmPri();

        ThirdpartService.prepareThirdpartPri(pri, "demo-app", "演示系统");

        assertEquals(CmPri.Catelog.THIRDPART.getCode(), pri.getCatelogType());
        assertEquals("demo-app", pri.getCatelogKey());
        assertEquals("演示系统", pri.getBusiName());
    }

    @Test
    public void normalizesDisabledWechatLogin() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "0");
        input.put("wechatType", "");
        input.put("wechatKey", "");
        input.put("wechatScope", "");

        Map<String, Object> normalized = ThirdpartService.normalizeWechatLogin(input);

        assertEquals(Integer.valueOf(0), normalized.get("wechatLoginEnabled"));
        assertEquals(null, normalized.get("wechatType"));
        assertEquals(null, normalized.get("wechatKey"));
        assertEquals(null, normalized.get("wechatScope"));
    }

    @Test
    public void normalizesEnabledAgentWechatLogin() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "1");
        input.put("wechatType", "agent");
        input.put("wechatKey", "corp-agent");
        input.put("wechatScope", "snsapi_userinfo");

        Map<String, Object> normalized = ThirdpartService.normalizeWechatLogin(input);

        assertEquals(Integer.valueOf(1), normalized.get("wechatLoginEnabled"));
        assertEquals("agent", normalized.get("wechatType"));
        assertEquals("corp-agent", normalized.get("wechatKey"));
        assertEquals(null, normalized.get("wechatScope"));
    }

    @Test
    public void normalizesEnabledMpWechatLoginDefaultScope() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "1");
        input.put("wechatType", "mp");
        input.put("wechatKey", "service-mp");
        input.put("wechatScope", "");

        Map<String, Object> normalized = ThirdpartService.normalizeWechatLogin(input);

        assertEquals(Integer.valueOf(1), normalized.get("wechatLoginEnabled"));
        assertEquals("mp", normalized.get("wechatType"));
        assertEquals("service-mp", normalized.get("wechatKey"));
        assertEquals("snsapi_base", normalized.get("wechatScope"));
    }

    @Test(expected = com.riversoft.core.exception.SystemRuntimeException.class)
    public void rejectsEnabledWechatLoginWithoutType() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "1");
        input.put("wechatType", "");
        input.put("wechatKey", "corp-agent");

        ThirdpartService.normalizeWechatLogin(input);
    }

    @Test(expected = com.riversoft.core.exception.SystemRuntimeException.class)
    public void rejectsEnabledWechatLoginWithoutKey() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "1");
        input.put("wechatType", "agent");
        input.put("wechatKey", "");

        ThirdpartService.normalizeWechatLogin(input);
    }

    @Test(expected = com.riversoft.core.exception.SystemRuntimeException.class)
    public void rejectsInvalidWechatScope() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("wechatLoginEnabled", "1");
        input.put("wechatType", "mp");
        input.put("wechatKey", "service-mp");
        input.put("wechatScope", "snsapi_private");

        ThirdpartService.normalizeWechatLogin(input);
    }
}
