package com.riversoft.api.modules.dynamic_tables;

import com.riversoft.api.http.ApiException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public final class DynamicTableValidator {
    private static final String[] SYSTEM_PREFIXES = new String[]{
            "CM_", "DEV_", "VW_", "WDG_", "ACT_GE_", "ACT_RE_", "US_", "WF_", "TB_", "TPL_", "WX_"
    };
    private static final String IDENTIFIER_PATTERN = "[A-Za-z][A-Za-z0-9_]*";

    private DynamicTableValidator() {
    }

    public static void validateForCreate(DynamicTableRequest request) {
        if (request == null) {
            throw new ApiException(400, "DYNAMIC_TABLE_NAME_REQUIRED", "表名不能为空。");
        }
        validateName(request.getName());
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw new ApiException(400, "DYNAMIC_TABLE_COLUMNS_REQUIRED", "字段不能为空。");
        }
        boolean hasPrimaryKey = false;
        Set<String> columnNames = new HashSet<String>();
        for (DynamicTableRequest.Column column : request.getColumns()) {
            if (column == null || StringUtils.isBlank(column.getName())) {
                throw new ApiException(400, "DYNAMIC_TABLE_COLUMN_NAME_REQUIRED", "字段名不能为空。");
            }
            String columnName = StringUtils.trim(column.getName());
            if (!columnName.matches(IDENTIFIER_PATTERN)) {
                throw new ApiException(422, "DYNAMIC_TABLE_COLUMN_NAME_INVALID", "字段名只能包含字母、数字和下划线，且必须以字母开头。");
            }
            if (!columnNames.add(columnName.toUpperCase())) {
                throw new ApiException(409, "DYNAMIC_TABLE_COLUMN_DUPLICATED", "字段名重复。");
            }
            hasPrimaryKey = hasPrimaryKey || column.isPrimaryKey();
        }
        if (!hasPrimaryKey) {
            throw new ApiException(400, "DYNAMIC_TABLE_PRIMARY_KEY_REQUIRED", "没有设置主键。");
        }
    }

    public static void validateName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new ApiException(400, "DYNAMIC_TABLE_NAME_REQUIRED", "表名不能为空。");
        }
        String trimmed = StringUtils.trim(name);
        if (!trimmed.matches(IDENTIFIER_PATTERN)) {
            throw new ApiException(422, "DYNAMIC_TABLE_NAME_INVALID", "表名只能包含字母、数字和下划线，且必须以字母开头。");
        }
        String upper = trimmed.toUpperCase();
        for (String prefix : SYSTEM_PREFIXES) {
            if (upper.startsWith(prefix)) {
                throw new ApiException(422, "DYNAMIC_TABLE_SYSTEM_PREFIX_FORBIDDEN", "[" + prefix + "]开头的表是系统表。");
            }
        }
    }
}
