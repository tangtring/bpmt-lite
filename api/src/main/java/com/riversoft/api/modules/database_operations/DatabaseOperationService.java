package com.riversoft.api.modules.database_operations;

import com.riversoft.api.http.ApiException;
import com.riversoft.platform.db.DbHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseOperationService {

    private final DbHelper dbHelper;

    public DatabaseOperationService() {
        this(new DbHelper());
    }

    public DatabaseOperationService(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Map<String, Object> query(DatabaseOperationRequest request) {
        String sql = normalizeSql(request.getSql());
        ensureVerb(sql, "SELECT");
        Object[] args = request.getArgs() == null ? new Object[0] : request.getArgs();
        List<?> items = dbHelper.query(sql, args);
        int maxRows = readMaxRows();
        if (items != null && maxRows > 0 && items.size() > maxRows) {
            throw new ApiException(422, "DBOPS_RESULT_TOO_LARGE", "查询结果超过最大返回行数限制。");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", items);
        result.put("count", items == null ? 0 : items.size());
        return result;
    }

    public Map<String, Object> find(DatabaseOperationRequest request) {
        String sql = normalizeSql(request.getSql());
        ensureVerb(sql, "SELECT");
        Object[] args = request.getArgs() == null ? new Object[0] : request.getArgs();
        Map<String, Object> item = dbHelper.find(sql, args);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("item", item);
        return result;
    }

    public Map<String, Object> save(DatabaseOperationRequest request) {
        ensureWriteEnabled();
        String sql = normalizeSql(request.getSql());
        ensureVerb(sql, "INSERT");
        Object[] args = request.getArgs() == null ? new Object[0] : request.getArgs();
        Long id = dbHelper.save(sql, args);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", id);
        return result;
    }

    public Map<String, Object> exec(DatabaseOperationRequest request) {
        ensureWriteEnabled();
        String sql = normalizeSql(request.getSql());
        String verb = readVerb(sql);
        if (!"UPDATE".equals(verb) && !"DELETE".equals(verb)) {
            throw new ApiException(422, "DBOPS_SQL_NOT_ALLOWED", "exec 仅支持 UPDATE 或 DELETE。");
        }
        Object[] args = request.getArgs() == null ? new Object[0] : request.getArgs();
        dbHelper.exec(sql, args);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("executed", Boolean.TRUE);
        return result;
    }

    private static String normalizeSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new ApiException(400, "DBOPS_SQL_REQUIRED", "sql 不能为空。");
        }
        return StringUtils.trim(sql);
    }

    private static void ensureVerb(String sql, String expected) {
        String verb = readVerb(sql);
        if (!expected.equals(verb)) {
            throw new ApiException(422, "DBOPS_SQL_NOT_ALLOWED", expected + " 接口只允许 " + expected + " 语句。");
        }
    }

    private static String readVerb(String sql) {
        String[] tokens = StringUtils.split(sql, null, 2);
        if (tokens == null || tokens.length == 0) {
            throw new ApiException(422, "DBOPS_SQL_NOT_ALLOWED", "SQL 语句无效。");
        }
        return StringUtils.upperCase(tokens[0], Locale.ENGLISH);
    }

    private static void ensureWriteEnabled() {
        String enabled = System.getenv("BPMT_API_DBOPS_EXECUTE_ENABLED");
        if (!"true".equalsIgnoreCase(StringUtils.defaultString(enabled))) {
            throw new ApiException(403, "DBOPS_EXECUTE_DISABLED", "当前环境未开启数据库写操作能力。");
        }
    }

    private static int readMaxRows() {
        String raw = System.getenv("BPMT_API_DBOPS_MAX_ROWS");
        if (StringUtils.isBlank(raw)) {
            return 1000;
        }
        try {
            int value = Integer.parseInt(StringUtils.trim(raw));
            return Math.max(value, 1);
        } catch (NumberFormatException e) {
            return 1000;
        }
    }
}
