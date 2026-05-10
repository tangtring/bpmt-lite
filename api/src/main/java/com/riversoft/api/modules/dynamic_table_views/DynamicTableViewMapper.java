package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.platform.po.CmPri;
import com.riversoft.platform.po.VwUrl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DynamicTableViewMapper {
    private enum FieldSource {
        SYSTEM,
        COMPUTED,
        FORM
    }

    DynamicTableViewSnapshot toSnapshot(VwUrl url, Map<String, Object> table) {
        Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
        if (url != null) {
            urlMap.put("viewKey", url.getViewKey());
            urlMap.put("viewClass", url.getViewClass());
            urlMap.put("description", url.getDescription());
            urlMap.put("loginType", url.getLoginType());
        }
        return toSnapshot(urlMap, table);
    }

    DynamicTableViewSnapshot toSnapshot(Map<String, Object> url, Map<String, Object> table) {
        String viewKey = firstString(url, table, "viewKey");
        if (table == null || table.isEmpty()) {
            throw DynamicTableViewErrors.notFound(viewKey);
        }

        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        snapshot.setViewKey(viewKey);
        snapshot.setDescription(stringValue(url, "description"));
        snapshot.setLoginRequired(boolFromInt(intValue(url, "loginType"), true));
        snapshot.setBase(toBase(table));
        snapshot.setFields(toFields(table));
        snapshot.setQueries(toQueries(table));
        snapshot.setLimits(toLimits(table));
        snapshot.setVariables(toVariables(table));
        snapshot.setProcessors(toProcessors(table));
        snapshot.setSubviews(toSubviews(table));
        snapshot.setButtons(toButtons(table));
        snapshot.setWeixin(toWeixin(asMap(table.get("weixin"))));
        snapshot.setScripts(toScripts(table));
        return snapshot;
    }

    private DynamicTableViewSnapshot.Base toBase(Map<String, Object> table) {
        DynamicTableViewSnapshot.Base base = new DynamicTableViewSnapshot.Base();
        base.setTableName(stringValue(table, "name"));
        base.setDisplayName(stringValue(table, "busiName"));
        base.setLogTableName(stringValue(table, "logTable"));
        base.setLayoutColumns(intValue(table, "col"));
        base.setPageLimit(intValue(table, "pageLimit"));
        base.setInitQuery(Boolean.valueOf(boolFromInt(intValue(table, "initQuery"), false)));
        DynamicTableViewSnapshot.Sort sort = new DynamicTableViewSnapshot.Sort();
        sort.setField(stringValue(table, "sortName"));
        sort.setDirection(stringValue(table, "dir"));
        base.setDefaultSort(sort);
        return base;
    }

    private DynamicTableViewSnapshot.Fields toFields(Map<String, Object> table) {
        DynamicTableViewSnapshot.Fields fields = new DynamicTableViewSnapshot.Fields();
        fields.setSystemFields(toFields(collectionValue(table, "columns"), FieldSource.SYSTEM));
        fields.setComputedFields(toFields(collectionValue(table, "showColumns"), FieldSource.COMPUTED));
        fields.setFormFields(toFields(collectionValue(table, "formColumns"), FieldSource.FORM));
        fields.setSectionLines(toSectionLines(collectionValue(table, "lineColumns")));
        fields.setListOrder(toListOrder(table));
        return fields;
    }

    private List<DynamicTableViewSnapshot.Field> toFields(List<Map<String, Object>> rows, FieldSource source) {
        List<DynamicTableViewSnapshot.Field> result = new ArrayList<DynamicTableViewSnapshot.Field>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
            field.setKey(firstString(row, null, "id", "key", "subKey"));
            field.setName(firstString(row, null, "name", "sortField"));
            field.setDisplayName(stringValue(row, "busiName"));
            field.setShowInDetail(Boolean.valueOf(showInDetail(row, source)));
            field.setShowInForm(Boolean.valueOf(showInForm(row, source)));
            field.setShowInList(Boolean.valueOf(showInList(row)));
            field.setWidget(stringValue(row, "widget"));
            field.setContent(scriptValue(row, "contentType", "contentScript"));
            field.setWidgetParam(scriptValue(row, "widgetParamType", "widgetParamScript"));
            field.setWidgetContent(scriptValue(row, "widgetContentType", "widgetContentScript"));
            field.setTip(scriptValue(row, "tipType", "tipScript"));
            field.setBeforeSave(scriptValue(row, "execType", "execScript"));
            field.setStyle(stringValue(row, "style"));
            field.setWholeLine(Boolean.valueOf(boolFromInt(intValue(row, "whole"), false)));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            permissions.setCreate(permissionValue(row.get(source == FieldSource.SYSTEM ? "createPri" : "editPri")));
            permissions.setUpdate(permissionValue(row.get("updatePri")));
            field.setPermissions(permissions);
            result.add(field);
        }
        return result;
    }

    private boolean showInDetail(Map<String, Object> row, FieldSource source) {
        return boolFromInt(intValue(row, "showFlag"), source != FieldSource.FORM);
    }

    private boolean showInForm(Map<String, Object> row, FieldSource source) {
        return boolFromInt(intValue(row, "formFlag"), source == FieldSource.FORM);
    }

    private boolean showInList(Map<String, Object> row) {
        Integer listSort = intValue(row, "listSort");
        return listSort != null && listSort.intValue() >= 0;
    }

    private List<DynamicTableViewSnapshot.SectionLine> toSectionLines(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.SectionLine> result = new ArrayList<DynamicTableViewSnapshot.SectionLine>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.SectionLine line = new DynamicTableViewSnapshot.SectionLine();
            line.setKey(firstString(row, null, "id", "key"));
            line.setDisplayName(stringValue(row, "busiName"));
            line.setStyle(stringValue(row, "style"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            line.setPermissions(permissions);
            result.add(line);
        }
        return result;
    }

    private List<String> toListOrder(Map<String, Object> table) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.addAll(collectionValue(table, "columns"));
        rows.addAll(collectionValue(table, "showColumns"));
        rows = sortByInt(rows, "listSort");
        List<String> result = new ArrayList<String>();
        for (Map<String, Object> row : rows) {
            if (!showInList(row)) {
                continue;
            }
            String name = firstString(row, null, "name", "key", "sortField");
            if (name != null) {
                result.add(name);
            }
        }
        return result;
    }

    private DynamicTableViewSnapshot.Queries toQueries(Map<String, Object> table) {
        DynamicTableViewSnapshot.Queries queries = new DynamicTableViewSnapshot.Queries();
        queries.setNormal(toNormalQueries(collectionValue(table, "querys")));
        queries.setAdvanced(toAdvancedQueries(collectionValue(table, "extQuerys")));
        return queries;
    }

    private List<DynamicTableViewSnapshot.NormalQuery> toNormalQueries(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.NormalQuery> result = new ArrayList<DynamicTableViewSnapshot.NormalQuery>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.NormalQuery query = new DynamicTableViewSnapshot.NormalQuery();
            query.setKey(firstString(row, null, "id", "key"));
            query.setField(stringValue(row, "name"));
            query.setDisplayName(stringValue(row, "busiName"));
            query.setWidget(stringValue(row, "widget"));
            query.setWidgetParam(scriptValue(row, "widgetParamType", "widgetParamScript"));
            query.setDefaultValue(stringValue(row, "defVal"));
            result.add(query);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.AdvancedQuery> toAdvancedQueries(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.AdvancedQuery> result = new ArrayList<DynamicTableViewSnapshot.AdvancedQuery>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.AdvancedQuery query = new DynamicTableViewSnapshot.AdvancedQuery();
            query.setKey(firstString(row, null, "id", "key"));
            query.setName(stringValue(row, "name"));
            query.setDisplayName(stringValue(row, "busiName"));
            query.setWidget(stringValue(row, "widget"));
            query.setWidgetParam(scriptValue(row, "widgetParamType", "widgetParamScript"));
            query.setDefaultValue(stringValue(row, "defVal"));
            query.setSql(scriptValue(row, "sqlType", "sqlScript"));
            query.setDescription(stringValue(row, "description"));
            result.add(query);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.Limit> toLimits(Map<String, Object> table) {
        List<DynamicTableViewSnapshot.Limit> result = new ArrayList<DynamicTableViewSnapshot.Limit>();
        for (Map<String, Object> row : sortByInt(collectionValue(table, "limits"), "sort")) {
            DynamicTableViewSnapshot.Limit limit = new DynamicTableViewSnapshot.Limit();
            limit.setKey(firstString(row, null, "id", "key"));
            limit.setDescription(stringValue(row, "description"));
            limit.setSql(scriptValue(row, "sqlType", "sqlScript"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            limit.setPermissions(permissions);
            result.add(limit);
        }
        return result;
    }

    private DynamicTableViewSnapshot.Variables toVariables(Map<String, Object> table) {
        DynamicTableViewSnapshot.Variables variables = new DynamicTableViewSnapshot.Variables();
        variables.setPrepared(toPreparedVariables(collectionValue(table, "prepareExecs")));
        variables.setParents(toParentVariables(collectionValue(table, "parents")));
        return variables;
    }

    private List<DynamicTableViewSnapshot.PreparedVariable> toPreparedVariables(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.PreparedVariable> result = new ArrayList<DynamicTableViewSnapshot.PreparedVariable>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.PreparedVariable variable = new DynamicTableViewSnapshot.PreparedVariable();
            variable.setKey(firstString(row, null, "id", "key"));
            variable.setVar(stringValue(row, "var"));
            variable.setDescription(stringValue(row, "description"));
            variable.setExec(scriptValue(row, "execType", "execScript"));
            result.add(variable);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.ParentVariable> toParentVariables(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.ParentVariable> result = new ArrayList<DynamicTableViewSnapshot.ParentVariable>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.ParentVariable variable = new DynamicTableViewSnapshot.ParentVariable();
            variable.setKey(firstString(row, null, "parentKey", "key"));
            variable.setTableName(stringValue(row, "tableName"));
            variable.setVar(stringValue(row, "var"));
            variable.setDescription(stringValue(row, "description"));
            variable.setForeigns(toForeigns(collectionValue(row, "foreigns")));
            result.add(variable);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.Foreign> toForeigns(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.Foreign> result = new ArrayList<DynamicTableViewSnapshot.Foreign>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.Foreign foreign = new DynamicTableViewSnapshot.Foreign();
            foreign.setMainColumn(stringValue(row, "mainColumn"));
            foreign.setParentColumn(stringValue(row, "parentColumn"));
            foreign.setDescription(stringValue(row, "description"));
            result.add(foreign);
        }
        return result;
    }

    private DynamicTableViewSnapshot.Processors toProcessors(Map<String, Object> table) {
        DynamicTableViewSnapshot.Processors processors = new DynamicTableViewSnapshot.Processors();
        processors.setBefore(toProcessors(collectionValue(table, "beforeExecs")));
        processors.setAfter(toProcessors(collectionValue(table, "afterExecs")));
        return processors;
    }

    private List<DynamicTableViewSnapshot.Processor> toProcessors(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.Processor> result = new ArrayList<DynamicTableViewSnapshot.Processor>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.Processor processor = new DynamicTableViewSnapshot.Processor();
            processor.setKey(firstString(row, null, "id", "key"));
            processor.setDescription(stringValue(row, "description"));
            processor.setExec(scriptValue(row, "execType", "execScript"));
            result.add(processor);
        }
        return result;
    }

    private DynamicTableViewSnapshot.Subviews toSubviews(Map<String, Object> table) {
        DynamicTableViewSnapshot.Subviews subviews = new DynamicTableViewSnapshot.Subviews();
        subviews.setSystemTabs(toSystemTabs(collectionValue(table, "sysSubs")));
        subviews.setViewTabs(toViewTabs(collectionValue(table, "viewSubs")));
        return subviews;
    }

    private List<DynamicTableViewSnapshot.SystemTab> toSystemTabs(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.SystemTab> result = new ArrayList<DynamicTableViewSnapshot.SystemTab>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.SystemTab tab = new DynamicTableViewSnapshot.SystemTab();
            tab.setName(stringValue(row, "name"));
            tab.setDisplayName(stringValue(row, "busiName"));
            tab.setStyle(stringValue(row, "style"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            tab.setPermissions(permissions);
            result.add(tab);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.ViewTab> toViewTabs(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.ViewTab> result = new ArrayList<DynamicTableViewSnapshot.ViewTab>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.ViewTab tab = new DynamicTableViewSnapshot.ViewTab();
            tab.setKey(firstString(row, null, "subKey", "key"));
            tab.setDisplayName(stringValue(row, "busiName"));
            tab.setStyle(stringValue(row, "style"));
            tab.setAction(stringValue(row, "action"));
            tab.setParam(scriptValue(row, "paramType", "paramScript"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            tab.setPermissions(permissions);
            result.add(tab);
        }
        return result;
    }

    private DynamicTableViewSnapshot.Buttons toButtons(Map<String, Object> table) {
        DynamicTableViewSnapshot.Buttons buttons = new DynamicTableViewSnapshot.Buttons();
        buttons.setSystem(toSystemButtons(collectionValue(table, "sysBtns")));
        buttons.setItem(toCustomButtons(collectionValue(table, "itemBtns")));
        buttons.setSummary(toCustomButtons(collectionValue(table, "summaryBtns")));
        return buttons;
    }

    private List<DynamicTableViewSnapshot.SystemButton> toSystemButtons(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.SystemButton> result = new ArrayList<DynamicTableViewSnapshot.SystemButton>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
            button.setName(stringValue(row, "name"));
            button.setType(intValue(row, "type"));
            button.setDisplayName(stringValue(row, "busiName"));
            button.setIcon(stringValue(row, "icon"));
            button.setStyleClass(stringValue(row, "styleClass"));
            button.setDescription(stringValue(row, "description"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            button.setPermissions(permissions);
            result.add(button);
        }
        return result;
    }

    private List<DynamicTableViewSnapshot.CustomButton> toCustomButtons(List<Map<String, Object>> rows) {
        List<DynamicTableViewSnapshot.CustomButton> result = new ArrayList<DynamicTableViewSnapshot.CustomButton>();
        for (Map<String, Object> row : sortByInt(rows, "sort")) {
            DynamicTableViewSnapshot.CustomButton button = new DynamicTableViewSnapshot.CustomButton();
            button.setKey(firstString(row, null, "id", "key"));
            button.setDisplayName(stringValue(row, "busiName"));
            button.setIcon(stringValue(row, "icon"));
            button.setAction(stringValue(row, "action"));
            button.setOpenType(intValue(row, "openType"));
            button.setDescription(stringValue(row, "description"));
            button.setParam(scriptValue(row, "paramType", "paramScript"));
            button.setConfirmMessage(stringValue(row, "confirmMsg"));
            DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
            permissions.setView(permissionValue(row.get("pri")));
            button.setPermissions(permissions);
            result.add(button);
        }
        return result;
    }

    private DynamicTableViewSnapshot.Weixin toWeixin(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        DynamicTableViewSnapshot.Weixin weixin = new DynamicTableViewSnapshot.Weixin();
        weixin.setListMode(intValue(row, "listMode"));
        weixin.setUrlMode(intValue(row, "urlMode"));
        weixin.setTitle(scriptValue(row, "titleType", "titleScript"));
        weixin.setImage(scriptValue(row, "imgType", "imgScript"));
        weixin.setDescription(scriptValue(row, "desType", "desScript"));
        weixin.setDate(scriptValue(row, "dateType", "dateScript"));
        DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
        permissions.setView(permissionValue(row.get("pri")));
        weixin.setPermissions(permissions);
        return weixin;
    }

    private DynamicTableViewSnapshot.Scripts toScripts(Map<String, Object> table) {
        DynamicTableViewSnapshot.Scripts scripts = new DynamicTableViewSnapshot.Scripts();
        scripts.setList(scriptValue(table, "listJsType", "listJsScript"));
        scripts.setForm(scriptValue(table, "formJsType", "formJsScript"));
        return scripts;
    }

    private String firstString(Map<String, Object> first, Map<String, Object> second, String... keys) {
        for (String key : keys) {
            String value = stringValue(first, key);
            if (value != null) {
                return value;
            }
            value = stringValue(second, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }

    private boolean boolFromInt(Integer value, boolean defaultValue) {
        return value == null ? defaultValue : value.intValue() == 1;
    }

    private DynamicTableViewSnapshot.ScriptValue scriptValue(Map<String, Object> map, String typeKey, String scriptKey) {
        Integer type = intValue(map, typeKey);
        String script = stringValue(map, scriptKey);
        if (type == null && script == null) {
            return null;
        }
        DynamicTableViewSnapshot.ScriptValue value = new DynamicTableViewSnapshot.ScriptValue();
        value.setType(type);
        value.setScript(script);
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> collectionValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (value == null) {
            return result;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<Object>) value) {
                Map<String, Object> itemMap = asMap(item);
                if (itemMap != null) {
                    result.add(itemMap);
                }
            }
            return result;
        }
        Map<String, Object> itemMap = asMap(value);
        if (itemMap != null) {
            result.add(itemMap);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private List<Map<String, Object>> sortByInt(List<Map<String, Object>> rows, final String key) {
        List<Map<String, Object>> sorted = new ArrayList<Map<String, Object>>(rows);
        Collections.sort(sorted, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                Integer leftValue = intValue(left, key);
                Integer rightValue = intValue(right, key);
                if (leftValue == null && rightValue == null) {
                    return 0;
                }
                if (leftValue == null) {
                    return 1;
                }
                if (rightValue == null) {
                    return -1;
                }
                return leftValue.compareTo(rightValue);
            }
        });
        return sorted;
    }

    private String permissionValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof CmPri) {
            return ((CmPri) value).getPriKey();
        }
        Map<String, Object> map = asMap(value);
        if (map != null) {
            String priKey = firstString(map, null, "priKey", "id", "key", "name");
            return priKey == null ? String.valueOf(value) : priKey;
        }
        String reflected = reflectedValue(value, "getPriKey");
        if (reflected != null) {
            return reflected;
        }
        reflected = reflectedValue(value, "getId");
        return reflected == null ? String.valueOf(value) : reflected;
    }

    private String reflectedValue(Object value, String methodName) {
        try {
            Method method = value.getClass().getMethod(methodName);
            Object result = method.invoke(value);
            return result == null ? null : String.valueOf(result);
        } catch (Exception e) {
            return null;
        }
    }
}
