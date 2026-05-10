package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiJson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DynamicTableViewSnapshotTest {
    @Test
    public void parsesAgentFacingSnapshot() {
        String json = "{\"viewKey\":\"CRM_CUSTOMER_VIEW\",\"description\":\"客户资料维护视图\",\"loginRequired\":true,"
                + "\"base\":{\"tableName\":\"CRM_CUSTOMER\",\"displayName\":\"客户资料\",\"layoutColumns\":2,"
                + "\"initQuery\":true,\"pageLimit\":20,\"defaultSort\":{\"field\":\"CREATE_DATE\",\"direction\":\"desc\"}},"
                + "\"fields\":{\"systemFields\":[{\"name\":\"CUSTOMER_ID\",\"displayName\":\"客户ID\",\"showInDetail\":true,"
                + "\"showInForm\":true,\"showInList\":true,\"widget\":\"text{required:true}\",\"content\":{\"type\":1,"
                + "\"script\":\"return vo.CUSTOMER_ID;\"},\"permissions\":{\"view\":null,\"create\":null,\"update\":null}}],"
                + "\"computedFields\":[],\"formFields\":[],\"sectionLines\":[],\"listOrder\":[\"CUSTOMER_ID\"]},"
                + "\"queries\":{\"normal\":[],\"advanced\":[]},\"limits\":[],\"variables\":{\"prepared\":[],\"parents\":[]},"
                + "\"processors\":{\"before\":[],\"after\":[]},\"subviews\":{\"systemTabs\":[],\"viewTabs\":[]},"
                + "\"buttons\":{\"system\":[],\"item\":[],\"summary\":[]},\"weixin\":null,\"scripts\":{\"list\":null,\"form\":null}}";

        DynamicTableViewSnapshot snapshot = ApiJson.fromJson(new java.io.ByteArrayInputStream(json.getBytes()), DynamicTableViewSnapshot.class);

        assertEquals("CRM_CUSTOMER_VIEW", snapshot.getViewKey());
        assertTrue(snapshot.isLoginRequired());
        assertEquals("CRM_CUSTOMER", snapshot.getBase().getTableName());
        assertEquals("CUSTOMER_ID", snapshot.getFields().getSystemFields().get(0).getName());
    }

    @Test
    public void keepsDefaultSortWhenBaseIsEmpty() {
        String json = "{\"base\":{}}";

        DynamicTableViewSnapshot snapshot = ApiJson.fromJson(new java.io.ByteArrayInputStream(json.getBytes()), DynamicTableViewSnapshot.class);

        assertNotNull(snapshot.getBase().getDefaultSort());
    }

    @Test
    public void initializesPermissionsForNonFieldConfigBlocks() {
        String json = "{\"fields\":{\"sectionLines\":[{\"key\":\"basic\"}]},"
                + "\"queries\":{\"normal\":[{\"key\":\"customerName\"}],\"advanced\":[{\"key\":\"overdue\"}]},"
                + "\"variables\":{\"prepared\":[{\"key\":\"currentUser\"}],\"parents\":[{\"key\":\"parentOrder\"}]},"
                + "\"processors\":{\"before\":[{\"key\":\"prepareVo\"}],\"after\":[{\"key\":\"syncLog\"}]}}";

        DynamicTableViewSnapshot snapshot = ApiJson.fromJson(new java.io.ByteArrayInputStream(json.getBytes()), DynamicTableViewSnapshot.class);

        assertNotNull(snapshot.getFields().getSectionLines().get(0).getPermissions());
        assertNotNull(snapshot.getQueries().getNormal().get(0).getPermissions());
        assertNotNull(snapshot.getQueries().getAdvanced().get(0).getPermissions());
        assertNotNull(snapshot.getVariables().getPrepared().get(0).getPermissions());
        assertNotNull(snapshot.getVariables().getParents().get(0).getPermissions());
        assertNotNull(snapshot.getProcessors().getBefore().get(0).getPermissions());
        assertNotNull(snapshot.getProcessors().getAfter().get(0).getPermissions());
    }

    @Test
    public void sectionParserAcceptsOnlyConfiguredBlocks() {
        assertEquals(DynamicTableViewSection.FIELDS, DynamicTableViewSection.parse("fields"));
        assertEquals(DynamicTableViewSection.FIELDS, DynamicTableViewSection.parse(" fields "));
        assertEquals(DynamicTableViewSection.SCRIPTS, DynamicTableViewSection.parse("scripts"));
    }

    @Test(expected = com.riversoft.api.http.ApiException.class)
    public void sectionParserRejectsUnknownBlocks() {
        DynamicTableViewSection.parse("menu");
    }
}
