package com.riversoft.api.modules.dynamic_table_views;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DynamicTableViewScriptRiskScanner {
    private static final String HIGH = "HIGH";
    private static final String MEDIUM = "MEDIUM";

    public List<DynamicTableViewResponse.Warning> scan(String path, Integer type, String script) {
        List<DynamicTableViewResponse.Warning> warnings = new ArrayList<DynamicTableViewResponse.Warning>();
        if (isBlank(script)) {
            return warnings;
        }
        String safePath = path == null ? "" : path;
        String lowerPath = safePath.toLowerCase(Locale.ENGLISH);
        String lowerScript = script.toLowerCase(Locale.ENGLISH);

        if (lowerPath.indexOf(".sql") >= 0 || containsAny(lowerScript, "insert", "update", "delete", "drop", "truncate", "alter")) {
            warnings.add(warning(HIGH, safePath, "SCRIPT_RISK_SQL", "脚本包含 SQL 结构或数据变更风险。"));
        }
        if (lowerPath.indexOf("scripts.list") >= 0 || lowerPath.indexOf("scripts.form") >= 0) {
            warnings.add(warning(MEDIUM, safePath, "SCRIPT_RISK_FRONTEND", "脚本会影响前端页面行为。"));
        }
        if (containsAny(lowerScript, "ormservice", "executeupdate", "remove", "savepo", "updatepo", "mergepo")) {
            warnings.add(warning(HIGH, safePath, "SCRIPT_RISK_MUTATION", "脚本可能修改业务数据或持久化对象。"));
        }
        if (containsAny(lowerScript, "class.forname", "setaccessible")) {
            warnings.add(warning(HIGH, safePath, "SCRIPT_RISK_REFLECTION", "脚本包含反射访问风险。"));
        }
        if (containsAny(lowerScript, "runtime.getruntime", "processbuilder")) {
            warnings.add(warning(HIGH, safePath, "SCRIPT_RISK_PROCESS", "脚本包含进程执行风险。"));
        }
        return warnings;
    }

    public List<DynamicTableViewResponse.Warning> scanSnapshot(DynamicTableViewSnapshot snapshot) {
        DynamicTableViewValidationResult result = new DynamicTableViewValidationResult();
        new DynamicTableViewValidator(null, this).scanScripts(snapshot, result);
        return result.getWarnings();
    }

    private DynamicTableViewResponse.Warning warning(String level, String path, String code, String message) {
        return new DynamicTableViewResponse.Warning(level, path, code, message);
    }

    private boolean containsAny(String value, String first, String second, String third, String fourth, String fifth, String sixth) {
        return value.indexOf(first) >= 0
                || value.indexOf(second) >= 0
                || value.indexOf(third) >= 0
                || value.indexOf(fourth) >= 0
                || value.indexOf(fifth) >= 0
                || value.indexOf(sixth) >= 0;
    }

    private boolean containsAny(String value, String first, String second) {
        return value.indexOf(first) >= 0 || value.indexOf(second) >= 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
