package com.riversoft.module.oauth.wechat;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.riversoft.core.db.ORMService;
import com.riversoft.module.thirdpart.ThirdpartService;
import com.riversoft.platform.SessionManager;
import com.riversoft.platform.po.UsUser;
import com.riversoft.platform.web.WxActionAspect;
import com.riversoft.weixin.mp.oauth2.MpOAuth2s;
import com.riversoft.weixin.qy.oauth2.QyOAuth2s;
import com.riversoft.weixin.qy.oauth2.bean.QyUser;
import com.riversoft.wx.mp.MpAppSetting;
import com.riversoft.wx.mp.service.MpAppService;

public class RealWechatOAuthProvider implements WechatOAuthProvider {

    public String buildAuthorizationUrl(String wechatType, String wechatKey, String wechatScope, String callbackUrl) {
        if (ThirdpartService.WECHAT_TYPE_AGENT.equals(wechatType)) {
            return QyOAuth2s.defaultOAuth2s().authenticationUrl(callbackUrl, null);
        }
        if (ThirdpartService.WECHAT_TYPE_MP.equals(wechatType)) {
            Map<String, Object> mpConfig = loadMpConfig(wechatKey);
            MpAppSetting setting = MpAppService.getInstance().getAppSetting(mpConfig);
            if (setting == null) {
                throw new OAuthWechatConfigException("WxMp配置无效.");
            }
            String scope = StringUtils.isBlank(wechatScope) ? ThirdpartService.WECHAT_SCOPE_BASE : wechatScope;
            return MpOAuth2s.with(setting).authenticationUrl(callbackUrl, scope);
        }
        throw new IllegalArgumentException("unsupported wechatType: " + wechatType);
    }

    public String loginByCode(HttpServletRequest request, String wechatType, String wechatKey, String wechatScope,
            String code) {
        if (ThirdpartService.WECHAT_TYPE_AGENT.equals(wechatType)) {
            QyUser qyUser = QyOAuth2s.defaultOAuth2s().userInfo(code);
            SessionManager.doUserLogin(request, qyUser.getUserId());
            return qyUser.getUserId();
        }
        if (ThirdpartService.WECHAT_TYPE_MP.equals(wechatType)) {
            loadMpConfig(wechatKey);
            new WxActionAspect().mpCodeLogin(request, wechatKey, code);
            UsUser user = SessionManager.getUser();
            if (user == null || StringUtils.isBlank(user.getUid())) {
                throw new IllegalStateException("微信服务号登录未建立BPMT用户会话.");
            }
            return user.getUid();
        }
        throw new IllegalArgumentException("unsupported wechatType: " + wechatType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadMpConfig(String wechatKey) {
        Object mpConfig = ORMService.getInstance().findByPk("WxMp", wechatKey);
        if (!(mpConfig instanceof Map)) {
            throw new OAuthWechatConfigException("WxMp配置不存在.");
        }
        Map<String, Object> config = (Map<String, Object>) mpConfig;
        validateMpConfig(config);
        return config;
    }

    static void validateMpConfig(Map<String, Object> config) {
        requireMpConfigValue(config, "mpKey");
        requireMpConfigValue(config, "appId");
        requireMpConfigValue(config, "appSecret");
        requireMpConfigValue(config, "visitorTable");
    }

    private static void requireMpConfigValue(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            throw new OAuthWechatConfigException("WxMp配置不完整: " + key + "不能为空.");
        }
    }
}
