package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.platform.po.VwUrl;

import java.util.List;
import java.util.Map;

interface DynamicTableViewRepository {
    List<VwUrl> listDynUrls(int start, int limit);

    int countDynUrls();

    VwUrl findUrl(String viewKey);

    Map<String, Object> findTable(String viewKey);

    Map<String, Object> findTableDefinition(String tableName);

    Map<String, Object> findColumnDefinition(String tableName, String columnName);

    VwUrl saveUrl(VwUrl url);

    void updateUrl(VwUrl url);

    default void saveViewConfig(String viewKey, Map<String, Object> tableMap) {
        saveDynamicEntity("VwDynTable", tableMap);
    }

    default void replaceViewConfig(String viewKey, Map<String, Object> tableMap) {
        removeDynamicTableConfig(viewKey);
        saveViewConfig(viewKey, tableMap);
    }

    void saveDynamicEntity(String entityName, Map<String, Object> values);

    void updateDynamicEntity(String entityName, Map<String, Object> values);

    void removeDynamicEntity(String entityName, Object id);

    default void removeDynamicTableConfig(String viewKey) {
        removeDynamicEntity("VwDynTable", viewKey);
    }

    void removeViewConfig(String viewKey);

    void flushAndClearViewCache(String viewKey);
}
