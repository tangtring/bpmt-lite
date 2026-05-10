package com.riversoft.api.modules.dynamic_table_views;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DynamicTableViewMapperTest {
    @Test
    public void exportsVwDynMapsToAgentSnapshot() {
        Map<String, Object> url = new LinkedHashMap<String, Object>();
        url.put("viewKey", "CRM_CUSTOMER_VIEW");
        url.put("viewClass", "dyn");
        url.put("description", "客户资料维护视图");
        url.put("loginType", Integer.valueOf(1));

        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", "CRM_CUSTOMER_VIEW");
        table.put("name", "CRM_CUSTOMER");
        table.put("busiName", "客户资料");
        table.put("sortName", "CREATE_DATE");
        table.put("dir", "desc");
        table.put("col", Integer.valueOf(2));
        table.put("initQuery", Integer.valueOf(1));
        table.put("pageLimit", Integer.valueOf(20));
        table.put("columns", java.util.Collections.singleton(column("CUSTOMER_ID", "客户ID")));

        DynamicTableViewSnapshot snapshot = new DynamicTableViewMapper().toSnapshot(url, table);

        assertEquals("CRM_CUSTOMER_VIEW", snapshot.getViewKey());
        assertEquals("CRM_CUSTOMER", snapshot.getBase().getTableName());
        assertEquals("CUSTOMER_ID", snapshot.getFields().getSystemFields().get(0).getName());
        assertEquals("CUSTOMER_ID", snapshot.getFields().getListOrder().get(0));
    }

    @Test
    public void sortsChildrenBySortAndListOrderByListSort() {
        Map<String, Object> url = new LinkedHashMap<String, Object>();
        url.put("viewKey", "CRM_CUSTOMER_VIEW");

        Map<String, Object> second = column("SECOND_FIELD", "第二字段");
        second.put("sort", Integer.valueOf(2));
        second.put("listSort", Integer.valueOf(20));
        Map<String, Object> first = column("FIRST_FIELD", "第一字段");
        first.put("sort", Integer.valueOf(1));
        first.put("listSort", Integer.valueOf(10));

        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", "CRM_CUSTOMER_VIEW");
        table.put("name", "CRM_CUSTOMER");
        table.put("columns", Arrays.asList(second, first));

        DynamicTableViewSnapshot snapshot = new DynamicTableViewMapper().toSnapshot(url, table);

        assertEquals("FIRST_FIELD", snapshot.getFields().getSystemFields().get(0).getName());
        assertEquals("SECOND_FIELD", snapshot.getFields().getSystemFields().get(1).getName());
        assertEquals("FIRST_FIELD", snapshot.getFields().getListOrder().get(0));
        assertEquals("SECOND_FIELD", snapshot.getFields().getListOrder().get(1));
    }

    private Map<String, Object> column(String name, String displayName) {
        Map<String, Object> column = new LinkedHashMap<String, Object>();
        column.put("name", name);
        column.put("busiName", displayName);
        column.put("showFlag", Integer.valueOf(1));
        column.put("formFlag", Integer.valueOf(1));
        column.put("listSort", Integer.valueOf(1));
        column.put("sort", Integer.valueOf(1));
        column.put("widget", "text");
        column.put("whole", Integer.valueOf(0));
        column.put("contentType", Integer.valueOf(1));
        column.put("contentScript", "return vo." + name + ";");
        return column;
    }
}
