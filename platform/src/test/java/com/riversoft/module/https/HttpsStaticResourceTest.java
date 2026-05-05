package com.riversoft.module.https;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class HttpsStaticResourceTest {

    private static final String[] RUNTIME_JSP_PATHS = {
            "platform/src/main/webapp/h5/widget/h5_head.jsp",
            "platform/src/main/webapp/xhtml/frame/login.jsp",
            "platform/src/main/webapp/xhtml/frame_new/login.jsp",
            "platform/src/main/webapp/xhtml/frame/wx_binding.jsp"
    };

    @Test
    public void h5HeadDoesNotLoadHttpRuntimeResources() throws Exception {
        String jsp = read("platform/src/main/webapp/h5/widget/h5_head.jsp");

        assertFalse(jsp.contains("http://apps.bdimg.com/libs/jquery/2.1.4/jquery.min.js"));
        assertFalse(jsp.contains("http://cdn.bootcss.com/weui/0.4.3/style/weui.min.css"));
        assertFalse(jsp.contains("http://cdn.bootcss.com/jquery-weui/0.8.0/css/jquery-weui.min.css"));
        assertFalse(jsp.contains("http://cdn.bootcss.com/jquery-weui/0.8.0/js/jquery-weui.min.js"));
        assertTrue(jsp.contains("${_cp}/js/jquery-weui-0.7.2/lib/jquery-2.1.4.js"));
        assertTrue(jsp.contains("${_cp}/js/jquery-weui-0.7.2/lib/weui.min.css"));
        assertTrue(jsp.contains("${_cp}/js/jquery-weui-0.7.2/css/jquery-weui.min.css"));
        assertTrue(jsp.contains("${_cp}/js/jquery-weui-0.7.2/js/jquery-weui.min.js"));
    }

    @Test
    public void wechatOfficialScriptsUseHttps() throws Exception {
        assertContains("platform/src/main/webapp/h5/widget/h5_head.jsp",
                "https://res.wx.qq.com/open/js/jweixin-1.1.0.js");
        assertContains("platform/src/main/webapp/xhtml/frame/login.jsp",
                "https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js");
        assertContains("platform/src/main/webapp/xhtml/frame_new/login.jsp",
                "https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js");
        assertContains("platform/src/main/webapp/xhtml/frame/wx_binding.jsp",
                "https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js");
    }

    @Test
    public void wechatRedirectUrisFollowCurrentScheme() throws Exception {
        assertContains("platform/src/main/webapp/xhtml/frame/login.jsp",
                "redirect_uri : '${_cp}/frame/LoginAction/wxLogin.shtml'");
        assertContains("platform/src/main/webapp/xhtml/frame_new/login.jsp",
                "redirect_uri : '${_cp}/frame/LoginAction/wxLogin.shtml'");
        assertContains("platform/src/main/webapp/xhtml/frame/wx_binding.jsp",
                "redirect_uri : '${_cp}/frame/FrameAction/submitBinding.shtml'");
    }

    @Test
    public void targetRuntimeJspsDoNotReferenceHttpResources() throws Exception {
        for (int i = 0; i < RUNTIME_JSP_PATHS.length; i++) {
            String path = RUNTIME_JSP_PATHS[i];
            String jsp = read(path);

            assertFalse(path, jsp.contains("src=\"http://"));
            assertFalse(path, jsp.contains("href=\"http://"));
            assertFalse(path, jsp.contains("redirect_uri : 'http://"));
        }
    }

    private void assertContains(String path, String expected) throws Exception {
        assertTrue(path + " should contain " + expected, read(path).contains(expected));
    }

    private String read(String path) throws Exception {
        File file = new File(path);
        if (!file.isFile() && path.startsWith("platform/")) {
            file = new File(path.substring("platform/".length()));
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
