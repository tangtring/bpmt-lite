package com.riversoft.api.modules.dynamic_table_views;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DynamicTableViewValidator {
    private static final String INVALID = "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT";
    private static final String TABLE_NOT_FOUND = "DYNAMIC_TABLE_VIEW_TABLE_NOT_FOUND";
    private static final String FIELD_NOT_FOUND = "DYNAMIC_TABLE_VIEW_FIELD_NOT_FOUND";

    private final DynamicTableViewRepository repository;
    private final DynamicTableViewDefaults defaults;
    private final DynamicTableViewScriptRiskScanner scanner;

    public DynamicTableViewValidator(DynamicTableViewRepository repository) {
        this(repository, new DynamicTableViewScriptRiskScanner());
    }

    DynamicTableViewValidator(DynamicTableViewRepository repository, DynamicTableViewScriptRiskScanner scanner) {
        this.repository = repository;
        this.defaults = new DynamicTableViewDefaults();
        this.scanner = scanner;
    }

    public DynamicTableViewValidationResult validate(DynamicTableViewSnapshot snapshot) {
        DynamicTableViewValidationResult result = new DynamicTableViewValidationResult();
        DynamicTableViewSnapshot normalized = defaults.normalize(snapshot);
        result.setNormalizedSnapshot(normalized);

        DynamicTableViewSnapshot.Base base = normalized.getBase();
        String tableName = trimToNull(base.getTableName());
        validateBase(base, tableName, result);
        validateDefaultSortField(base, tableName, result);
        Map<String, Object> tableDefinition = validateTable(tableName, "base.tableName", result);
        validateFields(normalized, tableName, result);
        validateQueries(normalized, result);
        validateParents(normalized, tableName, result);
        validateLogTable(base, tableDefinition, result);
        validateButtons(normalized.getButtons(), result);
        validateTabs(normalized.getSubviews(), result);
        scanScripts(normalized, result);
        return result;
    }

    void scanScripts(DynamicTableViewSnapshot snapshot, DynamicTableViewValidationResult result) {
        if (snapshot == null) {
            return;
        }
        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        if (fields != null) {
            scanFields(fields.getSystemFields(), "fields.systemFields", result);
            scanFields(fields.getComputedFields(), "fields.computedFields", result);
            scanFields(fields.getFormFields(), "fields.formFields", result);
        }
        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        if (queries != null) {
            scanNormalQueries(queries.getNormal(), result);
            scanAdvancedQueries(queries.getAdvanced(), result);
        }
        scanLimits(snapshot.getLimits(), result);
        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        if (variables != null) {
            scanPreparedVariables(variables.getPrepared(), result);
        }
        DynamicTableViewSnapshot.Processors processors = snapshot.getProcessors();
        if (processors != null) {
            scanProcessors(processors.getBefore(), "processors.before", result);
            scanProcessors(processors.getAfter(), "processors.after", result);
        }
        DynamicTableViewSnapshot.Subviews subviews = snapshot.getSubviews();
        if (subviews != null) {
            scanViewTabs(subviews.getViewTabs(), result);
        }
        DynamicTableViewSnapshot.Buttons buttons = snapshot.getButtons();
        if (buttons != null) {
            scanCustomButtons(buttons.getItem(), "buttons.item", result);
            scanCustomButtons(buttons.getSummary(), "buttons.summary", result);
        }
        scanWeixin(snapshot.getWeixin(), result);
        DynamicTableViewSnapshot.Scripts scripts = snapshot.getScripts();
        if (scripts != null) {
            validateScriptValue("scripts.list", scripts.getList(), result);
            validateScriptValue("scripts.form", scripts.getForm(), result);
        }
    }

    private void validateBase(DynamicTableViewSnapshot.Base base, String tableName, DynamicTableViewValidationResult result) {
        if (tableName == null) {
            result.addError("base.tableName", INVALID, "base.tableName 不能为空。");
        }
        if (isBlank(base.getDisplayName())) {
            result.addError("base.displayName", INVALID, "base.displayName 不能为空。");
        }
        DynamicTableViewSnapshot.Sort sort = base.getDefaultSort();
        if (sort == null || isBlank(sort.getField())) {
            result.addError("base.defaultSort.field", INVALID, "base.defaultSort.field 不能为空。");
        }
        if (sort == null || isBlank(sort.getDirection())) {
            result.addError("base.defaultSort.direction", INVALID, "base.defaultSort.direction 不能为空。");
        } else {
            String direction = sort.getDirection().trim().toLowerCase(Locale.ENGLISH);
            if (!"asc".equals(direction) && !"desc".equals(direction)) {
                result.addError("base.defaultSort.direction", INVALID, "base.defaultSort.direction 只允许 asc 或 desc。");
            }
        }
        if (base.getLayoutColumns() == null
                || base.getLayoutColumns().intValue() < 1
                || base.getLayoutColumns().intValue() > 5) {
            result.addError("base.layoutColumns", INVALID, "base.layoutColumns 必须在 1 到 5 之间。");
        }
    }

    private void validateDefaultSortField(DynamicTableViewSnapshot.Base base,
                                          String tableName,
                                          DynamicTableViewValidationResult result) {
        DynamicTableViewSnapshot.Sort sort = base.getDefaultSort();
        String fieldName = sort == null ? null : trimToNull(sort.getField());
        if (tableName == null || fieldName == null || repository == null) {
            return;
        }
        if (repository.findColumnDefinition(tableName, fieldName) == null) {
            result.addError("base.defaultSort.field", FIELD_NOT_FOUND, "默认排序字段不存在。");
        }
    }

    private Map<String, Object> validateTable(String tableName, String path, DynamicTableViewValidationResult result) {
        if (tableName == null || repository == null) {
            return null;
        }
        Map<String, Object> definition = repository.findTableDefinition(tableName);
        if (definition == null) {
            result.addError(path, TABLE_NOT_FOUND, "动态表不存在。");
        }
        return definition;
    }

    private void validateFields(DynamicTableViewSnapshot snapshot, String tableName, DynamicTableViewValidationResult result) {
        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        boolean visible = false;
        Set<String> fieldReferences = new HashSet<String>();
        addComputedKeys(fields.getComputedFields(), fieldReferences);
        List<DynamicTableViewSnapshot.Field> systemFields = fields.getSystemFields();
        for (int i = 0; i < systemFields.size(); i++) {
            DynamicTableViewSnapshot.Field field = systemFields.get(i);
            if (field == null) {
                continue;
            }
            if (Boolean.TRUE.equals(field.getShowInDetail())
                    || Boolean.TRUE.equals(field.getShowInForm())
                    || Boolean.TRUE.equals(field.getShowInList())) {
                visible = true;
            }
            String fieldName = trimToNull(field.getName());
            if (fieldName == null) {
                result.addError("fields.systemFields[" + i + "].name", FIELD_NOT_FOUND, "字段不存在。");
                continue;
            }
            fieldReferences.add(fieldName);
            if (tableName != null && repository != null && repository.findColumnDefinition(tableName, fieldName) == null) {
                result.addError("fields.systemFields[" + i + "].name", FIELD_NOT_FOUND, "字段不存在。");
            }
        }
        if (!visible) {
            result.addError("fields.systemFields", INVALID, "fields.systemFields 至少需要一个可展示字段。");
        }
        List<String> listOrder = fields.getListOrder();
        for (int i = 0; i < listOrder.size(); i++) {
            String item = trimToNull(listOrder.get(i));
            if (item != null && !fieldReferences.contains(item)) {
                result.addError("fields.listOrder[" + i + "]", FIELD_NOT_FOUND, "listOrder 引用了不存在的字段。");
            }
        }
    }

    private void addComputedKeys(List<DynamicTableViewSnapshot.Field> computedFields, Set<String> fieldReferences) {
        if (computedFields == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Field field : computedFields) {
            if (field != null && !isBlank(field.getKey())) {
                fieldReferences.add(field.getKey());
            }
        }
    }

    private void validateQueries(DynamicTableViewSnapshot snapshot, DynamicTableViewValidationResult result) {
        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        Set<String> references = fieldReferences(snapshot.getFields());
        List<DynamicTableViewSnapshot.NormalQuery> normal = queries.getNormal();
        for (int i = 0; i < normal.size(); i++) {
            DynamicTableViewSnapshot.NormalQuery query = normal.get(i);
            String field = query == null ? null : trimToNull(query.getField());
            if (field == null || !references.contains(field)) {
                result.addError("queries.normal[" + i + "].field", FIELD_NOT_FOUND, "查询字段不存在。");
            }
        }
    }

    private Set<String> fieldReferences(DynamicTableViewSnapshot.Fields fields) {
        Set<String> references = new HashSet<String>();
        if (fields == null) {
            return references;
        }
        for (DynamicTableViewSnapshot.Field field : fields.getSystemFields()) {
            if (field != null && !isBlank(field.getName())) {
                references.add(field.getName());
            }
        }
        addComputedKeys(fields.getComputedFields(), references);
        return references;
    }

    private void validateParents(DynamicTableViewSnapshot snapshot, String mainTableName, DynamicTableViewValidationResult result) {
        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        List<DynamicTableViewSnapshot.ParentVariable> parents = variables.getParents();
        for (int i = 0; i < parents.size(); i++) {
            DynamicTableViewSnapshot.ParentVariable parent = parents.get(i);
            if (parent == null) {
                continue;
            }
            String parentTableName = trimToNull(parent.getTableName());
            if (parentTableName == null) {
                result.addError("variables.parents[" + i + "].tableName", TABLE_NOT_FOUND, "动态表不存在。");
            }
            validateTable(parentTableName, "variables.parents[" + i + "].tableName", result);
            if ("vo".equals(trimToNull(parent.getVar()))) {
                result.addError("variables.parents[" + i + "].var", INVALID, "parent var 不能为 vo。");
            }
            List<DynamicTableViewSnapshot.Foreign> foreigns = parent.getForeigns();
            if (foreigns == null || foreigns.isEmpty()) {
                result.addError("variables.parents[" + i + "].foreigns", INVALID, "parent 至少需要一个 foreign。");
                continue;
            }
            for (int j = 0; j < foreigns.size(); j++) {
                DynamicTableViewSnapshot.Foreign foreign = foreigns.get(j);
                if (foreign == null) {
                    continue;
                }
                validateColumn(mainTableName, foreign.getMainColumn(),
                        "variables.parents[" + i + "].foreigns[" + j + "].mainColumn", result);
                validateColumn(parentTableName, foreign.getParentColumn(),
                        "variables.parents[" + i + "].foreigns[" + j + "].parentColumn", result);
            }
        }
    }

    private void validateColumn(String tableName, String columnName, String path, DynamicTableViewValidationResult result) {
        String safeTableName = trimToNull(tableName);
        String safeColumnName = trimToNull(columnName);
        if (safeTableName == null || safeColumnName == null || repository == null) {
            if (safeColumnName == null) {
                result.addError(path, FIELD_NOT_FOUND, "字段不存在。");
            }
            return;
        }
        if (repository.findColumnDefinition(safeTableName, safeColumnName) == null) {
            result.addError(path, FIELD_NOT_FOUND, "字段不存在。");
        }
    }

    private void validateLogTable(DynamicTableViewSnapshot.Base base,
                                  Map<String, Object> tableDefinition,
                                  DynamicTableViewValidationResult result) {
        String logTableName = trimToNull(base.getLogTableName());
        if (logTableName == null) {
            return;
        }
        Map<String, Object> logDefinition = validateTable(logTableName, "base.logTableName", result);
        if (tableDefinition == null || logDefinition == null) {
            return;
        }
        Object mainPk = tableDefinition.get("primaryKeyName");
        Object logPk = logDefinition.get("primaryKeyName");
        Object mainType = firstNonNull(tableDefinition.get("primaryKeyType"), tableDefinition.get("type"));
        Object logType = firstNonNull(logDefinition.get("primaryKeyType"), logDefinition.get("type"));
        if (mainPk != null && logPk != null && !mainPk.equals(logPk)) {
            result.addError("base.logTableName", INVALID, "日志表主键字段与主表不一致。");
        }
        if (mainType != null && logType != null && !mainType.equals(logType)) {
            result.addError("base.logTableName", INVALID, "日志表主键类型与主表不一致。");
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private void validateButtons(DynamicTableViewSnapshot.Buttons buttons, DynamicTableViewValidationResult result) {
        Set<String> allowed = setOf("CREATE", "EDIT", "DELETE", "VIEW", "IMPORT", "EXPORT", "BACK", "COPY");
        List<DynamicTableViewSnapshot.SystemButton> system = buttons.getSystem();
        for (int i = 0; i < system.size(); i++) {
            DynamicTableViewSnapshot.SystemButton button = system.get(i);
            String name = button == null ? null : trimToNull(button.getName());
            if (name == null || !allowed.contains(name)) {
                result.addError("buttons.system[" + i + "].name", INVALID, "系统按钮名称不在允许范围内。");
            }
        }
    }

    private void validateTabs(DynamicTableViewSnapshot.Subviews subviews, DynamicTableViewValidationResult result) {
        Set<String> allowed = setOf("detail", "log", "flow", "file", "comment", "history");
        List<DynamicTableViewSnapshot.SystemTab> systemTabs = subviews.getSystemTabs();
        for (int i = 0; i < systemTabs.size(); i++) {
            DynamicTableViewSnapshot.SystemTab tab = systemTabs.get(i);
            String name = tab == null ? null : trimToNull(tab.getName());
            if (name == null || !allowed.contains(name)) {
                result.addError("subviews.systemTabs[" + i + "].name", INVALID, "系统页签名称不在允许范围内。");
            }
        }
    }

    private Set<String> setOf(String a, String b, String c, String d, String e, String f, String g, String h) {
        Set<String> set = new HashSet<String>();
        set.add(a);
        set.add(b);
        set.add(c);
        set.add(d);
        set.add(e);
        set.add(f);
        set.add(g);
        set.add(h);
        return set;
    }

    private Set<String> setOf(String a, String b, String c, String d, String e, String f) {
        Set<String> set = new HashSet<String>();
        set.add(a);
        set.add(b);
        set.add(c);
        set.add(d);
        set.add(e);
        set.add(f);
        return set;
    }

    private void scanFields(List<DynamicTableViewSnapshot.Field> fields, String path, DynamicTableViewValidationResult result) {
        if (fields == null) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.get(i);
            if (field == null) {
                continue;
            }
            validateScriptValue(path + "[" + i + "].content", field.getContent(), result);
            validateScriptValue(path + "[" + i + "].widgetParam", field.getWidgetParam(), result);
            validateScriptValue(path + "[" + i + "].widgetContent", field.getWidgetContent(), result);
            validateScriptValue(path + "[" + i + "].tip", field.getTip(), result);
            validateScriptValue(path + "[" + i + "].beforeSave", field.getBeforeSave(), result);
        }
    }

    private void scanNormalQueries(List<DynamicTableViewSnapshot.NormalQuery> queries, DynamicTableViewValidationResult result) {
        if (queries == null) {
            return;
        }
        for (int i = 0; i < queries.size(); i++) {
            DynamicTableViewSnapshot.NormalQuery query = queries.get(i);
            if (query != null) {
                validateScriptValue("queries.normal[" + i + "].widgetParam", query.getWidgetParam(), result);
            }
        }
    }

    private void scanAdvancedQueries(List<DynamicTableViewSnapshot.AdvancedQuery> queries, DynamicTableViewValidationResult result) {
        if (queries == null) {
            return;
        }
        for (int i = 0; i < queries.size(); i++) {
            DynamicTableViewSnapshot.AdvancedQuery query = queries.get(i);
            if (query != null) {
                validateScriptValue("queries.advanced[" + i + "].widgetParam", query.getWidgetParam(), result);
                validateScriptValue("queries.advanced[" + i + "].sql", query.getSql(), result);
            }
        }
    }

    private void scanLimits(List<DynamicTableViewSnapshot.Limit> limits, DynamicTableViewValidationResult result) {
        if (limits == null) {
            return;
        }
        for (int i = 0; i < limits.size(); i++) {
            DynamicTableViewSnapshot.Limit limit = limits.get(i);
            if (limit != null) {
                validateScriptValue("limits[" + i + "].sql", limit.getSql(), result);
            }
        }
    }

    private void scanPreparedVariables(List<DynamicTableViewSnapshot.PreparedVariable> variables, DynamicTableViewValidationResult result) {
        if (variables == null) {
            return;
        }
        for (int i = 0; i < variables.size(); i++) {
            DynamicTableViewSnapshot.PreparedVariable variable = variables.get(i);
            if (variable != null) {
                validateScriptValue("variables.prepared[" + i + "].exec", variable.getExec(), result);
            }
        }
    }

    private void scanProcessors(List<DynamicTableViewSnapshot.Processor> processors, String path, DynamicTableViewValidationResult result) {
        if (processors == null) {
            return;
        }
        for (int i = 0; i < processors.size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.get(i);
            if (processor != null) {
                validateScriptValue(path + "[" + i + "].exec", processor.getExec(), result);
            }
        }
    }

    private void scanViewTabs(List<DynamicTableViewSnapshot.ViewTab> tabs, DynamicTableViewValidationResult result) {
        if (tabs == null) {
            return;
        }
        for (int i = 0; i < tabs.size(); i++) {
            DynamicTableViewSnapshot.ViewTab tab = tabs.get(i);
            if (tab != null) {
                validateScriptValue("subviews.viewTabs[" + i + "].param", tab.getParam(), result);
            }
        }
    }

    private void scanCustomButtons(List<DynamicTableViewSnapshot.CustomButton> buttons, String path, DynamicTableViewValidationResult result) {
        if (buttons == null) {
            return;
        }
        for (int i = 0; i < buttons.size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.get(i);
            if (button != null) {
                validateScriptValue(path + "[" + i + "].param", button.getParam(), result);
            }
        }
    }

    private void scanWeixin(DynamicTableViewSnapshot.Weixin weixin, DynamicTableViewValidationResult result) {
        if (weixin == null) {
            return;
        }
        validateScriptValue("weixin.title", weixin.getTitle(), result);
        validateScriptValue("weixin.image", weixin.getImage(), result);
        validateScriptValue("weixin.description", weixin.getDescription(), result);
        validateScriptValue("weixin.date", weixin.getDate(), result);
    }

    private void validateScriptValue(String path, DynamicTableViewSnapshot.ScriptValue value, DynamicTableViewValidationResult result) {
        if (value == null) {
            return;
        }
        if (value.getType() == null) {
            result.addError(path + ".type", INVALID, "脚本 type 不能为空。");
        } else if (value.getType().intValue() < 1 || value.getType().intValue() > 3) {
            result.addError(path + ".type", INVALID, "脚本 type 必须在 1 到 3 之间。");
        }
        if (isBlank(value.getScript())) {
            result.addError(path + ".script", INVALID, "脚本内容不能为空。");
            return;
        }
        result.getWarnings().addAll(scanner.scan(path + ".script", value.getType(), value.getScript()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }
}
