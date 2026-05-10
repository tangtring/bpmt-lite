package com.riversoft.api.modules.dynamic_table_views;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicTableViewResponse {

    private DynamicTableViewResponse() {
    }

    public static Map<String, Object> validation(boolean valid,
                                                 List<Warning> warnings,
                                                 List<ValidationError> errors,
                                                 DynamicTableViewSnapshot normalizedSnapshot) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("valid", Boolean.valueOf(valid));
        response.put("warnings", warnings);
        response.put("errors", errors);
        response.put("normalizedSnapshot", normalizedSnapshot);
        return response;
    }

    public static Map<String, Object> write(DynamicTableViewSnapshot snapshot,
                                            List<Warning> warnings,
                                            WritePlan plan) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("snapshot", snapshot);
        response.put("warnings", warnings);
        response.put("plan", plan);
        return response;
    }

    public static class Warning {
        private String level;
        private String path;
        private String code;
        private String message;

        public Warning() {
        }

        public Warning(String level, String path, String code, String message) {
            this.level = level;
            this.path = path;
            this.code = code;
            this.message = message;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ValidationError {
        private String path;
        private String code;
        private String message;

        public ValidationError() {
        }

        public ValidationError(String path, String code, String message) {
            this.path = path;
            this.code = code;
            this.message = message;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class WritePlan {
        private boolean dryRun;
        private List<String> creates = new ArrayList<String>();
        private List<String> updates = new ArrayList<String>();
        private List<String> deletes = new ArrayList<String>();
        private List<String> permissionCreates = new ArrayList<String>();
        private List<String> permissionKeeps = new ArrayList<String>();
        private List<String> permissionDeletes = new ArrayList<String>();

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public List<String> getCreates() {
            return creates;
        }

        public void setCreates(List<String> creates) {
            this.creates = creates;
        }

        public List<String> getUpdates() {
            return updates;
        }

        public void setUpdates(List<String> updates) {
            this.updates = updates;
        }

        public List<String> getDeletes() {
            return deletes;
        }

        public void setDeletes(List<String> deletes) {
            this.deletes = deletes;
        }

        public List<String> getPermissionCreates() {
            return permissionCreates;
        }

        public void setPermissionCreates(List<String> permissionCreates) {
            this.permissionCreates = permissionCreates;
        }

        public List<String> getPermissionKeeps() {
            return permissionKeeps;
        }

        public void setPermissionKeeps(List<String> permissionKeeps) {
            this.permissionKeeps = permissionKeeps;
        }

        public List<String> getPermissionDeletes() {
            return permissionDeletes;
        }

        public void setPermissionDeletes(List<String> permissionDeletes) {
            this.permissionDeletes = permissionDeletes;
        }
    }
}
