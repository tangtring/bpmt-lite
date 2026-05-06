package com.riversoft.module.thirdpart;

import com.riversoft.core.db.po.Code2NameVO;

public enum ThirdpartWechatType implements Code2NameVO {

    AGENT("agent", "企业号 agent"),
    MP("mp", "服务号 mp");

    private String code;
    private String showName;

    ThirdpartWechatType(String code, String showName) {
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
