package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicTableViewController {

    public Map<String, Object> list(ApiRequest request) {
        throw serviceNotInitialized();
    }

    public Map<String, Object> create(ApiRequest request) {
        throw serviceNotInitialized();
    }

    public Map<String, Object> detail(String viewKey) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("viewKey", viewKey);
        return result;
    }

    public Map<String, Object> replace(String viewKey, ApiRequest request) {
        throw serviceNotInitialized();
    }

    public Map<String, Object> patch(String viewKey, String section, ApiRequest request) {
        throw serviceNotInitialized();
    }

    public Map<String, Object> delete(String viewKey, ApiRequest request) {
        throw serviceNotInitialized();
    }

    public Map<String, Object> validate(ApiRequest request) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("valid", Boolean.TRUE);
        return result;
    }

    private RuntimeException serviceNotInitialized() {
        return DynamicTableViewErrors.invalidSnapshot("动态表视图服务尚未初始化。");
    }
}
