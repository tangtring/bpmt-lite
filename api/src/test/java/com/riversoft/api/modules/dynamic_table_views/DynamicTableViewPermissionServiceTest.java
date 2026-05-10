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

    @Test
    public void preservesExistingNormalQueryPermissionByStableKey() {
        DynamicTableViewSnapshot oldSnapshot = new DynamicTableViewSnapshot();
        oldSnapshot.getQueries().getNormal().add(normalQuery("customerName", "pri-old-query"));
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        target.getQueries().getNormal().add(normalQuery("customerName", null));

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-old-query", target.getQueries().getNormal().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-query"));
    }

    @Test
    public void generatesBeforeProcessorPermissionWhenAgentOmitsIt() {
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Processor processor = new DynamicTableViewSnapshot.Processor();
        processor.setKey("prepareVo");
        target.getProcessors().getBefore().add(processor);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.processor.before.prepareVo.view",
                target.getProcessors().getBefore().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.processor.before.prepareVo.view"));
    }

    @Test
    public void keepsTargetPreparedVariablePermissionBeforeOldPermission() {
        DynamicTableViewSnapshot oldSnapshot = new DynamicTableViewSnapshot();
        oldSnapshot.getVariables().getPrepared().add(preparedVariable("currentUser", "pri-old-variable"));
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        target.getVariables().getPrepared().add(preparedVariable("currentUser", "pri-target-variable"));

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-target-variable", target.getVariables().getPrepared().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-variable"));
    }

    @Test
    public void deletesOldAdvancedQueryPermissionWhenStableKeyRemoved() {
        DynamicTableViewSnapshot oldSnapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.AdvancedQuery query = new DynamicTableViewSnapshot.AdvancedQuery();
        query.setKey("overdue");
        query.getPermissions().setView("pri-old-advanced-query");
        oldSnapshot.getQueries().getAdvanced().add(query);
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertTrue(plan.getPermissionDeletes().contains("pri-old-advanced-query"));
    }

    private DynamicTableViewSnapshot snapshotWithField(String name, String viewPermission) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Field field = new DynamicTableViewSnapshot.Field();
        field.setName(name);
        field.getPermissions().setView(viewPermission);
        snapshot.getFields().getSystemFields().add(field);
        return snapshot;
    }

    private DynamicTableViewSnapshot.NormalQuery normalQuery(String key, String viewPermission) {
        DynamicTableViewSnapshot.NormalQuery query = new DynamicTableViewSnapshot.NormalQuery();
        query.setKey(key);
        query.getPermissions().setView(viewPermission);
        return query;
    }

    private DynamicTableViewSnapshot.PreparedVariable preparedVariable(String key, String viewPermission) {
        DynamicTableViewSnapshot.PreparedVariable variable = new DynamicTableViewSnapshot.PreparedVariable();
        variable.setKey(key);
        variable.getPermissions().setView(viewPermission);
        return variable;
    }
}
