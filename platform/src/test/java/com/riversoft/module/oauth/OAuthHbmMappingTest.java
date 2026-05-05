package com.riversoft.module.oauth;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.Test;

public class OAuthHbmMappingTest {
    @Test
    public void thirdpartMappingsExistOnClasspath() {
        assertNotNull(resource("hbm/common/CM_THIRDPART.hbm.xml"));
        assertNotNull(resource("hbm/common/CM_THIRDPART_AUTH_CODE.hbm.xml"));
        assertNotNull(resource("hbm/common/CM_THIRDPART_ACCESS_TOKEN.hbm.xml"));
    }

    @Test
    public void thirdpartMappingContainsWechatLoginFields() throws Exception {
        String hbm = readResource("hbm/common/CM_THIRDPART.hbm.xml");

        org.junit.Assert.assertTrue(hbm.contains("name=\"wechatLoginEnabled\""));
        org.junit.Assert.assertTrue(hbm.contains("name=\"wechatType\""));
        org.junit.Assert.assertTrue(hbm.contains("name=\"wechatKey\""));
        org.junit.Assert.assertTrue(hbm.contains("name=\"wechatScope\""));
        org.junit.Assert.assertTrue(hbm.contains("WECHAT_LOGIN_ENABLED"));
        org.junit.Assert.assertTrue(hbm.contains("WECHAT_TYPE"));
        org.junit.Assert.assertTrue(hbm.contains("WECHAT_KEY"));
        org.junit.Assert.assertTrue(hbm.contains("WECHAT_SCOPE"));
    }

    private String readResource(String name) throws Exception {
        InputStream input = resource(name);
        org.junit.Assert.assertNotNull(input);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            input.close();
        }
    }

    private InputStream resource(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
}
