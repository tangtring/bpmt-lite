package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.api.http.ApiRequest;
import com.riversoft.core.db.DataPackage;
import com.riversoft.platform.po.TbTable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicTableController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private DynamicTableService tableService;
    private DynamicTableTemplateService templateService;

    public DynamicTableController() {
    }

    public DynamicTableController(DynamicTableService tableService, DynamicTableTemplateService templateService) {
        this.tableService = tableService;
        this.templateService = templateService;
    }

    public Map<String, Object> list(ApiRequest request) {
        int start = parseStart(request.getParameter("start"));
        int limit = parseLimit(request.getParameter("limit"));
        String sort = DynamicTableService.normalizeSort(request.getParameter("sort"));
        String order = DynamicTableService.normalizeOrder(sort, request.getParameter("order"));
        DataPackage dataPackage = tableService().list(start, limit, sort, order);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("start", dataPackage.getStart());
        result.put("limit", dataPackage.getLimit());
        result.put("sort", sort);
        result.put("order", order);
        result.put("totalRecord", dataPackage.getTotalRecord());
        result.put("items", toResponses(dataPackage.getList()));
        return result;
    }

    public DynamicTableResponse detail(String name) {
        return DynamicTableService.toResponse(tableService().detail(name));
    }

    public DynamicTableResponse create(ApiRequest request) {
        DynamicTableRequest payload = request.readJson(DynamicTableRequest.class);
        return DynamicTableService.toResponse(tableService().create(payload));
    }

    public DynamicTableResponse update(String name, ApiRequest request) {
        DynamicTableRequest payload = request.readJson(DynamicTableRequest.class);
        return DynamicTableService.toResponse(tableService().update(name, payload));
    }

    public Map<String, Object> syncDdl(String name) {
        tableService().syncDdl(name);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", StringUtils.trim(name));
        result.put("synced", Boolean.TRUE);
        return result;
    }

    public Map<String, Object> templates() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", templateService().listTemplates());
        return result;
    }

    private DynamicTableService tableService() {
        if (tableService == null) {
            tableService = new DynamicTableService();
        }
        return tableService;
    }

    private DynamicTableTemplateService templateService() {
        if (templateService == null) {
            templateService = new DynamicTableTemplateService();
        }
        return templateService;
    }

    private List<DynamicTableResponse> toResponses(List<?> tables) {
        List<DynamicTableResponse> responses = new ArrayList<DynamicTableResponse>();
        if (tables == null) {
            return responses;
        }
        for (Object table : tables) {
            if (table instanceof TbTable) {
                responses.add(DynamicTableService.toResponse((TbTable) table));
            }
        }
        return responses;
    }

    private int parseStart(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        int start = parseInt(value);
        if (start < 0) {
            throw new ApiException(400, "API_INVALID_PARAMETER", "分页参数 start 不能小于 0。");
        }
        return start;
    }

    private int parseLimit(String value) {
        if (StringUtils.isBlank(value)) {
            return DEFAULT_LIMIT;
        }
        int limit = parseInt(value);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ApiException(400, "API_INVALID_PARAMETER", "分页参数 limit 范围是 1 到 100。");
        }
        return limit;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(StringUtils.trim(value));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "API_INVALID_PARAMETER", "分页参数无效。");
        }
    }
}
