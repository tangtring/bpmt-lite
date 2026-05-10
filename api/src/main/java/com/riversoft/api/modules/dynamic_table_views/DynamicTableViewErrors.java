package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiException;

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

    static ApiException confirmRequired() {
        return new ApiException(400, "DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED", "删除动态表视图必须传入 confirmViewKey 并与 viewKey 一致。");
    }
}
