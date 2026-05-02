package com.riversoft.api.modules.database_operations;

import com.riversoft.api.http.ApiException;
import com.riversoft.platform.db.DbHelper;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatabaseOperationServiceTest {

    @Test
    public void queryRequiresSelect() {
        DatabaseOperationService service = new DatabaseOperationService(new StubDbHelper());
        DatabaseOperationRequest request = new DatabaseOperationRequest();
        request.setSql("update TB_TABLE set DESCRIPTION=? where NAME=?");
        request.setArgs(new Object[]{"x", "T1"});
        try {
            service.query(request);
        } catch (ApiException e) {
            assertEquals("DBOPS_SQL_NOT_ALLOWED", e.getCode());
            assertEquals(422, e.getStatus());
            return;
        }
        throw new AssertionError("query should reject non-select sql");
    }

    @Test
    public void findReturnsMap() {
        DatabaseOperationService service = new DatabaseOperationService(new StubDbHelper());
        DatabaseOperationRequest request = new DatabaseOperationRequest();
        request.setSql("select NAME from TB_TABLE where NAME=?");
        request.setArgs(new Object[]{"RV_TEST"});
        Map<String, Object> data = service.find(request);
        assertTrue(data.containsKey("item"));
    }

    private static final class StubDbHelper extends DbHelper {
        @Override
        public java.util.List<?> query(String sql, Object... args) {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> find(String sql, Object... args) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("NAME", "RV_TEST");
            return row;
        }
    }
}
