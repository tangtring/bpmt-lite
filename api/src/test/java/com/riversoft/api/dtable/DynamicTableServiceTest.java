package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.core.db.DataPackage;
import com.riversoft.platform.po.TbColumn;
import com.riversoft.platform.po.TbTable;
import org.junit.Test;

import java.util.ArrayList;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DynamicTableServiceTest {
    @Test
    public void convertsRequestToTbTable() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setDescription("API测试表");
        request.setCacheFlag(0);
        request.setColumns(Arrays.asList(primaryKey("ID")));

        TbTable table = DynamicTableService.toTbTable(request);

        assertEquals("RV_API_TEST", table.getName());
        assertEquals("API测试表", table.getDescription());
        assertEquals(Integer.valueOf(0), table.getCacheFlag());
        assertEquals(1, table.getTbColumns().size());
        TbColumn column = table.getTbColumns().iterator().next();
        assertEquals("RV_API_TEST", column.getTableName());
        assertEquals("ID", column.getName());
        assertEquals(Integer.valueOf(0), column.getSort());
    }

    @Test
    public void convertsColumnTypeToJdbcCode() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setColumns(Arrays.asList(primaryKey("ID")));

        TbTable table = DynamicTableService.toTbTable(request);
        TbColumn column = table.getTbColumns().iterator().next();

        assertEquals(Types.VARCHAR, column.getMappedTypeCode());
    }

    @Test
    public void convertsTableToResponseDto() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setDescription("API测试表");
        request.setColumns(Arrays.asList(primaryKey("ID")));

        DynamicTableResponse response = DynamicTableService.toResponse(DynamicTableService.toTbTable(request));

        assertEquals("RV_API_TEST", response.getName());
        assertEquals("API测试表", response.getDescription());
        assertEquals(1, response.getColumns().size());
        assertEquals("String", response.getColumns().get(0).getType());
    }

    @Test
    public void rejectsUnsupportedColumnType() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        DynamicTableRequest.Column id = primaryKey("ID");
        id.setType("Unsupported");
        request.setColumns(Arrays.asList(id));
        try {
            DynamicTableService.toTbTable(request);
        } catch (ApiException e) {
            assertEquals("DYNAMIC_TABLE_COLUMN_TYPE_INVALID", e.getCode());
            return;
        }
        throw new AssertionError("Expected unsupported type rejection");
    }

    @Test
    public void updateReturnsNotFoundBeforeExecutingDdlWhenMetadataIsMissing() {
        TestableDynamicTableService service = new TestableDynamicTableService(null);

        try {
            service.update("RV_API_MISSING", sampleRequest("RV_API_MISSING"));
        } catch (ApiException e) {
            assertEquals(404, e.getStatus());
            assertEquals("DYNAMIC_TABLE_NOT_FOUND", e.getCode());
            assertFalse(service.updateExecuted);
            return;
        }
        throw new AssertionError("Expected missing dynamic table to return 404");
    }

    @Test
    public void updateUsesSafeDdlModeForStructuralChanges() {
        TestableDynamicTableService service = new TestableDynamicTableService(sampleTable("RV_API_TEST"));

        service.update("RV_API_TEST", sampleRequest("RV_API_TEST"));

        assertTrue(service.updateExecuted);
        assertFalse("API updates must use TableService safe DDL mode", service.lastUpdateUnsafeMode);
    }

    @Test
    public void syncDdlUsesSafeDdlMode() {
        TestableDynamicTableService service = new TestableDynamicTableService(sampleTable("RV_API_TEST"));

        service.syncDdl("RV_API_TEST");

        assertTrue(service.syncExecuted);
        assertFalse("API sync-ddl must use TableService safe DDL mode", service.lastSyncUnsafeMode);
    }

    @Test
    public void listAppliesWhitelistedSortParameter() {
        TestableDynamicTableService service = new TestableDynamicTableService(null);

        service.list(0, 20, "createDate", "desc");

        assertEquals("desc", service.queryMap.get("_orderby_createDate"));
    }

    private DynamicTableRequest.Column primaryKey(String name) {
        DynamicTableRequest.Column column = new DynamicTableRequest.Column();
        column.setName(name);
        column.setType("String");
        column.setTotalSize(64);
        column.setPrimaryKey(true);
        column.setRequired(true);
        return column;
    }

    private DynamicTableRequest sampleRequest(String tableName) {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName(tableName);
        request.setColumns(Arrays.asList(primaryKey("ID"), stringColumn("NAME_STR")));
        return request;
    }

    private DynamicTableRequest.Column stringColumn(String name) {
        DynamicTableRequest.Column column = new DynamicTableRequest.Column();
        column.setName(name);
        column.setType("String");
        column.setTotalSize(100);
        return column;
    }

    private TbTable sampleTable(String tableName) {
        return DynamicTableService.toTbTable(sampleRequest(tableName));
    }

    private static class TestableDynamicTableService extends DynamicTableService {
        private final TbTable existingTable;
        private boolean updateExecuted;
        private boolean syncExecuted;
        private boolean lastUpdateUnsafeMode;
        private boolean lastSyncUnsafeMode;
        private Map<String, ?> queryMap;

        TestableDynamicTableService(TbTable existingTable) {
            super(null);
            this.existingTable = existingTable;
        }

        @Override
        protected DataPackage queryTables(int start, int limit, Map<String, ?> queryMap) {
            this.queryMap = queryMap;
            DataPackage dataPackage = new DataPackage();
            dataPackage.setStart(start);
            dataPackage.setLimit(limit);
            dataPackage.setTotalRecord(existingTable == null ? 0 : 1);
            List<TbTable> tables = new ArrayList<TbTable>();
            if (existingTable != null) {
                tables.add(existingTable);
            }
            dataPackage.setList(tables);
            return dataPackage;
        }

        @Override
        protected TbTable findDynamicTable(String tableName) {
            if (existingTable == null || !existingTable.getName().equals(tableName)) {
                return null;
            }
            return existingTable;
        }

        @Override
        protected void executeUpdateTable(TbTable table, boolean unsafeMode) {
            this.updateExecuted = true;
            this.lastUpdateUnsafeMode = unsafeMode;
        }

        @Override
        protected void executeSyncTable(String tableName, boolean unsafeMode) {
            this.syncExecuted = true;
            this.lastSyncUnsafeMode = unsafeMode;
        }
    }
}
