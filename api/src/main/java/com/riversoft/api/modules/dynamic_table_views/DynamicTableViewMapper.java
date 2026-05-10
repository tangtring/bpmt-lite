package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.platform.po.CmPri;
import com.riversoft.platform.po.VwUrl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    Map<String, Object> toTableMap(DynamicTableViewSnapshot snapshot) {
        DynamicTableViewSnapshot.Base base = snapshot.getBase();
        DynamicTableViewSnapshot.Scripts scripts = snapshot.getScripts();
        Date now = new Date();
        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", snapshot.getViewKey());
        table.put("name", base.getTableName());
        table.put("logTable", base.getLogTableName());
        table.put("busiName", base.getDisplayName());
        table.put("sortName", base.getDefaultSort() == null ? null : base.getDefaultSort().getField());
        table.put("dir", base.getDefaultSort() == null ? null : base.getDefaultSort().getDirection());
        table.put("col", base.getLayoutColumns());
        table.put("pageLimit", base.getPageLimit());
        table.put("initQuery", boolToInt(base.getInitQuery()));
        table.put("listJsType", scriptType(scripts == null ? null : scripts.getList()));
        table.put("listJsScript", scriptText(scripts == null ? null : scripts.getList()));
        table.put("formJsType", scriptType(scripts == null ? null : scripts.getForm()));
        table.put("formJsScript", scriptText(scripts == null ? null : scripts.getForm()));
        table.put("createDate", now);
        table.put("updateDate", now);
        table.put("description", snapshot.getDescription());

        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        table.put("columns", toSystemFieldMaps(snapshot.getViewKey(), fields));
        table.put("showColumns", toComputedFieldMaps(snapshot.getViewKey(), fields));
        table.put("formColumns", toFormFieldMaps(snapshot.getViewKey(), fields));
        table.put("lineColumns", toSectionLineMaps(snapshot.getViewKey(), fields));

        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        table.put("querys", toNormalQueryMaps(snapshot.getViewKey(), queries));
        table.put("extQuerys", toAdvancedQueryMaps(snapshot.getViewKey(), queries));
        table.put("limits", toLimitMaps(snapshot.getViewKey(), snapshot.getLimits()));

        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        table.put("prepareExecs", toPreparedVariableMaps(snapshot.getViewKey(), variables));
        table.put("parents", toParentVariableMaps(snapshot.getViewKey(), variables));

        DynamicTableViewSnapshot.Processors processors = snapshot.getProcessors();
        table.put("beforeExecs", toProcessorMaps(snapshot.getViewKey(), processors == null ? null : processors.getBefore(),
                "VwDynExecBefore"));
        table.put("afterExecs", toProcessorMaps(snapshot.getViewKey(), processors == null ? null : processors.getAfter(),
                "VwDynExecAfter"));

        DynamicTableViewSnapshot.Subviews subviews = snapshot.getSubviews();
        table.put("sysSubs", toSystemTabMaps(snapshot.getViewKey(), subviews));
        table.put("viewSubs", toViewTabMaps(snapshot.getViewKey(), subviews));

        DynamicTableViewSnapshot.Buttons buttons = snapshot.getButtons();
        table.put("sysBtns", toSystemButtonMaps(snapshot.getViewKey(), buttons));
        table.put("itemBtns", toCustomButtonMaps(snapshot.getViewKey(), buttons == null ? null : buttons.getItem(),
                "VwDynBtnItem"));
        table.put("summaryBtns", toCustomButtonMaps(snapshot.getViewKey(), buttons == null ? null : buttons.getSummary(),
                "VwDynBtnSummary"));
        table.put("weixin", toWeixinMap(snapshot.getViewKey(), snapshot.getWeixin()));
        return table;
    }

    private Set<Map<String, Object>> toSystemFieldMaps(String viewKey, DynamicTableViewSnapshot.Fields fields) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (fields == null || fields.getSystemFields() == null) {
            return result;
        }
        List<String> listOrder = fields.getListOrder();
        for (int i = 0; i < fields.getSystemFields().size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.getSystemFields().get(i);
            if (field == null) {
                continue;
            }
            Map<String, Object> row = fieldBase(viewKey, field, "VwDynColumn", i);
            row.put("name", field.getName());
            row.put("showFlag", boolToInt(field.getShowInDetail()));
            row.put("formFlag", boolToInt(field.getShowInForm()));
            row.put("contentType", scriptType(field.getContent(), Integer.valueOf(3)));
            row.put("contentScript", scriptText(field.getContent(), ""));
            row.put("widgetContentType", scriptType(field.getWidgetContent()));
            row.put("widgetContentScript", scriptText(field.getWidgetContent()));
            row.put("listSort", Integer.valueOf(listSort(listOrder, field.getName(), field.getShowInList())));
            row.put("pri", pri(permission(field.getPermissions(), "view"), row, "主表字段", "查看"));
            row.put("createPri", pri(permission(field.getPermissions(), "create"), row, "主表字段", "录入"));
            row.put("updatePri", pri(permission(field.getPermissions(), "update"), row, "主表字段", "编辑"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toComputedFieldMaps(String viewKey, DynamicTableViewSnapshot.Fields fields) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (fields == null || fields.getComputedFields() == null) {
            return result;
        }
        List<String> listOrder = fields.getListOrder();
        for (int i = 0; i < fields.getComputedFields().size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.getComputedFields().get(i);
            if (field == null) {
                continue;
            }
            Map<String, Object> row = fieldBase(viewKey, field, "VwDynColumnShow", i);
            row.put("sortField", field.getName());
            row.put("contentType", scriptType(field.getContent(), Integer.valueOf(3)));
            row.put("contentScript", scriptText(field.getContent(), ""));
            row.put("listSort", Integer.valueOf(listSort(listOrder, fieldReference(field), field.getShowInList())));
            row.put("pri", pri(permission(field.getPermissions(), "view"), row, "展示字段", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toFormFieldMaps(String viewKey, DynamicTableViewSnapshot.Fields fields) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (fields == null || fields.getFormFields() == null) {
            return result;
        }
        for (int i = 0; i < fields.getFormFields().size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.getFormFields().get(i);
            if (field == null) {
                continue;
            }
            Map<String, Object> row = fieldBase(viewKey, field, "VwDynColumnForm", i);
            row.put("name", fieldReference(field));
            row.put("contentType", scriptType(field.getContent(), Integer.valueOf(3)));
            row.put("contentScript", scriptText(field.getContent(), ""));
            row.put("pri", pri(permission(field.getPermissions(), "view"), row, "表单字段", "查看"));
            row.put("editPri", pri(permission(field.getPermissions(), "update"), row, "表单字段", "编辑"));
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> fieldBase(String viewKey,
                                          DynamicTableViewSnapshot.Field field,
                                          String type,
                                          int sort) {
        Map<String, Object> row = typed(type);
        row.put("viewKey", viewKey);
        row.put("busiName", field.getDisplayName());
        row.put("tipType", scriptType(field.getTip()));
        row.put("tipScript", scriptText(field.getTip()));
        row.put("style", field.getStyle());
        row.put("whole", boolToInt(field.getWholeLine()));
        row.put("widget", field.getWidget());
        row.put("widgetParamType", scriptType(field.getWidgetParam()));
        row.put("widgetParamScript", scriptText(field.getWidgetParam()));
        row.put("sort", Integer.valueOf(sort));
        return row;
    }

    private Set<Map<String, Object>> toSectionLineMaps(String viewKey, DynamicTableViewSnapshot.Fields fields) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (fields == null || fields.getSectionLines() == null) {
            return result;
        }
        for (int i = 0; i < fields.getSectionLines().size(); i++) {
            DynamicTableViewSnapshot.SectionLine line = fields.getSectionLines().get(i);
            if (line == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynColumnLine");
            row.put("viewKey", viewKey);
            row.put("busiName", line.getDisplayName());
            row.put("style", line.getStyle());
            row.put("expandFlag", Integer.valueOf(1));
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(line.getPermissions(), "view"), row, "字段分割线", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toNormalQueryMaps(String viewKey, DynamicTableViewSnapshot.Queries queries) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (queries == null || queries.getNormal() == null) {
            return result;
        }
        for (int i = 0; i < queries.getNormal().size(); i++) {
            DynamicTableViewSnapshot.NormalQuery query = queries.getNormal().get(i);
            if (query == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynQuery");
            row.put("viewKey", viewKey);
            row.put("name", query.getField());
            row.put("busiName", query.getDisplayName());
            row.put("widget", query.getWidget());
            row.put("widgetParamType", scriptType(query.getWidgetParam()));
            row.put("widgetParamScript", scriptText(query.getWidgetParam()));
            row.put("defVal", query.getDefaultValue());
            row.put("sort", Integer.valueOf(i));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toAdvancedQueryMaps(String viewKey, DynamicTableViewSnapshot.Queries queries) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (queries == null || queries.getAdvanced() == null) {
            return result;
        }
        for (int i = 0; i < queries.getAdvanced().size(); i++) {
            DynamicTableViewSnapshot.AdvancedQuery query = queries.getAdvanced().get(i);
            if (query == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynQueryExt");
            row.put("viewKey", viewKey);
            row.put("name", query.getName());
            row.put("busiName", query.getDisplayName());
            row.put("widget", query.getWidget());
            row.put("widgetParamType", scriptType(query.getWidgetParam()));
            row.put("widgetParamScript", scriptText(query.getWidgetParam()));
            row.put("defVal", query.getDefaultValue());
            row.put("sqlType", scriptType(query.getSql()));
            row.put("sqlScript", scriptText(query.getSql()));
            row.put("description", query.getDescription());
            row.put("sort", Integer.valueOf(i));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toLimitMaps(String viewKey, List<DynamicTableViewSnapshot.Limit> limits) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (limits == null) {
            return result;
        }
        for (int i = 0; i < limits.size(); i++) {
            DynamicTableViewSnapshot.Limit limit = limits.get(i);
            if (limit == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynLimit");
            row.put("viewKey", viewKey);
            row.put("description", limit.getDescription());
            row.put("sqlType", scriptType(limit.getSql()));
            row.put("sqlScript", scriptText(limit.getSql()));
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(limit.getPermissions(), "view"), row, "数据限制", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toPreparedVariableMaps(String viewKey, DynamicTableViewSnapshot.Variables variables) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (variables == null || variables.getPrepared() == null) {
            return result;
        }
        for (int i = 0; i < variables.getPrepared().size(); i++) {
            DynamicTableViewSnapshot.PreparedVariable variable = variables.getPrepared().get(i);
            if (variable == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynExecPrepare");
            row.put("viewKey", viewKey);
            row.put("var", variable.getVar());
            row.put("description", variable.getDescription());
            row.put("execType", scriptType(variable.getExec()));
            row.put("execScript", scriptText(variable.getExec()));
            row.put("sort", Integer.valueOf(i));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toParentVariableMaps(String viewKey, DynamicTableViewSnapshot.Variables variables) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (variables == null || variables.getParents() == null) {
            return result;
        }
        for (int i = 0; i < variables.getParents().size(); i++) {
            DynamicTableViewSnapshot.ParentVariable variable = variables.getParents().get(i);
            if (variable == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynParent");
            row.put("parentKey", variable.getKey());
            row.put("viewKey", viewKey);
            row.put("tableName", variable.getTableName());
            row.put("var", variable.getVar());
            row.put("description", variable.getDescription());
            row.put("sort", Integer.valueOf(i));
            row.put("foreigns", toForeignMaps(variable));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toForeignMaps(DynamicTableViewSnapshot.ParentVariable variable) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (variable.getForeigns() == null) {
            return result;
        }
        for (int i = 0; i < variable.getForeigns().size(); i++) {
            DynamicTableViewSnapshot.Foreign foreign = variable.getForeigns().get(i);
            if (foreign == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynParentForeign");
            row.put("parentKey", variable.getKey());
            row.put("mainColumn", foreign.getMainColumn());
            row.put("parentColumn", foreign.getParentColumn());
            row.put("description", foreign.getDescription());
            row.put("sort", Integer.valueOf(i));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toProcessorMaps(String viewKey,
                                                     List<DynamicTableViewSnapshot.Processor> processors,
                                                     String type) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (processors == null) {
            return result;
        }
        for (int i = 0; i < processors.size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.get(i);
            if (processor == null) {
                continue;
            }
            Map<String, Object> row = typed(type);
            row.put("viewKey", viewKey);
            row.put("description", processor.getDescription());
            row.put("execType", scriptType(processor.getExec()));
            row.put("execScript", scriptText(processor.getExec()));
            row.put("sort", Integer.valueOf(i));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toSystemTabMaps(String viewKey, DynamicTableViewSnapshot.Subviews subviews) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (subviews == null || subviews.getSystemTabs() == null) {
            return result;
        }
        for (int i = 0; i < subviews.getSystemTabs().size(); i++) {
            DynamicTableViewSnapshot.SystemTab tab = subviews.getSystemTabs().get(i);
            if (tab == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynSubSys");
            row.put("subKey", viewKey + ".sys." + tab.getName());
            row.put("viewKey", viewKey);
            row.put("name", tab.getName());
            row.put("busiName", tab.getDisplayName());
            row.put("style", tab.getStyle());
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(tab.getPermissions(), "view"), row, "系统页签", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toViewTabMaps(String viewKey, DynamicTableViewSnapshot.Subviews subviews) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (subviews == null || subviews.getViewTabs() == null) {
            return result;
        }
        for (int i = 0; i < subviews.getViewTabs().size(); i++) {
            DynamicTableViewSnapshot.ViewTab tab = subviews.getViewTabs().get(i);
            if (tab == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynSubView");
            row.put("subKey", tab.getKey());
            row.put("viewKey", viewKey);
            row.put("busiName", tab.getDisplayName());
            row.put("style", tab.getStyle());
            row.put("action", tab.getAction());
            row.put("paramType", scriptType(tab.getParam()));
            row.put("paramScript", scriptText(tab.getParam()));
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(tab.getPermissions(), "view"), row, "视图页签", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toSystemButtonMaps(String viewKey, DynamicTableViewSnapshot.Buttons buttons) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (buttons == null || buttons.getSystem() == null) {
            return result;
        }
        for (int i = 0; i < buttons.getSystem().size(); i++) {
            DynamicTableViewSnapshot.SystemButton button = buttons.getSystem().get(i);
            if (button == null) {
                continue;
            }
            Map<String, Object> row = typed("VwDynBtnSys");
            row.put("viewKey", viewKey);
            row.put("type", button.getType());
            row.put("name", button.getName());
            row.put("busiName", button.getDisplayName());
            row.put("icon", button.getIcon());
            row.put("styleClass", button.getStyleClass());
            row.put("description", button.getDescription());
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(button.getPermissions(), "view"), row, "系统按钮", "查看"));
            result.add(row);
        }
        return result;
    }

    private Set<Map<String, Object>> toCustomButtonMaps(String viewKey,
                                                        List<DynamicTableViewSnapshot.CustomButton> buttons,
                                                        String type) {
        Set<Map<String, Object>> result = new LinkedHashSet<Map<String, Object>>();
        if (buttons == null) {
            return result;
        }
        for (int i = 0; i < buttons.size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.get(i);
            if (button == null) {
                continue;
            }
            Map<String, Object> row = typed(type);
            row.put("viewKey", viewKey);
            row.put("busiName", button.getDisplayName());
            row.put("icon", button.getIcon());
            row.put("action", button.getAction());
            row.put("openType", button.getOpenType());
            row.put("description", button.getDescription());
            row.put("paramType", scriptType(button.getParam()));
            row.put("paramScript", scriptText(button.getParam()));
            row.put("confirmMsg", button.getConfirmMessage());
            row.put("sort", Integer.valueOf(i));
            row.put("pri", pri(permission(button.getPermissions(), "view"), row, "自定义按钮", "查看"));
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> toWeixinMap(String viewKey, DynamicTableViewSnapshot.Weixin weixin) {
        if (weixin == null) {
            return null;
        }
        Map<String, Object> row = typed("VwDynWeixin");
        row.put("viewKey", viewKey);
        row.put("listMode", weixin.getListMode());
        row.put("urlMode", weixin.getUrlMode());
        row.put("titleType", scriptType(weixin.getTitle()));
        row.put("titleScript", scriptText(weixin.getTitle()));
        row.put("imgType", scriptType(weixin.getImage()));
        row.put("imgScript", scriptText(weixin.getImage()));
        row.put("desType", scriptType(weixin.getDescription()));
        row.put("desScript", scriptText(weixin.getDescription()));
        row.put("dateType", scriptType(weixin.getDate()));
        row.put("dateScript", scriptText(weixin.getDate()));
        row.put("pri", pri(permission(weixin.getPermissions(), "view"), row, "微信配置", "查看"));
        return row;
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

    private Map<String, Object> typed(String type) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("$type$", type);
        return row;
    }

    private int listSort(List<String> listOrder, String field, Boolean showInList) {
        if (!Boolean.TRUE.equals(showInList) || field == null || listOrder == null) {
            return -1;
        }
        int index = listOrder.indexOf(field);
        return index < 0 ? -1 : index;
    }

    private String fieldReference(DynamicTableViewSnapshot.Field field) {
        if (field == null) {
            return null;
        }
        return field.getName() == null ? field.getKey() : field.getName();
    }

    private Integer boolToInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? Integer.valueOf(1) : Integer.valueOf(0);
    }

    private Integer scriptType(DynamicTableViewSnapshot.ScriptValue value) {
        return scriptType(value, null);
    }

    private Integer scriptType(DynamicTableViewSnapshot.ScriptValue value, Integer defaultValue) {
        return value == null || value.getType() == null ? defaultValue : value.getType();
    }

    private String scriptText(DynamicTableViewSnapshot.ScriptValue value) {
        return scriptText(value, null);
    }

    private String scriptText(DynamicTableViewSnapshot.ScriptValue value, String defaultValue) {
        return value == null || value.getScript() == null ? defaultValue : value.getScript();
    }

    private String permission(DynamicTableViewSnapshot.PermissionSet permissions, String action) {
        if (permissions == null) {
            return null;
        }
        if ("create".equals(action)) {
            return permissions.getCreate();
        }
        if ("update".equals(action)) {
            return permissions.getUpdate();
        }
        return permissions.getView();
    }

    private CmPri pri(String priKey, Map<String, Object> owner, String... labels) {
        if (priKey == null || priKey.trim().length() == 0) {
            return null;
        }
        CmPri pri = new CmPri();
        pri.setPriKey(priKey);
        pri.setType(Integer.valueOf(1));
        pri.setCheckType(Integer.valueOf(2));
        pri.setCheckScript("${true}");
        pri.setDevelopmentInfo(owner, labels);
        if (pri.getCatelogType() == null) {
            pri.setCatelogType((Integer) CmPri.Catelog.VIEW.getCode());
        }
        if (pri.getCatelogKey() == null || pri.getCatelogKey().trim().length() == 0) {
            pri.setCatelogKey(stringValue(owner, "viewKey"));
        }
        if (pri.getBusiName() == null || pri.getBusiName().trim().length() == 0) {
            pri.setBusiName(priKey);
        }
        return pri;
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
