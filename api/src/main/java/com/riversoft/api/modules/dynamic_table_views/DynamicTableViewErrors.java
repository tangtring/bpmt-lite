package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class DynamicTableViewErrors {
    private DynamicTableViewErrors() {
    }

    static ApiException notFound(String viewKey) {
        return new ApiException(404, "DYNAMIC_TABLE_VIEW_NOT_FOUND", "动态表视图不存在：" + viewKey);
    }

    static ApiException notDyn(String viewKey) {
        return new ApiException(409, "DYNAMIC_TABLE_VIEW_NOT_DYN", "目标视图不是 dyn 动态表视图：" + viewKey);
    }

    static ApiException invalidSnapshot(String message) {
        return new ApiException(400, "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT", message);
    }

    static ApiException invalidSnapshot(DynamicTableViewValidationResult result) {
        String message = "动态表视图快照校验失败。";
        if (result != null && result.getErrors() != null && !result.getErrors().isEmpty()) {
            message = result.getErrors().get(0).getMessage();
        }
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("errors", result == null ? Collections.emptyList() : result.getErrors());
        return new ApiException(400, "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT", message, details);
    }

    static ApiException alreadyExists(String viewKey) {
        return new ApiException(409, "DYNAMIC_TABLE_VIEW_ALREADY_EXISTS", "动态表视图已存在：" + viewKey);
    }

    static ApiException confirmRequired() {
        return new ApiException(400, "DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED", "删除动态表视图必须传入 confirmViewKey 并与 viewKey 一致。");
    }
}
