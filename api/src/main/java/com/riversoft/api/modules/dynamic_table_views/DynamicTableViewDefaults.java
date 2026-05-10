package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.core.IDGenerator;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DynamicTableViewDefaults {
    public DynamicTableViewSnapshot normalize(DynamicTableViewSnapshot snapshot) {
        return normalize(snapshot, null);
    }

    public DynamicTableViewSnapshot normalize(DynamicTableViewSnapshot snapshot, DynamicTableViewRepository repository) {
        DynamicTableViewSnapshot normalized = snapshot == null ? new DynamicTableViewSnapshot() : snapshot;
        if (normalized.getBase() == null) {
            normalized.setBase(new DynamicTableViewSnapshot.Base());
        }
        if (normalized.getFields() == null) {
            normalized.setFields(new DynamicTableViewSnapshot.Fields());
        }
        if (normalized.getQueries() == null) {
            normalized.setQueries(new DynamicTableViewSnapshot.Queries());
        }
        if (normalized.getLimits() == null) {
            normalized.setLimits(new ArrayList<DynamicTableViewSnapshot.Limit>());
        }
        if (normalized.getVariables() == null) {
            normalized.setVariables(new DynamicTableViewSnapshot.Variables());
        }
        if (normalized.getProcessors() == null) {
            normalized.setProcessors(new DynamicTableViewSnapshot.Processors());
        }
        if (normalized.getSubviews() == null) {
            normalized.setSubviews(new DynamicTableViewSnapshot.Subviews());
        }
        if (normalized.getButtons() == null) {
            normalized.setButtons(new DynamicTableViewSnapshot.Buttons());
        }
        if (normalized.getScripts() == null) {
            normalized.setScripts(new DynamicTableViewSnapshot.Scripts());
        }

        normalizeBase(normalized.getBase());
        normalizeFields(normalized.getFields());
        normalizeSystemFieldWidgets(normalized, repository);
        normalizeQueries(normalized.getQueries());
        normalizeLimits(normalized.getLimits());
        normalizeVariables(normalized.getVariables());
        normalizeProcessors(normalized.getProcessors());
        normalizeSubviews(normalized.getSubviews());
        normalizeButtons(normalized.getButtons());
        return normalized;
    }

    public DynamicTableViewSnapshot normalizeForCreate(DynamicTableViewSnapshot snapshot, DynamicTableViewRepository repository) {
        DynamicTableViewSnapshot normalized = normalize(snapshot, repository);
        if (isBlank(normalized.getViewKey())) {
            normalized.setViewKey(IDGenerator.next());
        }
        return normalized;
    }

    private void normalizeBase(DynamicTableViewSnapshot.Base base) {
        if (base.getLayoutColumns() == null) {
            base.setLayoutColumns(Integer.valueOf(2));
        }
        if (base.getInitQuery() == null) {
            base.setInitQuery(Boolean.TRUE);
        }
        if (base.getPageLimit() == null) {
            base.setPageLimit(Integer.valueOf(20));
        }
        if (base.getDefaultSort() == null) {
            base.setDefaultSort(new DynamicTableViewSnapshot.Sort());
        }
    }

    private void normalizeFields(DynamicTableViewSnapshot.Fields fields) {
        if (fields.getSystemFields() == null) {
            fields.setSystemFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getComputedFields() == null) {
            fields.setComputedFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getFormFields() == null) {
            fields.setFormFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getSectionLines() == null) {
            fields.setSectionLines(new ArrayList<DynamicTableViewSnapshot.SectionLine>());
        }
        if (fields.getListOrder() == null) {
            fields.setListOrder(new ArrayList<String>());
        }
        fillFieldKeys(fields.getComputedFields(), "computedField");
        fillFieldKeys(fields.getFormFields(), "formField");
        fillSectionLineKeys(fields.getSectionLines());
    }

    private void normalizeSystemFieldWidgets(DynamicTableViewSnapshot snapshot, DynamicTableViewRepository repository) {
        if (repository == null || snapshot.getFields() == null || snapshot.getBase() == null) {
            return;
        }
        String tableName = trimToNull(snapshot.getBase().getTableName());
        if (tableName == null || snapshot.getFields().getSystemFields() == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Field field : snapshot.getFields().getSystemFields()) {
            if (field == null || isBlank(field.getName())) {
                continue;
            }
            Map<String, Object> column = repository.findColumnDefinition(tableName, field.getName());
            if (column == null) {
                continue;
            }
            normalizeSystemFieldWidget(field, column);
        }
    }

    private void normalizeSystemFieldWidget(DynamicTableViewSnapshot.Field field, Map<String, Object> column) {
        boolean primaryKey = booleanValue(column.get("primaryKey"));
        boolean requiredField = primaryKey || booleanValue(column.get("required")) || booleanValue(column.get("notNull"));
        String currentWidget = trimToNull(field.getWidget());
        if (currentWidget == null) {
            String widget = defaultWidget(column);
            field.setWidget(requiredField ? appendRequired(widget) : widget);
            if ("textarea".equals(widget)) {
                field.setWholeLine(Boolean.TRUE);
            }
            return;
        }
        if (requiredField && !hasRequiredRule(currentWidget)) {
            field.setWidget(appendRequired(currentWidget));
        }
    }

    private String defaultWidget(Map<String, Object> column) {
        String type = lowerString(firstNonNull(column.get("type"), column.get("typeName"), column.get("columnType")));
        Integer typeCode = integerValue(firstNonNull(column.get("typeCode"), column.get("mappedTypeCode")));
        int length = intValue(firstNonNull(column.get("totalSize"), column.get("length")));
        if (containsAny(type, "date", "time", "timestamp") || isTypeCode(typeCode, Types.DATE, Types.TIME, Types.TIMESTAMP)) {
            return "date";
        }
        if (containsAny(type, "clob", "text")
                || isTypeCode(typeCode, Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR)
                || ((containsAny(type, "char", "string", "varchar")
                || isTypeCode(typeCode, Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR))
                && length > 1000)) {
            return "textarea";
        }
        if (containsAny(type, "blob", "binary")
                || isTypeCode(typeCode, Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY)) {
            return "file";
        }
        if (containsAny(type, "int", "integer", "long", "bigint")
                || isTypeCode(typeCode, Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BIGINT)) {
            return "text{digits:true}";
        }
        if (containsAny(type, "number", "decimal", "numeric", "double", "float", "real")
                || isTypeCode(typeCode, Types.NUMERIC, Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.REAL)) {
            return "text{number:true}";
        }
        return "text";
    }

    private String appendRequired(String widget) {
        String safeWidget = isBlank(widget) ? "text" : widget;
        int braceIndex = safeWidget.lastIndexOf('}');
        if (braceIndex >= 0) {
            String prefix = safeWidget.substring(0, braceIndex).trim();
            if (prefix.endsWith("{")) {
                return prefix + "required:true" + safeWidget.substring(braceIndex);
            }
            return prefix + ",required:true" + safeWidget.substring(braceIndex);
        }
        return safeWidget + "{required:true}";
    }

    private boolean hasRequiredRule(String widget) {
        if (widget == null) {
            return false;
        }
        String normalized = widget.toLowerCase(Locale.ENGLISH).replace(" ", "");
        if (normalized.indexOf("required:true") >= 0) {
            return true;
        }
        int open = normalized.indexOf('{');
        int close = normalized.lastIndexOf('}');
        if (open < 0 || close <= open) {
            return false;
        }
        String[] rules = normalized.substring(open + 1, close).split(",");
        for (String rule : rules) {
            if ("required".equals(rule) || "required:true".equals(rule)) {
                return true;
            }
        }
        return false;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private Object firstNonNull(Object first, Object second, Object third) {
        if (first != null) {
            return first;
        }
        return second != null ? second : third;
    }

    private String lowerString(Object value) {
        return value == null ? "" : String.valueOf(value).toLowerCase(Locale.ENGLISH);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        if (value != null) {
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value != null) {
            String text = String.valueOf(value).trim();
            return "true".equalsIgnoreCase(text) || "1".equals(text) || "Y".equalsIgnoreCase(text);
        }
        return false;
    }

    private boolean isTypeCode(Integer value, int... expected) {
        if (value == null || expected == null) {
            return false;
        }
        for (int typeCode : expected) {
            if (value.intValue() == typeCode) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, String first, String second) {
        return value.indexOf(first) >= 0 || value.indexOf(second) >= 0;
    }

    private boolean containsAny(String value, String first, String second, String third) {
        return value.indexOf(first) >= 0 || value.indexOf(second) >= 0 || value.indexOf(third) >= 0;
    }

    private boolean containsAny(String value, String first, String second, String third, String fourth) {
        return value.indexOf(first) >= 0
                || value.indexOf(second) >= 0
                || value.indexOf(third) >= 0
                || value.indexOf(fourth) >= 0;
    }

    private boolean containsAny(String value, String first, String second, String third, String fourth, String fifth, String sixth) {
        return value.indexOf(first) >= 0
                || value.indexOf(second) >= 0
                || value.indexOf(third) >= 0
                || value.indexOf(fourth) >= 0
                || value.indexOf(fifth) >= 0
                || value.indexOf(sixth) >= 0;
    }

    private void normalizeQueries(DynamicTableViewSnapshot.Queries queries) {
        if (queries.getNormal() == null) {
            queries.setNormal(new ArrayList<DynamicTableViewSnapshot.NormalQuery>());
        }
        if (queries.getAdvanced() == null) {
            queries.setAdvanced(new ArrayList<DynamicTableViewSnapshot.AdvancedQuery>());
        }
        for (int i = 0; i < queries.getNormal().size(); i++) {
            DynamicTableViewSnapshot.NormalQuery query = queries.getNormal().get(i);
            if (query != null && isBlank(query.getKey())) {
                query.setKey("normalQuery-" + (i + 1));
            }
        }
        for (int i = 0; i < queries.getAdvanced().size(); i++) {
            DynamicTableViewSnapshot.AdvancedQuery query = queries.getAdvanced().get(i);
            if (query != null && isBlank(query.getKey())) {
                query.setKey("advancedQuery-" + (i + 1));
            }
        }
    }

    private void normalizeLimits(List<DynamicTableViewSnapshot.Limit> limits) {
        for (int i = 0; i < limits.size(); i++) {
            DynamicTableViewSnapshot.Limit limit = limits.get(i);
            if (limit != null && isBlank(limit.getKey())) {
                limit.setKey("limit-" + (i + 1));
            }
        }
    }

    private void normalizeVariables(DynamicTableViewSnapshot.Variables variables) {
        if (variables.getPrepared() == null) {
            variables.setPrepared(new ArrayList<DynamicTableViewSnapshot.PreparedVariable>());
        }
        if (variables.getParents() == null) {
            variables.setParents(new ArrayList<DynamicTableViewSnapshot.ParentVariable>());
        }
        for (int i = 0; i < variables.getPrepared().size(); i++) {
            DynamicTableViewSnapshot.PreparedVariable variable = variables.getPrepared().get(i);
            if (variable != null && isBlank(variable.getKey())) {
                variable.setKey("preparedVariable-" + (i + 1));
            }
        }
        for (int i = 0; i < variables.getParents().size(); i++) {
            DynamicTableViewSnapshot.ParentVariable variable = variables.getParents().get(i);
            if (variable != null && isBlank(variable.getKey())) {
                variable.setKey("parentVariable-" + (i + 1));
            }
            if (variable != null && variable.getForeigns() == null) {
                variable.setForeigns(new ArrayList<DynamicTableViewSnapshot.Foreign>());
            }
        }
    }

    private void normalizeProcessors(DynamicTableViewSnapshot.Processors processors) {
        if (processors.getBefore() == null) {
            processors.setBefore(new ArrayList<DynamicTableViewSnapshot.Processor>());
        }
        if (processors.getAfter() == null) {
            processors.setAfter(new ArrayList<DynamicTableViewSnapshot.Processor>());
        }
        for (int i = 0; i < processors.getBefore().size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.getBefore().get(i);
            if (processor != null && isBlank(processor.getKey())) {
                processor.setKey("processor-before-" + (i + 1));
            }
        }
        for (int i = 0; i < processors.getAfter().size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.getAfter().get(i);
            if (processor != null && isBlank(processor.getKey())) {
                processor.setKey("processor-after-" + (i + 1));
            }
        }
    }

    private void normalizeSubviews(DynamicTableViewSnapshot.Subviews subviews) {
        if (subviews.getSystemTabs() == null) {
            subviews.setSystemTabs(new ArrayList<DynamicTableViewSnapshot.SystemTab>());
        }
        if (subviews.getViewTabs() == null) {
            subviews.setViewTabs(new ArrayList<DynamicTableViewSnapshot.ViewTab>());
        }
        for (int i = 0; i < subviews.getViewTabs().size(); i++) {
            DynamicTableViewSnapshot.ViewTab tab = subviews.getViewTabs().get(i);
            if (tab != null && isBlank(tab.getKey())) {
                tab.setKey("viewTab-" + (i + 1));
            }
        }
    }

    private void normalizeButtons(DynamicTableViewSnapshot.Buttons buttons) {
        if (buttons.getSystem() == null || buttons.getSystem().isEmpty()) {
            buttons.setSystem(defaultSystemButtons());
        } else {
            normalizeSystemButtons(buttons.getSystem());
        }
        if (buttons.getItem() == null) {
            buttons.setItem(new ArrayList<DynamicTableViewSnapshot.CustomButton>());
        }
        if (buttons.getSummary() == null) {
            buttons.setSummary(new ArrayList<DynamicTableViewSnapshot.CustomButton>());
        }
        for (int i = 0; i < buttons.getItem().size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.getItem().get(i);
            if (button != null && isBlank(button.getKey())) {
                button.setKey("itemButton-" + (i + 1));
            }
        }
        for (int i = 0; i < buttons.getSummary().size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.getSummary().get(i);
            if (button != null && isBlank(button.getKey())) {
                button.setKey("summaryButton-" + (i + 1));
            }
        }
    }

    private List<DynamicTableViewSnapshot.SystemButton> defaultSystemButtons() {
        List<DynamicTableViewSnapshot.SystemButton> buttons = new ArrayList<DynamicTableViewSnapshot.SystemButton>();
        buttons.add(systemButton("show", Integer.valueOf(1), "查看", "zoomin", "left"));
        buttons.add(systemButton("edit", Integer.valueOf(1), "编辑", "wrench", "left"));
        buttons.add(systemButton("del", Integer.valueOf(1), "删除", "trash", "left"));
        buttons.add(systemButton("create", Integer.valueOf(2), "新增", "plus", "left"));
        return buttons;
    }

    private void normalizeSystemButtons(List<DynamicTableViewSnapshot.SystemButton> buttons) {
        for (DynamicTableViewSnapshot.SystemButton button : buttons) {
            if (button == null || isBlank(button.getName())) {
                continue;
            }
            DynamicTableViewSnapshot.SystemButton defaults = systemButtonDefaults(button.getName());
            if (defaults == null) {
                continue;
            }
            if (button.getType() == null) {
                button.setType(defaults.getType());
            }
            if (isBlank(button.getDisplayName())) {
                button.setDisplayName(defaults.getDisplayName());
            }
            if (isBlank(button.getIcon())) {
                button.setIcon(defaults.getIcon());
            }
            if (isBlank(button.getStyleClass())) {
                button.setStyleClass(defaults.getStyleClass());
            }
        }
    }

    private DynamicTableViewSnapshot.SystemButton systemButtonDefaults(String name) {
        if ("show".equals(name)) {
            return systemButton("show", Integer.valueOf(1), "查看", "zoomin", "left");
        }
        if ("edit".equals(name)) {
            return systemButton("edit", Integer.valueOf(1), "编辑", "wrench", "left");
        }
        if ("del".equals(name)) {
            return systemButton("del", Integer.valueOf(1), "删除", "trash", "left");
        }
        if ("create".equals(name)) {
            return systemButton("create", Integer.valueOf(2), "新增", "plus", "left");
        }
        if ("upload".equals(name)) {
            return systemButton("upload", Integer.valueOf(2), "导入", "arrowthickstop-1-n", "right");
        }
        if ("download".equals(name)) {
            return systemButton("download", Integer.valueOf(2), "批量导出", "arrowthickstop-1-s", "right");
        }
        if ("delAll".equals(name)) {
            return systemButton("delAll", Integer.valueOf(2), "批量删除", "trash", "right");
        }
        return null;
    }

    private DynamicTableViewSnapshot.SystemButton systemButton(String name, Integer type, String displayName, String icon, String styleClass) {
        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName(name);
        button.setType(type);
        button.setDisplayName(displayName);
        button.setIcon(icon);
        button.setStyleClass(styleClass);
        return button;
    }

    private void fillFieldKeys(List<DynamicTableViewSnapshot.Field> fields, String prefix) {
        for (int i = 0; i < fields.size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.get(i);
            if (field != null && isBlank(field.getKey())) {
                field.setKey(prefix + "-" + (i + 1));
            }
        }
    }

    private void fillSectionLineKeys(List<DynamicTableViewSnapshot.SectionLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            DynamicTableViewSnapshot.SectionLine line = lines.get(i);
            if (line != null && isBlank(line.getKey())) {
                line.setKey("sectionLine-" + (i + 1));
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
