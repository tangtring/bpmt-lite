package com.riversoft.module.oauth.wechat;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.riversoft.core.db.ORMService;
import com.riversoft.module.thirdpart.ThirdpartService;
import com.riversoft.platform.SessionManager;
import com.riversoft.platform.web.WxActionAspect;
import com.riversoft.weixin.mp.oauth2.MpOAuth2s;
import com.riversoft.weixin.qy.oauth2.QyOAuth2s;
import com.riversoft.weixin.qy.oauth2.bean.QyUser;
import com.riversoft.wx.mp.MpAppSetting;
import com.riversoft.wx.mp.service.MpAppService;

public class RealWechatOAuthProvider implements WechatOAuthProvider {

    @SuppressWarnings("unchecked")
    public String buildAuthorizationUrl(String wechatType, String wechatKey, String wechatScope, String callbackUrl) {
        if (ThirdpartService.WECHAT_TYPE_AGENT.equals(wechatType)) {
            return QyOAuth2s.defaultOAuth2s().authenticationUrl(callbackUrl, null);
        }
        if (ThirdpartService.WECHAT_TYPE_MP.equals(wechatType)) {
            Map<String, Object> mpConfig = (Map<String, Object>) ORMService.getInstance().findByPk("WxMp", wechatKey);
            MpAppSetting setting = MpAppService.getInstance().getAppSetting(mpConfig);
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
            return new WxActionAspect().mpCodeLogin(request, wechatKey, code);
        }
        throw new IllegalArgumentException("unsupported wechatType: " + wechatType);
    }
}
