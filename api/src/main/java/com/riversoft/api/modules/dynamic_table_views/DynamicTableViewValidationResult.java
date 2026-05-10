package com.riversoft.api.modules.dynamic_table_views;

import java.util.ArrayList;
import java.util.List;

public class DynamicTableViewValidationResult {
    private boolean valid = true;
    private List<DynamicTableViewResponse.Warning> warnings = new ArrayList<DynamicTableViewResponse.Warning>();
    private List<DynamicTableViewResponse.ValidationError> errors = new ArrayList<DynamicTableViewResponse.ValidationError>();
    private DynamicTableViewSnapshot normalizedSnapshot;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<DynamicTableViewResponse.Warning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<DynamicTableViewResponse.Warning> warnings) {
        this.warnings = warnings;
    }

    public List<DynamicTableViewResponse.ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<DynamicTableViewResponse.ValidationError> errors) {
        this.errors = errors;
    }

    public DynamicTableViewSnapshot getNormalizedSnapshot() {
        return normalizedSnapshot;
    }

    public void setNormalizedSnapshot(DynamicTableViewSnapshot normalizedSnapshot) {
        this.normalizedSnapshot = normalizedSnapshot;
    }

    public void addError(String path, String code, String message) {
        valid = false;
        errors.add(new DynamicTableViewResponse.ValidationError(path, code, message));
    }
}
