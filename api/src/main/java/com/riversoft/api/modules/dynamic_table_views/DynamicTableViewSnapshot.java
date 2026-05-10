package com.riversoft.api.modules.dynamic_table_views;

import java.util.ArrayList;
import java.util.List;

public class DynamicTableViewSnapshot {
    private String viewKey;
    private String description;
    private boolean loginRequired = true;
    private Base base = new Base();
    private Fields fields = new Fields();
    private Queries queries = new Queries();
    private List<Limit> limits = new ArrayList<Limit>();
    private Variables variables = new Variables();
    private Processors processors = new Processors();
    private Subviews subviews = new Subviews();
    private Buttons buttons = new Buttons();
    private Weixin weixin;
    private Scripts scripts = new Scripts();

    public String getViewKey() {
        return viewKey;
    }

    public void setViewKey(String viewKey) {
        this.viewKey = viewKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLoginRequired() {
        return loginRequired;
    }

    public void setLoginRequired(boolean loginRequired) {
        this.loginRequired = loginRequired;
    }

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    public Queries getQueries() {
        return queries;
    }

    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    public List<Limit> getLimits() {
        return limits;
    }

    public void setLimits(List<Limit> limits) {
        this.limits = limits;
    }

    public Variables getVariables() {
        return variables;
    }

    public void setVariables(Variables variables) {
        this.variables = variables;
    }

    public Processors getProcessors() {
        return processors;
    }

    public void setProcessors(Processors processors) {
        this.processors = processors;
    }

    public Subviews getSubviews() {
        return subviews;
    }

    public void setSubviews(Subviews subviews) {
        this.subviews = subviews;
    }

    public Buttons getButtons() {
        return buttons;
    }

    public void setButtons(Buttons buttons) {
        this.buttons = buttons;
    }

    public Weixin getWeixin() {
        return weixin;
    }

    public void setWeixin(Weixin weixin) {
        this.weixin = weixin;
    }

    public Scripts getScripts() {
        return scripts;
    }

    public void setScripts(Scripts scripts) {
        this.scripts = scripts;
    }

    public static class Base {
        private String tableName;
        private String displayName;
        private String logTableName;
        private Integer layoutColumns;
        private Boolean initQuery;
        private Integer pageLimit;
        private Sort defaultSort = new Sort();

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getLogTableName() {
            return logTableName;
        }

        public void setLogTableName(String logTableName) {
            this.logTableName = logTableName;
        }

        public Integer getLayoutColumns() {
            return layoutColumns;
        }

        public void setLayoutColumns(Integer layoutColumns) {
            this.layoutColumns = layoutColumns;
        }

        public Boolean getInitQuery() {
            return initQuery;
        }

        public void setInitQuery(Boolean initQuery) {
            this.initQuery = initQuery;
        }

        public Integer getPageLimit() {
            return pageLimit;
        }

        public void setPageLimit(Integer pageLimit) {
            this.pageLimit = pageLimit;
        }

        public Sort getDefaultSort() {
            return defaultSort;
        }

        public void setDefaultSort(Sort defaultSort) {
            this.defaultSort = defaultSort;
        }
    }

    public static class Sort {
        private String field;
        private String direction;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    public static class ScriptValue {
        private Integer type;
        private String script;

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }

    public static class PermissionSet {
        private String view;
        private String create;
        private String update;

        public String getView() {
            return view;
        }

        public void setView(String view) {
            this.view = view;
        }

        public String getCreate() {
            return create;
        }

        public void setCreate(String create) {
            this.create = create;
        }

        public String getUpdate() {
            return update;
        }

        public void setUpdate(String update) {
            this.update = update;
        }
    }

    public static class Fields {
        private List<Field> systemFields = new ArrayList<Field>();
        private List<Field> computedFields = new ArrayList<Field>();
        private List<Field> formFields = new ArrayList<Field>();
        private List<SectionLine> sectionLines = new ArrayList<SectionLine>();
        private List<String> listOrder = new ArrayList<String>();

        public List<Field> getSystemFields() {
            return systemFields;
        }

        public void setSystemFields(List<Field> systemFields) {
            this.systemFields = systemFields;
        }

        public List<Field> getComputedFields() {
            return computedFields;
        }

        public void setComputedFields(List<Field> computedFields) {
            this.computedFields = computedFields;
        }

        public List<Field> getFormFields() {
            return formFields;
        }

        public void setFormFields(List<Field> formFields) {
            this.formFields = formFields;
        }

        public List<SectionLine> getSectionLines() {
            return sectionLines;
        }

        public void setSectionLines(List<SectionLine> sectionLines) {
            this.sectionLines = sectionLines;
        }

        public List<String> getListOrder() {
            return listOrder;
        }

        public void setListOrder(List<String> listOrder) {
            this.listOrder = listOrder;
        }
    }

    public static class Field {
        private String key;
        private String name;
        private String displayName;
        private Boolean showInDetail;
        private Boolean showInForm;
        private Boolean showInList;
        private String widget;
        private ScriptValue content;
        private ScriptValue widgetParam;
        private ScriptValue widgetContent;
        private ScriptValue tip;
        private ScriptValue beforeSave;
        private String style;
        private Boolean wholeLine;
        private PermissionSet permissions = new PermissionSet();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Boolean getShowInDetail() {
            return showInDetail;
        }

        public void setShowInDetail(Boolean showInDetail) {
            this.showInDetail = showInDetail;
        }

        public Boolean getShowInForm() {
            return showInForm;
        }

        public void setShowInForm(Boolean showInForm) {
            this.showInForm = showInForm;
        }

        public Boolean getShowInList() {
            return showInList;
        }

        public void setShowInList(Boolean showInList) {
            this.showInList = showInList;
        }

        public String getWidget() {
            return widget;
        }

        public void setWidget(String widget) {
            this.widget = widget;
        }

        public ScriptValue getContent() {
            return content;
        }

        public void setContent(ScriptValue content) {
            this.content = content;
        }

        public ScriptValue getWidgetParam() {
            return widgetParam;
        }

        public void setWidgetParam(ScriptValue widgetParam) {
            this.widgetParam = widgetParam;
        }

        public ScriptValue getWidgetContent() {
            return widgetContent;
        }

        public void setWidgetContent(ScriptValue widgetContent) {
            this.widgetContent = widgetContent;
        }

        public ScriptValue getTip() {
            return tip;
        }

        public void setTip(ScriptValue tip) {
            this.tip = tip;
        }

        public ScriptValue getBeforeSave() {
            return beforeSave;
        }

        public void setBeforeSave(ScriptValue beforeSave) {
            this.beforeSave = beforeSave;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public Boolean getWholeLine() {
            return wholeLine;
        }

        public void setWholeLine(Boolean wholeLine) {
            this.wholeLine = wholeLine;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class SectionLine {
        private String key;
        private String displayName;
        private String style;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    public static class Queries {
        private List<NormalQuery> normal = new ArrayList<NormalQuery>();
        private List<AdvancedQuery> advanced = new ArrayList<AdvancedQuery>();

        public List<NormalQuery> getNormal() {
            return normal;
        }

        public void setNormal(List<NormalQuery> normal) {
            this.normal = normal;
        }

        public List<AdvancedQuery> getAdvanced() {
            return advanced;
        }

        public void setAdvanced(List<AdvancedQuery> advanced) {
            this.advanced = advanced;
        }
    }

    public static class NormalQuery {
        private String key;
        private String field;
        private String displayName;
        private String widget;
        private ScriptValue widgetParam;
        private String defaultValue;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getWidget() {
            return widget;
        }

        public void setWidget(String widget) {
            this.widget = widget;
        }

        public ScriptValue getWidgetParam() {
            return widgetParam;
        }

        public void setWidgetParam(ScriptValue widgetParam) {
            this.widgetParam = widgetParam;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    public static class AdvancedQuery {
        private String key;
        private String name;
        private String displayName;
        private String widget;
        private ScriptValue widgetParam;
        private String defaultValue;
        private ScriptValue sql;
        private String description;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getWidget() {
            return widget;
        }

        public void setWidget(String widget) {
            this.widget = widget;
        }

        public ScriptValue getWidgetParam() {
            return widgetParam;
        }

        public void setWidgetParam(ScriptValue widgetParam) {
            this.widgetParam = widgetParam;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public ScriptValue getSql() {
            return sql;
        }

        public void setSql(ScriptValue sql) {
            this.sql = sql;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Limit {
        private String key;
        private String description;
        private ScriptValue sql;
        private PermissionSet permissions = new PermissionSet();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ScriptValue getSql() {
            return sql;
        }

        public void setSql(ScriptValue sql) {
            this.sql = sql;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class Variables {
        private List<PreparedVariable> prepared = new ArrayList<PreparedVariable>();
        private List<ParentVariable> parents = new ArrayList<ParentVariable>();

        public List<PreparedVariable> getPrepared() {
            return prepared;
        }

        public void setPrepared(List<PreparedVariable> prepared) {
            this.prepared = prepared;
        }

        public List<ParentVariable> getParents() {
            return parents;
        }

        public void setParents(List<ParentVariable> parents) {
            this.parents = parents;
        }
    }

    public static class PreparedVariable {
        private String key;
        private String var;
        private String description;
        private ScriptValue exec;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getVar() {
            return var;
        }

        public void setVar(String var) {
            this.var = var;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ScriptValue getExec() {
            return exec;
        }

        public void setExec(ScriptValue exec) {
            this.exec = exec;
        }
    }

    public static class ParentVariable {
        private String key;
        private String tableName;
        private String var;
        private String description;
        private List<Foreign> foreigns = new ArrayList<Foreign>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getVar() {
            return var;
        }

        public void setVar(String var) {
            this.var = var;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Foreign> getForeigns() {
            return foreigns;
        }

        public void setForeigns(List<Foreign> foreigns) {
            this.foreigns = foreigns;
        }
    }

    public static class Foreign {
        private String mainColumn;
        private String parentColumn;
        private String description;

        public String getMainColumn() {
            return mainColumn;
        }

        public void setMainColumn(String mainColumn) {
            this.mainColumn = mainColumn;
        }

        public String getParentColumn() {
            return parentColumn;
        }

        public void setParentColumn(String parentColumn) {
            this.parentColumn = parentColumn;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Processors {
        private List<Processor> before = new ArrayList<Processor>();
        private List<Processor> after = new ArrayList<Processor>();

        public List<Processor> getBefore() {
            return before;
        }

        public void setBefore(List<Processor> before) {
            this.before = before;
        }

        public List<Processor> getAfter() {
            return after;
        }

        public void setAfter(List<Processor> after) {
            this.after = after;
        }
    }

    public static class Processor {
        private String key;
        private String description;
        private ScriptValue exec;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ScriptValue getExec() {
            return exec;
        }

        public void setExec(ScriptValue exec) {
            this.exec = exec;
        }
    }

    public static class Subviews {
        private List<SystemTab> systemTabs = new ArrayList<SystemTab>();
        private List<ViewTab> viewTabs = new ArrayList<ViewTab>();

        public List<SystemTab> getSystemTabs() {
            return systemTabs;
        }

        public void setSystemTabs(List<SystemTab> systemTabs) {
            this.systemTabs = systemTabs;
        }

        public List<ViewTab> getViewTabs() {
            return viewTabs;
        }

        public void setViewTabs(List<ViewTab> viewTabs) {
            this.viewTabs = viewTabs;
        }
    }

    public static class SystemTab {
        private String name;
        private String displayName;
        private String style;
        private PermissionSet permissions = new PermissionSet();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class ViewTab {
        private String key;
        private String displayName;
        private String style;
        private String action;
        private ScriptValue param;
        private PermissionSet permissions = new PermissionSet();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public ScriptValue getParam() {
            return param;
        }

        public void setParam(ScriptValue param) {
            this.param = param;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class Buttons {
        private List<SystemButton> system = new ArrayList<SystemButton>();
        private List<CustomButton> item = new ArrayList<CustomButton>();
        private List<CustomButton> summary = new ArrayList<CustomButton>();

        public List<SystemButton> getSystem() {
            return system;
        }

        public void setSystem(List<SystemButton> system) {
            this.system = system;
        }

        public List<CustomButton> getItem() {
            return item;
        }

        public void setItem(List<CustomButton> item) {
            this.item = item;
        }

        public List<CustomButton> getSummary() {
            return summary;
        }

        public void setSummary(List<CustomButton> summary) {
            this.summary = summary;
        }
    }

    public static class SystemButton {
        private String name;
        private Integer type;
        private String displayName;
        private String icon;
        private String styleClass;
        private String description;
        private PermissionSet permissions = new PermissionSet();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public void setStyleClass(String styleClass) {
            this.styleClass = styleClass;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class CustomButton {
        private String key;
        private String displayName;
        private String icon;
        private String action;
        private Integer openType;
        private String description;
        private ScriptValue param;
        private String confirmMessage;
        private PermissionSet permissions = new PermissionSet();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Integer getOpenType() {
            return openType;
        }

        public void setOpenType(Integer openType) {
            this.openType = openType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ScriptValue getParam() {
            return param;
        }

        public void setParam(ScriptValue param) {
            this.param = param;
        }

        public String getConfirmMessage() {
            return confirmMessage;
        }

        public void setConfirmMessage(String confirmMessage) {
            this.confirmMessage = confirmMessage;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class Weixin {
        private Integer listMode;
        private Integer urlMode;
        private ScriptValue title;
        private ScriptValue image;
        private ScriptValue description;
        private ScriptValue date;
        private PermissionSet permissions = new PermissionSet();

        public Integer getListMode() {
            return listMode;
        }

        public void setListMode(Integer listMode) {
            this.listMode = listMode;
        }

        public Integer getUrlMode() {
            return urlMode;
        }

        public void setUrlMode(Integer urlMode) {
            this.urlMode = urlMode;
        }

        public ScriptValue getTitle() {
            return title;
        }

        public void setTitle(ScriptValue title) {
            this.title = title;
        }

        public ScriptValue getImage() {
            return image;
        }

        public void setImage(ScriptValue image) {
            this.image = image;
        }

        public ScriptValue getDescription() {
            return description;
        }

        public void setDescription(ScriptValue description) {
            this.description = description;
        }

        public ScriptValue getDate() {
            return date;
        }

        public void setDate(ScriptValue date) {
            this.date = date;
        }

        public PermissionSet getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionSet permissions) {
            this.permissions = permissions;
        }
    }

    public static class Scripts {
        private ScriptValue list;
        private ScriptValue form;

        public ScriptValue getList() {
            return list;
        }

        public void setList(ScriptValue list) {
            this.list = list;
        }

        public ScriptValue getForm() {
            return form;
        }

        public void setForm(ScriptValue form) {
            this.form = form;
        }
    }
}
