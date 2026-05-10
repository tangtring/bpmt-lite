package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiException;
import com.riversoft.platform.po.VwUrl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DynamicTableViewServiceTest {
    @Test
    public void dryRunCreateDoesNotWriteRepository() {
        RecordingRepository repository = new RecordingRepository();
        DynamicTableViewService service = new DynamicTableViewService(repository);

        Map<String, Object> result = service.create(snapshot("CRM_CUSTOMER_VIEW"), true);

        assertTrue(repository.savedUrls.isEmpty());
        assertTrue(repository.savedViewConfigs.isEmpty());
        assertEquals(Integer.valueOf(0), Integer.valueOf(repository.flushes));
        DynamicTableViewResponse.WritePlan plan = (DynamicTableViewResponse.WritePlan) result.get("plan");
        assertTrue(plan.isDryRun());
        assertTrue(plan.getCreates().contains("VW_URL"));
        assertTrue(plan.getCreates().contains("VW_DYN_TABLE"));
    }

    @Test
    public void createWritesUrlAndTableWhenNotDryRun() {
        RecordingRepository repository = new RecordingRepository();
        DynamicTableViewService service = new DynamicTableViewService(repository);

        service.create(snapshot("CRM_CUSTOMER_VIEW"), false);

        assertEquals(1, repository.savedUrls.size());
        assertEquals("CRM_CUSTOMER_VIEW", repository.savedUrls.get(0).getViewKey());
        assertTrue(repository.savedViewConfigs.containsKey("CRM_CUSTOMER_VIEW"));
        assertEquals(1, repository.flushes);
    }

    @Test
    public void exportReturnsSnapshot() {
        RecordingRepository repository = new RecordingRepository();
        DynamicTableViewSnapshot source = snapshot("CRM_CUSTOMER_VIEW");
        repository.seedDynView(source);
        DynamicTableViewService service = new DynamicTableViewService(repository);

        Map<String, Object> result = service.export("CRM_CUSTOMER_VIEW");

        DynamicTableViewSnapshot exported = (DynamicTableViewSnapshot) result.get("snapshot");
        assertEquals("CRM_CUSTOMER_VIEW", exported.getViewKey());
        assertEquals("CRM_CUSTOMER", exported.getBase().getTableName());
        assertEquals("ID", exported.getFields().getSystemFields().get(0).getName());
    }

    @Test
    public void validateInvalidReturnsValidFalse() {
        RecordingRepository repository = new RecordingRepository();
        DynamicTableViewService service = new DynamicTableViewService(repository);
        DynamicTableViewSnapshot invalid = snapshot("CRM_CUSTOMER_VIEW");
        invalid.getBase().setTableName(null);

        Map<String, Object> result = service.validate(invalid);

        assertEquals(Boolean.FALSE, result.get("valid"));
        assertFalse(((List<?>) result.get("errors")).isEmpty());
    }

    @Test
    public void patchFieldsDryRunDoesNotWriteAndReplacesFieldsInReturnedSnapshot() {
        RecordingRepository repository = new RecordingRepository();
        repository.seedDynView(snapshot("CRM_CUSTOMER_VIEW"));
        DynamicTableViewService service = new DynamicTableViewService(repository);
        DynamicTableViewSnapshot.Fields fields = new DynamicTableViewSnapshot.Fields();
        DynamicTableViewSnapshot.Field name = field("NAME", "客户名称");
        fields.setSystemFields(Collections.singletonList(name));
        fields.setListOrder(Collections.singletonList("NAME"));

        Map<String, Object> result = service.patch("CRM_CUSTOMER_VIEW", DynamicTableViewSection.FIELDS, fields, true);

        DynamicTableViewSnapshot patched = (DynamicTableViewSnapshot) result.get("snapshot");
        assertEquals("NAME", patched.getFields().getSystemFields().get(0).getName());
        assertTrue(repository.savedViewConfigs.isEmpty());
        assertTrue(repository.removedTableConfigKeys.isEmpty());
        assertEquals(0, repository.flushes);
    }

    @Test
    public void deleteRequiresConfirmViewKey() {
        RecordingRepository repository = new RecordingRepository();
        repository.seedDynView(snapshot("CRM_CUSTOMER_VIEW"));
        DynamicTableViewService service = new DynamicTableViewService(repository);

        try {
            service.delete("CRM_CUSTOMER_VIEW", "OTHER_VIEW");
            fail("Expected confirm required error");
        } catch (ApiException e) {
            assertEquals("DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED", e.getCode());
        }
        assertTrue(repository.removedViewKeys.isEmpty());
    }

    @Test
    public void deleteConfirmedRemovesConfigNotBusinessTableFlags() {
        RecordingRepository repository = new RecordingRepository();
        repository.seedDynView(snapshot("CRM_CUSTOMER_VIEW"));
        DynamicTableViewService service = new DynamicTableViewService(repository);

        Map<String, Object> result = service.delete("CRM_CUSTOMER_VIEW", "CRM_CUSTOMER_VIEW");

        assertEquals(Collections.singletonList("CRM_CUSTOMER_VIEW"), repository.removedViewKeys);
        assertEquals(Boolean.TRUE, result.get("deleted"));
        assertEquals(Boolean.FALSE, result.get("businessTableDeleted"));
        assertEquals(Boolean.FALSE, result.get("businessDataDeleted"));
        assertEquals(1, repository.flushes);
    }

    @Test
    public void replaceRejectsNonDynView() {
        RecordingRepository repository = new RecordingRepository();
        repository.urls.put("REPORT_VIEW", url("REPORT_VIEW", "report"));
        DynamicTableViewService service = new DynamicTableViewService(repository);

        try {
            service.replace("REPORT_VIEW", snapshot("REPORT_VIEW"), false);
            fail("Expected non dyn error");
        } catch (ApiException e) {
            assertEquals("DYNAMIC_TABLE_VIEW_NOT_DYN", e.getCode());
            assertEquals(409, e.getStatus());
        }
    }

    private DynamicTableViewSnapshot snapshot(String viewKey) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        snapshot.setViewKey(viewKey);
        snapshot.setDescription("客户资料维护视图");
        snapshot.setLoginRequired(true);
        snapshot.getBase().setTableName("CRM_CUSTOMER");
        snapshot.getBase().setDisplayName("客户资料");
        snapshot.getBase().setLayoutColumns(Integer.valueOf(2));
        snapshot.getBase().setInitQuery(Boolean.TRUE);
        snapshot.getBase().setPageLimit(Integer.valueOf(20));
        snapshot.getBase().getDefaultSort().setField("ID");
        snapshot.getBase().getDefaultSort().setDirection("desc");
        snapshot.getFields().getSystemFields().add(field("ID", "主键"));
        snapshot.getFields().getListOrder().add("ID");
        return snapshot;
    }

    private DynamicTableViewSnapshot.Field field(String name, String displayName) {
        DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
        field.setName(name);
        field.setDisplayName(displayName);
        field.setShowInDetail(Boolean.TRUE);
        field.setShowInForm(Boolean.TRUE);
        field.setShowInList(Boolean.TRUE);
        field.setWidget("text");
        return field;
    }

    private static VwUrl url(String viewKey, String viewClass) {
        VwUrl url = new VwUrl();
        url.setViewKey(viewKey);
        url.setViewClass(viewClass);
        url.setDescription("客户资料维护视图");
        url.setLoginType(Integer.valueOf(1));
        url.setLockFlag(Integer.valueOf(0));
        url.setCreateUid("admin");
        return url;
    }

    private static class RecordingRepository implements DynamicTableViewRepository {
        private final Map<String, VwUrl> urls = new LinkedHashMap<String, VwUrl>();
        private final Map<String, Map<String, Object>> tables = new LinkedHashMap<String, Map<String, Object>>();
        private final List<VwUrl> savedUrls = new ArrayList<VwUrl>();
        private final Map<String, Map<String, Object>> savedViewConfigs = new LinkedHashMap<String, Map<String, Object>>();
        private final List<String> removedViewKeys = new ArrayList<String>();
        private final List<String> removedTableConfigKeys = new ArrayList<String>();
        private int flushes;

        public List<VwUrl> listDynUrls(int start, int limit) {
            return new ArrayList<VwUrl>(urls.values());
        }

        public int countDynUrls() {
            return urls.size();
        }

        public VwUrl findUrl(String viewKey) {
            return urls.get(viewKey);
        }

        public Map<String, Object> findTable(String viewKey) {
            return tables.get(viewKey);
        }

        public Map<String, Object> findTableDefinition(String tableName) {
            if (!"CRM_CUSTOMER".equals(tableName)) {
                return null;
            }
            Map<String, Object> table = new LinkedHashMap<String, Object>();
            table.put("name", tableName);
            table.put("primaryKeyName", "ID");
            table.put("primaryKeyType", "VARCHAR");
            return table;
        }

        public Map<String, Object> findColumnDefinition(String tableName, String columnName) {
            if (!"CRM_CUSTOMER".equals(tableName)) {
                return null;
            }
            if (!"ID".equals(columnName) && !"NAME".equals(columnName)) {
                return null;
            }
            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("tableName", tableName);
            column.put("name", columnName);
            column.put("typeName", "VARCHAR");
            column.put("totalSize", Integer.valueOf(100));
            column.put("primaryKey", Boolean.valueOf("ID".equals(columnName)));
            column.put("required", Boolean.valueOf("ID".equals(columnName)));
            return column;
        }

        public VwUrl saveUrl(VwUrl url) {
            savedUrls.add(url);
            urls.put(url.getViewKey(), url);
            return url;
        }

        public void updateUrl(VwUrl url) {
            urls.put(url.getViewKey(), url);
        }

        public void saveViewConfig(String viewKey, Map<String, Object> tableMap) {
            savedViewConfigs.put(viewKey, tableMap);
            tables.put(viewKey, tableMap);
        }

        public void saveDynamicEntity(String entityName, Map<String, Object> values) {
        }

        public void updateDynamicEntity(String entityName, Map<String, Object> values) {
        }

        public void removeDynamicEntity(String entityName, Object id) {
        }

        public void removeDynamicTableConfig(String viewKey) {
            removedTableConfigKeys.add(viewKey);
            tables.remove(viewKey);
        }

        public void removeViewConfig(String viewKey) {
            removedViewKeys.add(viewKey);
            urls.remove(viewKey);
            tables.remove(viewKey);
        }

        public void flushAndClearViewCache(String viewKey) {
            flushes++;
        }

        private void seedDynView(DynamicTableViewSnapshot snapshot) {
            VwUrl url = url(snapshot.getViewKey(), "dyn");
            urls.put(snapshot.getViewKey(), url);
            tables.put(snapshot.getViewKey(), new DynamicTableViewMapper().toTableMap(snapshot));
        }
    }
}
