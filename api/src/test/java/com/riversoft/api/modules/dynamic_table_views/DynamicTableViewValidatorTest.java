package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.platform.po.VwUrl;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DynamicTableViewValidatorTest {
    @Test
    public void validateRejectsMissingBaseTableName() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getBase().setTableName(null);

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "base.tableName", "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT");
    }

    @Test
    public void validateRejectsUnknownMainField() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getFields().getSystemFields().get(0).setName("UNKNOWN_FIELD");

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "fields.systemFields[0].name", "DYNAMIC_TABLE_VIEW_FIELD_NOT_FOUND");
    }

    @Test
    public void validateRejectsUnknownDefaultSortField() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getBase().getDefaultSort().setField("UNKNOWN_SORT");

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "base.defaultSort.field", "DYNAMIC_TABLE_VIEW_FIELD_NOT_FOUND");
    }

    @Test
    public void validMinimalSnapshotPassesAndNormalizesDefaults() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getBase().setLayoutColumns(null);
        snapshot.getBase().setInitQuery(null);
        snapshot.getBase().setPageLimit(null);
        snapshot.getButtons().setSystem(null);

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertTrue(result.isValid());
        assertNotNull(result.getNormalizedSnapshot());
        assertEquals(Integer.valueOf(2), result.getNormalizedSnapshot().getBase().getLayoutColumns());
        assertEquals(Boolean.TRUE, result.getNormalizedSnapshot().getBase().getInitQuery());
        assertEquals(Integer.valueOf(20), result.getNormalizedSnapshot().getBase().getPageLimit());
        assertEquals(4, result.getNormalizedSnapshot().getButtons().getSystem().size());
    }

    @Test
    public void validateRejectsUnknownListOrderField() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getFields().getListOrder().add("NOT_PRESENT");

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "fields.listOrder[1]", "DYNAMIC_TABLE_VIEW_FIELD_NOT_FOUND");
    }

    @Test
    public void validateRejectsParentVarVo() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        DynamicTableViewSnapshot.ParentVariable parent = new DynamicTableViewSnapshot.ParentVariable();
        parent.setTableName("CRM_PARENT");
        parent.setVar("vo");
        DynamicTableViewSnapshot.Foreign foreign = new DynamicTableViewSnapshot.Foreign();
        foreign.setMainColumn("PARENT_ID");
        foreign.setParentColumn("ID");
        parent.setForeigns(Collections.singletonList(foreign));
        snapshot.getVariables().getParents().add(parent);

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "variables.parents[0].var", "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT");
    }

    @Test
    public void validateRejectsInvalidScriptType() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        DynamicTableViewSnapshot.Processor processor = new DynamicTableViewSnapshot.Processor();
        DynamicTableViewSnapshot.ScriptValue exec = new DynamicTableViewSnapshot.ScriptValue();
        exec.setType(Integer.valueOf(9));
        exec.setScript("return vo.ID;");
        processor.setExec(exec);
        snapshot.getProcessors().getBefore().add(processor);

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "processors.before[0].exec.type", "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT");
    }

    @Test
    public void validateAcceptsExistingDynSystemButtonNames() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getButtons().setSystem(java.util.Arrays.asList(
                systemButton("show"),
                systemButton("edit"),
                systemButton("del"),
                systemButton("create"),
                systemButton("upload"),
                systemButton("download"),
                systemButton("delAll")));

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertTrue(result.isValid());
    }

    @Test
    public void validateRejectsNonDynSystemButtonName() {
        DynamicTableViewSnapshot snapshot = minimalSnapshot();
        snapshot.getButtons().setSystem(Collections.singletonList(systemButton("CREATE")));

        DynamicTableViewValidationResult result = new DynamicTableViewValidator(new FakeRepository()).validate(snapshot);

        assertFalse(result.isValid());
        assertError(result, "buttons.system[0].name", "DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT");
    }

    private DynamicTableViewSnapshot minimalSnapshot() {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        snapshot.setBase(new DynamicTableViewSnapshot.Base());
        snapshot.getBase().setTableName("CRM_CUSTOMER");
        snapshot.getBase().setDisplayName("客户资料");
        snapshot.getBase().setLayoutColumns(Integer.valueOf(2));
        snapshot.getBase().setInitQuery(Boolean.TRUE);
        snapshot.getBase().setPageLimit(Integer.valueOf(20));
        snapshot.getBase().setDefaultSort(new DynamicTableViewSnapshot.Sort());
        snapshot.getBase().getDefaultSort().setField("ID");
        snapshot.getBase().getDefaultSort().setDirection("desc");

        DynamicTableViewSnapshot.Field id = new DynamicTableViewSnapshot.Field();
        id.setName("ID");
        id.setDisplayName("主键");
        id.setShowInDetail(Boolean.TRUE);
        id.setShowInForm(Boolean.TRUE);
        id.setShowInList(Boolean.TRUE);
        snapshot.getFields().getSystemFields().add(id);
        snapshot.getFields().getListOrder().add("ID");
        return snapshot;
    }

    private DynamicTableViewSnapshot.SystemButton systemButton(String name) {
        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName(name);
        return button;
    }

    private void assertError(DynamicTableViewValidationResult result, String path, String code) {
        for (DynamicTableViewResponse.ValidationError error : result.getErrors()) {
            if (path.equals(error.getPath()) && code.equals(error.getCode())) {
                return;
            }
        }
        throw new AssertionError("Expected error " + code + " at " + path);
    }

    private static class FakeRepository implements DynamicTableViewRepository {
        public List<VwUrl> listDynUrls(int start, int limit) {
            return Collections.emptyList();
        }

        public int countDynUrls() {
            return 0;
        }

        public VwUrl findUrl(String viewKey) {
            return null;
        }

        public Map<String, Object> findTable(String viewKey) {
            return null;
        }

        public Map<String, Object> findTableDefinition(String tableName) {
            if ("CRM_CUSTOMER".equals(tableName) || "CRM_PARENT".equals(tableName)) {
                Map<String, Object> table = new LinkedHashMap<String, Object>();
                table.put("name", tableName);
                table.put("primaryKeyName", "ID");
                table.put("primaryKeyType", "VARCHAR");
                return table;
            }
            return null;
        }

        public Map<String, Object> findColumnDefinition(String tableName, String columnName) {
            if ("CRM_CUSTOMER".equals(tableName)
                    && ("ID".equals(columnName) || "PARENT_ID".equals(columnName))) {
                return column(tableName, columnName);
            }
            if ("CRM_PARENT".equals(tableName) && "ID".equals(columnName)) {
                return column(tableName, columnName);
            }
            return null;
        }

        public VwUrl saveUrl(VwUrl url) {
            return url;
        }

        public void updateUrl(VwUrl url) {
        }

        public void saveDynamicEntity(String entityName, Map<String, Object> values) {
        }

        public void updateDynamicEntity(String entityName, Map<String, Object> values) {
        }

        public void removeDynamicEntity(String entityName, Object id) {
        }

        public void removeViewConfig(String viewKey) {
        }

        public void flushAndClearViewCache(String viewKey) {
        }

        private Map<String, Object> column(String tableName, String columnName) {
            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("tableName", tableName);
            column.put("name", columnName);
            return column;
        }
    }
}
