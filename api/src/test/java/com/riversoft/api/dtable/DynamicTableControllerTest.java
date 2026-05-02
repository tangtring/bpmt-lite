package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.api.http.ApiJson;
import com.riversoft.api.http.ApiRequest;
import com.riversoft.core.db.DataPackage;
import com.riversoft.platform.po.TbTable;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicTableControllerTest {

    @Test
    public void listReturnsPagedDynamicTables() {
        StubTableService tableService = new StubTableService();
        DynamicTableController controller = new DynamicTableController(tableService, new StubTemplateService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/dynamic-tables");
        request.addParameter("start", "5");
        request.addParameter("limit", "10");

        Map<String, Object> response = controller.list(new ApiRequest(request));

        assertEquals(Integer.valueOf(5), tableService.start);
        assertEquals(Integer.valueOf(10), tableService.limit);
        assertEquals(1, ((java.util.List<?>) response.get("items")).size());
        assertEquals(Long.valueOf(1L), Long.valueOf(((Number) response.get("totalRecord")).longValue()));
    }

    @Test
    public void listPassesSortAndOrderParameters() {
        StubTableService tableService = new StubTableService();
        DynamicTableController controller = new DynamicTableController(tableService, new StubTemplateService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/dynamic-tables");
        request.addParameter("sort", "createDate");
        request.addParameter("order", "desc");

        Map<String, Object> response = controller.list(new ApiRequest(request));

        assertEquals("createDate", tableService.sort);
        assertEquals("desc", tableService.order);
        assertEquals("createDate", response.get("sort"));
        assertEquals("desc", response.get("order"));
    }

    @Test
    public void listRejectsUnsupportedSortParameter() {
        DynamicTableController controller = new DynamicTableController(new StubTableService(), new StubTemplateService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/dynamic-tables");
        request.addParameter("sort", "name;drop table tb_table");

        try {
            controller.list(new ApiRequest(request));
        } catch (ApiException e) {
            assertEquals("API_INVALID_PARAMETER", e.getCode());
            return;
        }
        throw new AssertionError("Expected invalid sort rejection");
    }

    @Test
    public void createReadsJsonPayload() throws UnsupportedEncodingException {
        StubTableService tableService = new StubTableService();
        DynamicTableController controller = new DynamicTableController(tableService, new StubTemplateService());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/dynamic-tables");
        request.setContentType("application/json");
        request.setContent(ApiJson.toJson(sampleRequest()).getBytes("UTF-8"));

        DynamicTableResponse response = controller.create(new ApiRequest(request));

        assertEquals("RV_API_TEST", tableService.created.getName());
        assertEquals("RV_API_TEST", response.getName());
    }

    @Test
    public void rejectsInvalidLimit() {
        DynamicTableController controller = new DynamicTableController(new StubTableService(), new StubTemplateService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/dynamic-tables");
        request.addParameter("limit", "101");

        try {
            controller.list(new ApiRequest(request));
        } catch (ApiException e) {
            assertEquals("API_INVALID_PARAMETER", e.getCode());
            return;
        }
        throw new AssertionError("Expected invalid pagination rejection");
    }

    @Test
    public void returnsTemplateNames() {
        DynamicTableController controller = new DynamicTableController(new StubTableService(), new StubTemplateService());

        Map<String, Object> response = controller.templates();

        assertTrue(((Set<?>) response.get("items")).contains("default"));
    }

    private static DynamicTableRequest sampleRequest() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setColumns(Arrays.asList(primaryKey()));
        return request;
    }

    private static DynamicTableRequest.Column primaryKey() {
        DynamicTableRequest.Column column = new DynamicTableRequest.Column();
        column.setName("ID");
        column.setType("String");
        column.setTotalSize(64);
        column.setPrimaryKey(true);
        column.setRequired(true);
        return column;
    }

    private static class StubTableService extends DynamicTableService {
        private Integer start;
        private Integer limit;
        private String sort;
        private String order;
        private DynamicTableRequest created;

        StubTableService() {
            super(null);
        }

        @Override
        public DataPackage list(int start, int limit) {
            return list(start, limit, null, null);
        }

        @Override
        public DataPackage list(int start, int limit, String sort, String order) {
            this.start = start;
            this.limit = limit;
            this.sort = sort;
            this.order = order;
            DataPackage dataPackage = new DataPackage();
            dataPackage.setStart(start);
            dataPackage.setLimit(limit);
            dataPackage.setTotalRecord(1L);
            dataPackage.setList(Collections.singletonList(DynamicTableService.toTbTable(sampleRequest())));
            return dataPackage;
        }

        @Override
        public TbTable create(DynamicTableRequest request) {
            this.created = request;
            return DynamicTableService.toTbTable(request);
        }
    }

    private static class StubTemplateService extends DynamicTableTemplateService {
        @Override
        public Set<String> listTemplates() {
            return Collections.singleton("default");
        }
    }
}
