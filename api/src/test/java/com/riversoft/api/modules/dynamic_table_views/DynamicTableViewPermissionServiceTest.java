package com.riversoft.api.modules.dynamic_table_views;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicTableViewPermissionServiceTest {
    @Test
    public void preservesExistingFieldPermissionByStableName() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithField("CUSTOMER_ID", "pri-old-view");
        DynamicTableViewSnapshot target = snapshotWithField("CUSTOMER_ID", null);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-old-view", target.getFields().getSystemFields().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-view"));
    }

    @Test
    public void generatesPermissionWhenAgentOmitsIt() {
        DynamicTableViewSnapshot target = snapshotWithField("CUSTOMER_ID", null);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.view",
                target.getFields().getSystemFields().get(0).getPermissions().getView());
        assertEquals("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.create",
                target.getFields().getSystemFields().get(0).getPermissions().getCreate());
        assertEquals("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.update",
                target.getFields().getSystemFields().get(0).getPermissions().getUpdate());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.view"));
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.create"));
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.update"));
    }

    @Test
    public void keepsTargetPermissionBeforeOldPermission() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithField("CUSTOMER_ID", "pri-old-view");
        DynamicTableViewSnapshot target = snapshotWithField("CUSTOMER_ID", "pri-target-view");

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-target-view", target.getFields().getSystemFields().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-view"));
    }

    @Test
    public void deletesOldLimitPermissionWhenStableKeyRemoved() {
        DynamicTableViewSnapshot oldSnapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Limit limit = new DynamicTableViewSnapshot.Limit();
        limit.setKey("own_rows");
        limit.getPermissions().setView("pri-old-limit");
        oldSnapshot.getLimits().add(limit);
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertTrue(plan.getPermissionDeletes().contains("pri-old-limit"));
    }

    @Test
    public void generatesSystemButtonPermissionWithIntegerType() {
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName("create");
        button.setType(Integer.valueOf(2));
        target.getButtons().getSystem().add(button);

        new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.systemButton.create.2.view",
                target.getButtons().getSystem().get(0).getPermissions().getView());
    }

    @Test
    public void generatesWeixinPermission() {
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        target.setWeixin(new DynamicTableViewSnapshot.Weixin());

        new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.weixin.view", target.getWeixin().getPermissions().getView());
    }

    private DynamicTableViewSnapshot snapshotWithField(String name, String viewPermission) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
        field.setName(name);
        field.getPermissions().setView(viewPermission);
        snapshot.getFields().getSystemFields().add(field);
        return snapshot;
    }
}
