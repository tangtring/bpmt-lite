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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DynamicTableViewDefaultsTest {
    @Test
    public void normalizeForCreateGeneratesViewKeyWhenMissing() {
        DynamicTableViewSnapshot snapshot = snapshotWithFields();

        new DynamicTableViewDefaults().normalizeForCreate(snapshot, new DefaultsRepository());

        assertNotNull(snapshot.getViewKey());
        assertFalse(snapshot.getViewKey().trim().length() == 0);
    }

    @Test
    public void normalizeDoesNotGenerateViewKeyWhenMissing() {
        DynamicTableViewSnapshot snapshot = snapshotWithFields();

        new DynamicTableViewDefaults().normalize(snapshot, new DefaultsRepository());

        assertNull(snapshot.getViewKey());
    }

    @Test
    public void normalizeDefaultsRequiredStringWidget() {
        DynamicTableViewSnapshot snapshot = snapshotWithFields(field("NAME"));

        new DynamicTableViewDefaults().normalize(snapshot, new DefaultsRepository());

        assertTrue(snapshot.getFields().getSystemFields().get(0).getWidget().contains("required:true"));
    }

    @Test
    public void normalizeDefaultsIntegerWidgetToDigits() {
        DynamicTableViewSnapshot snapshot = snapshotWithFields(field("AGE"));

        new DynamicTableViewDefaults().normalize(snapshot, new DefaultsRepository());

        assertTrue(snapshot.getFields().getSystemFields().get(0).getWidget().contains("digits:true"));
    }

    @Test
    public void normalizeDefaultsClobAndLargeStringToTextareaWholeLine() {
        DynamicTableViewSnapshot clobSnapshot = snapshotWithFields(field("BIO"));
        DynamicTableViewSnapshot largeStringSnapshot = snapshotWithFields(field("MEMO"));

        new DynamicTableViewDefaults().normalize(clobSnapshot, new DefaultsRepository());
        new DynamicTableViewDefaults().normalize(largeStringSnapshot, new DefaultsRepository());

        assertEquals("textarea", clobSnapshot.getFields().getSystemFields().get(0).getWidget());
        assertEquals(Boolean.TRUE, clobSnapshot.getFields().getSystemFields().get(0).getWholeLine());
        assertEquals("textarea", largeStringSnapshot.getFields().getSystemFields().get(0).getWidget());
        assertEquals(Boolean.TRUE, largeStringSnapshot.getFields().getSystemFields().get(0).getWholeLine());
    }

    @Test
    public void normalizeDefaultsSystemButtonsToDynStorageNames() {
        DynamicTableViewSnapshot snapshot = snapshotWithFields();
        snapshot.getButtons().setSystem(null);

        new DynamicTableViewDefaults().normalize(snapshot, new DefaultsRepository());

        assertEquals("show", snapshot.getButtons().getSystem().get(0).getName());
        assertEquals("edit", snapshot.getButtons().getSystem().get(1).getName());
        assertEquals("del", snapshot.getButtons().getSystem().get(2).getName());
        assertEquals("create", snapshot.getButtons().getSystem().get(3).getName());
    }

    private DynamicTableViewSnapshot snapshotWithFields() {
        return snapshotWithFields(field("NAME"));
    }

    private DynamicTableViewSnapshot snapshotWithFields(DynamicTableViewSnapshot.Field field) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        snapshot.getBase().setTableName("CRM_CUSTOMER");
        snapshot.getFields().getSystemFields().add(field);
        return snapshot;
    }

    private DynamicTableViewSnapshot.Field field(String name) {
        DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
        field.setName(name);
        return field;
    }

    private static class DefaultsRepository implements DynamicTableViewRepository {
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
            return Collections.emptyMap();
        }

        public Map<String, Object> findColumnDefinition(String tableName, String columnName) {
            if (!"CRM_CUSTOMER".equals(tableName)) {
                return null;
            }
            if ("NAME".equals(columnName)) {
                return column("NAME", "VARCHAR", Integer.valueOf(80), Boolean.FALSE, Boolean.TRUE);
            }
            if ("AGE".equals(columnName)) {
                return column("AGE", "INTEGER", Integer.valueOf(10), Boolean.FALSE, Boolean.FALSE);
            }
            if ("BIO".equals(columnName)) {
                return column("BIO", "CLOB", Integer.valueOf(0), Boolean.FALSE, Boolean.FALSE);
            }
            if ("MEMO".equals(columnName)) {
                return column("MEMO", "VARCHAR", Integer.valueOf(1200), Boolean.FALSE, Boolean.FALSE);
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

        private Map<String, Object> column(String name, String type, Integer totalSize, Boolean primaryKey, Boolean required) {
            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("name", name);
            column.put("typeName", type);
            column.put("totalSize", totalSize);
            column.put("primaryKey", primaryKey);
            column.put("required", required);
            return column;
        }
    }
}
