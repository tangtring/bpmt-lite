package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.core.db.ORMService;
import com.riversoft.platform.po.TbColumn;
import com.riversoft.platform.po.TbTable;
import com.riversoft.platform.po.VwUrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OrmDynamicTableViewRepository implements DynamicTableViewRepository {
    private static final String DYN_VIEW_CLASS = "dyn";

    @SuppressWarnings("unchecked")
    public List<VwUrl> listDynUrls(int start, int limit) {
        return (List<VwUrl>) ORMService.getInstance().queryHQLPage(
                "from " + VwUrl.class.getName() + " where viewClass = ? order by viewKey asc", start, limit,
                DYN_VIEW_CLASS);
    }

    public int countDynUrls() {
        Long count = (Long) ORMService.getInstance().findHQL(
                "select count(1) from " + VwUrl.class.getName() + " where viewClass = ?", DYN_VIEW_CLASS);
        return count == null ? 0 : count.intValue();
    }

    public VwUrl findUrl(String viewKey) {
        return (VwUrl) ORMService.getInstance().findByPk(VwUrl.class.getName(), viewKey);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> findTable(String viewKey) {
        return (Map<String, Object>) ORMService.getInstance().findByPk("VwDynTable", viewKey);
    }

    public Map<String, Object> findTableDefinition(String tableName) {
        TbTable table = (TbTable) ORMService.getInstance().findByPk(TbTable.class.getName(), tableName);
        if (table == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", table.getName());
        result.put("description", table.getDescription());
        result.put("cacheFlag", table.getCacheFlag());
        result.put("columns", toColumnDefinitions(table.getTbColumns()));
        return result;
    }

    public Map<String, Object> findColumnDefinition(String tableName, String columnName) {
        TbTable table = (TbTable) ORMService.getInstance().findByPk(TbTable.class.getName(), tableName);
        if (table == null || table.getTbColumns() == null) {
            return null;
        }
        for (TbColumn column : table.getTbColumns()) {
            if (columnName != null && columnName.equals(column.getName())) {
                return toColumnDefinition(column);
            }
        }
        return null;
    }

    public VwUrl saveUrl(VwUrl url) {
        ORMService.getInstance().savePO(url);
        return url;
    }

    public void updateUrl(VwUrl url) {
        ORMService.getInstance().updatePO(url);
    }

    public void saveDynamicEntity(String entityName, Map<String, Object> values) {
        values.put("$type$", entityName);
        ORMService.getInstance().save(values);
    }

    public void updateDynamicEntity(String entityName, Map<String, Object> values) {
        values.put("$type$", entityName);
        ORMService.getInstance().update(values);
    }

    public void removeDynamicEntity(String entityName, Object id) {
        if (id instanceof Serializable) {
            ORMService.getInstance().removeByPk(entityName, (Serializable) id);
        }
    }

    public void removeViewConfig(String viewKey) {
        ORMService.getInstance().removeByPk("VwDynTable", viewKey);
        ORMService.getInstance().removeByPk(VwUrl.class.getName(), viewKey);
    }

    public void flushAndClearViewCache(String viewKey) {
        ORMService.getInstance().flush();
        ORMService.getInstance().clear();
    }

    private List<Map<String, Object>> toColumnDefinitions(Set<TbColumn> columns) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (columns == null) {
            return result;
        }
        for (TbColumn column : columns) {
            result.add(toColumnDefinition(column));
        }
        return result;
    }

    private Map<String, Object> toColumnDefinition(TbColumn column) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", column.getName());
        result.put("description", column.getDescription());
        result.put("tableName", column.getTableName());
        result.put("typeCode", Integer.valueOf(column.getMappedTypeCode()));
        result.put("totalSize", Integer.valueOf(column.getTotalSize()));
        result.put("scale", Integer.valueOf(column.getScale()));
        result.put("primaryKey", Boolean.valueOf(column.isPrimaryKey()));
        result.put("autoIncrement", Boolean.valueOf(column.isAutoIncrement()));
        result.put("required", Boolean.valueOf(column.isRequired()));
        result.put("defaultValue", column.getDefaultValue());
        result.put("sort", column.getSort());
        result.put("memo", column.getMemo());
        return result;
    }
}
