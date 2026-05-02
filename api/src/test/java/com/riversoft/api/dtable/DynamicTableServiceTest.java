package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.platform.po.TbColumn;
import com.riversoft.platform.po.TbTable;
import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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

    private DynamicTableRequest.Column primaryKey(String name) {
        DynamicTableRequest.Column column = new DynamicTableRequest.Column();
        column.setName(name);
        column.setType("String");
        column.setTotalSize(64);
        column.setPrimaryKey(true);
        column.setRequired(true);
        return column;
    }
}
