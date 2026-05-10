package com.riversoft.api.modules.dynamic_table_views;

import java.util.Locale;

public enum DynamicTableViewSection {
    BASE,
    FIELDS,
    QUERIES,
    LIMITS,
    PROCESSORS,
    VARIABLES,
    SUBVIEWS,
    BUTTONS,
    WEIXIN,
    SCRIPTS;

    public String value() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static DynamicTableViewSection parse(String value) {
        for (DynamicTableViewSection section : values()) {
            if (section.value().equals(value)) {
                return section;
            }
        }
        throw DynamicTableViewErrors.invalidSnapshot("不支持的动态表视图区块：" + value);
    }
}
