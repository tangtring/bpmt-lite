package com.riversoft.ali;

import com.riversoft.core.BeanFactory;
import com.riversoft.util.jackson.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 阿里大鱼短信发送。
 *
 * v1.1.0 已裁剪历史 taobao-sdk 依赖和对应实现。
 * 当前类仅保留原入口和配置兼容层，未来可在不恢复旧 SDK 的前提下重新实现短信发送。
 *
 * @borball on 3/7/2016.
 */
public class SmsClient {

    private static final Logger logger = LoggerFactory.getLogger(SmsClient.class);

    /**
     * 短信签名
     */
    private String signName;

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public void setAliClient(AliClient aliClient) {
    }

    public static SmsClient getInstance(){
        return (SmsClient)BeanFactory.getInstance().getBean("smsClient");
    }

    /**
     * 发送短信
     * @param templateId 模板ID
     * @param mobile 手机号码
     * @param params 模板参数
     */
    public void send(String templateId, String mobile, Map<String, String> params) {
        logger.warn("ali dayu sms implementation has been removed in bpmt-lite v1.1.0. templateId={}, mobile={}, signName={}, params={}",
                templateId, mobile, signName, JsonMapper.defaultMapper().toJson(params));
    }
}
