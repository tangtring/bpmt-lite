package com.riversoft.module.thirdpart;

import com.riversoft.core.db.po.Code2NameVO;

public enum ThirdpartWechatScope implements Code2NameVO {

    SNSAPI_BASE("snsapi_base", "snsapi_base"),
    SNSAPI_USERINFO("snsapi_userinfo", "snsapi_userinfo");

    private String code;
    private String showName;

    ThirdpartWechatScope(String code, String showName) {
        this.code = code;
        this.showName = showName;
    }

    @Override
    public Object getCode() {
        return code;
    }

    @Override
    public String getShowName() {
        return showName;
    }
}
