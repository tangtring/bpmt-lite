package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.platform.po.CmPri;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void hidesNegativeListSortFromListOrder() {
        Map<String, Object> url = new LinkedHashMap<String, Object>();
        url.put("viewKey", "CRM_CUSTOMER_VIEW");

        Map<String, Object> hidden = column("HIDDEN_FIELD", "隐藏字段");
        hidden.put("listSort", Integer.valueOf(-1));

        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", "CRM_CUSTOMER_VIEW");
        table.put("name", "CRM_CUSTOMER");
        table.put("columns", Collections.singleton(hidden));

        DynamicTableViewSnapshot snapshot = new DynamicTableViewMapper().toSnapshot(url, table);

        assertFalse(snapshot.getFields().getSystemFields().get(0).getShowInList().booleanValue());
        assertTrue(snapshot.getFields().getListOrder().isEmpty());
    }

    @Test
    public void exportsFormOnlyColumnsWithFormDefaults() {
        Map<String, Object> url = new LinkedHashMap<String, Object>();
        url.put("viewKey", "CRM_CUSTOMER_VIEW");

        Map<String, Object> formOnly = formColumn("FORM_NOTE", "表单备注");

        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", "CRM_CUSTOMER_VIEW");
        table.put("name", "CRM_CUSTOMER");
        table.put("formColumns", Collections.singleton(formOnly));

        DynamicTableViewSnapshot snapshot = new DynamicTableViewMapper().toSnapshot(url, table);

        assertEquals("FORM_NOTE", snapshot.getFields().getFormFields().get(0).getName());
        assertFalse(snapshot.getFields().getFormFields().get(0).getShowInDetail().booleanValue());
        assertTrue(snapshot.getFields().getFormFields().get(0).getShowInForm().booleanValue());
        assertFalse(snapshot.getFields().getFormFields().get(0).getShowInList().booleanValue());
    }

    @Test
    public void exportsSectionLinePermissions() {
        Map<String, Object> url = new LinkedHashMap<String, Object>();
        url.put("viewKey", "CRM_CUSTOMER_VIEW");

        Map<String, Object> stringPriLine = sectionLine("basic", "基础信息", "pri-line-basic", 1);
        Map<String, Object> mapPriLine = sectionLine("extra", "扩展信息", priMap("pri-line-extra"), 2);
        Map<String, Object> table = new LinkedHashMap<String, Object>();
        table.put("viewKey", "CRM_CUSTOMER_VIEW");
        table.put("name", "CRM_CUSTOMER");
        table.put("lineColumns", Arrays.asList(stringPriLine, mapPriLine));

        DynamicTableViewSnapshot snapshot = new DynamicTableViewMapper().toSnapshot(url, table);

        assertEquals("pri-line-basic",
                snapshot.getFields().getSectionLines().get(0).getPermissions().getView());
        assertEquals("pri-line-extra",
                snapshot.getFields().getSectionLines().get(1).getPermissions().getView());
    }

    @Test
    public void writesCompleteCmPriForDynamicMapPermissions() {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        snapshot.setViewKey("CRM_CUSTOMER_VIEW");
        snapshot.getBase().setTableName("CRM_CUSTOMER");
        snapshot.getBase().setDisplayName("客户资料");

        DynamicTableViewSnapshot.Field systemField = field("ID", "主键", "pri-field-view");
        systemField.getPermissions().setCreate("pri-field-create");
        systemField.getPermissions().setUpdate("pri-field-update");
        snapshot.getFields().setSystemFields(Collections.singletonList(systemField));
        snapshot.getFields().setListOrder(Collections.singletonList("ID"));

        DynamicTableViewSnapshot.Field formField = field("FORM_NOTE", "表单备注", "pri-form-view");
        formField.getPermissions().setUpdate("pri-form-update");
        snapshot.getFields().setFormFields(Collections.singletonList(formField));

        DynamicTableViewSnapshot.SectionLine line = new DynamicTableViewSnapshot.SectionLine();
        line.setDisplayName("基础信息");
        line.setPermissions(permission("pri-line-view"));
        snapshot.getFields().setSectionLines(Collections.singletonList(line));

        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName("show");
        button.setType(Integer.valueOf(1));
        button.setDisplayName("查看");
        button.setIcon("zoomin");
        button.setStyleClass("left");
        button.setPermissions(permission("pri-button-view"));
        snapshot.getButtons().setSystem(Collections.singletonList(button));

        DynamicTableViewSnapshot.Weixin weixin = new DynamicTableViewSnapshot.Weixin();
        weixin.setListMode(Integer.valueOf(0));
        weixin.setUrlMode(Integer.valueOf(0));
        weixin.setPermissions(permission("pri-weixin-view"));
        snapshot.setWeixin(weixin);

        Map<String, Object> table = new DynamicTableViewMapper().toTableMap(snapshot);

        Map<String, Object> systemFieldMap = firstMap(table.get("columns"));
        assertCompletePri((CmPri) systemFieldMap.get("pri"), "pri-field-view", "CRM_CUSTOMER_VIEW");
        assertCompletePri((CmPri) systemFieldMap.get("createPri"), "pri-field-create", "CRM_CUSTOMER_VIEW");
        assertCompletePri((CmPri) systemFieldMap.get("updatePri"), "pri-field-update", "CRM_CUSTOMER_VIEW");

        Map<String, Object> formFieldMap = firstMap(table.get("formColumns"));
        assertCompletePri((CmPri) formFieldMap.get("editPri"), "pri-form-update", "CRM_CUSTOMER_VIEW");

        Map<String, Object> sectionLineMap = firstMap(table.get("lineColumns"));
        assertCompletePri((CmPri) sectionLineMap.get("pri"), "pri-line-view", "CRM_CUSTOMER_VIEW");

        Map<String, Object> buttonMap = firstMap(table.get("sysBtns"));
        assertEquals(Integer.valueOf(1), buttonMap.get("type"));
        assertCompletePri((CmPri) buttonMap.get("pri"), "pri-button-view", "CRM_CUSTOMER_VIEW");

        Map<String, Object> weixinMap = castMap(table.get("weixin"));
        assertEquals("CRM_CUSTOMER_VIEW", weixinMap.get("viewKey"));
        assertCompletePri((CmPri) weixinMap.get("pri"), "pri-weixin-view", "CRM_CUSTOMER_VIEW");
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

    private Map<String, Object> formColumn(String name, String displayName) {
        Map<String, Object> column = new LinkedHashMap<String, Object>();
        column.put("name", name);
        column.put("busiName", displayName);
        column.put("sort", Integer.valueOf(1));
        column.put("widget", "textarea");
        column.put("whole", Integer.valueOf(1));
        column.put("contentType", Integer.valueOf(1));
        column.put("contentScript", "return vo." + name + ";");
        return column;
    }

    private Map<String, Object> sectionLine(String key, String displayName, Object pri, int sort) {
        Map<String, Object> line = new LinkedHashMap<String, Object>();
        line.put("key", key);
        line.put("busiName", displayName);
        line.put("pri", pri);
        line.put("sort", Integer.valueOf(sort));
        return line;
    }

    private Map<String, Object> priMap(String priKey) {
        Map<String, Object> pri = new LinkedHashMap<String, Object>();
        pri.put("priKey", priKey);
        return pri;
    }

    private DynamicTableViewSnapshot.Field field(String name, String displayName, String viewPriKey) {
        DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
        field.setName(name);
        field.setDisplayName(displayName);
        field.setShowInDetail(Boolean.TRUE);
        field.setShowInForm(Boolean.TRUE);
        field.setShowInList(Boolean.TRUE);
        field.setWidget("text");
        field.setPermissions(permission(viewPriKey));
        return field;
    }

    private DynamicTableViewSnapshot.PermissionSet permission(String viewPriKey) {
        DynamicTableViewSnapshot.PermissionSet permissions = new DynamicTableViewSnapshot.PermissionSet();
        permissions.setView(viewPriKey);
        return permissions;
    }

    private void assertCompletePri(CmPri pri, String priKey, String viewKey) {
        assertNotNull(pri);
        assertEquals(priKey, pri.getPriKey());
        assertEquals(Integer.valueOf(2), pri.getCatelogType());
        assertEquals(viewKey, pri.getCatelogKey());
        assertEquals(Integer.valueOf(1), pri.getType());
        assertEquals(Integer.valueOf(2), pri.getCheckType());
        assertEquals("${true}", pri.getCheckScript());
        assertNotNull(pri.getBusiName());
        assertTrue(pri.getBusiName().length() > 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstMap(Object value) {
        Collection<Map<String, Object>> collection = (Collection<Map<String, Object>>) value;
        return collection.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
