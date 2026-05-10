package com.riversoft.api.modules.dynamic_table_views;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

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
        String trimmedValue = StringUtils.trimToEmpty(value);
        for (DynamicTableViewSection section : values()) {
            if (section.value().equals(trimmedValue)) {
                return section;
            }
        }
        throw DynamicTableViewErrors.invalidSnapshot("不支持的动态表视图区块：" + trimmedValue);
    }
}
