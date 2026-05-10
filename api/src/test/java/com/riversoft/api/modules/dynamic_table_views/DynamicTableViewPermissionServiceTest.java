package com.riversoft.api.modules.dynamic_table_views;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void retainedPermissionIsNotDeleted() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithField("CUSTOMER_ID", "pri-shared-field-view");
        DynamicTableViewSnapshot target = snapshotWithField("CUSTOMER_ID", null);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertTrue(plan.getPermissionKeeps().contains("pri-shared-field-view"));
        assertFalse(plan.getPermissionDeletes().contains("pri-shared-field-view"));
    }

    @Test
    public void deletedPermissionsExcludeActivePermissionKeys() {
        DynamicTableViewSnapshot oldSnapshot = new DynamicTableViewSnapshot();
        oldSnapshot.getLimits().add(limit("removed", "pri-shared-limit-view"));
        oldSnapshot.getSubviews().getViewTabs().add(viewTab("kept", "pri-shared-limit-view"));
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        target.getSubviews().getViewTabs().add(viewTab("kept", null));

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertTrue(plan.getPermissionKeeps().contains("pri-shared-limit-view"));
        assertFalse(plan.getPermissionDeletes().contains("pri-shared-limit-view"));
    }

    @Test
    public void deletedPermissionsAreReturnedInStableSectionOrder() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithDeletedPermissionOrderSections();
        DynamicTableViewResponse.WritePlan firstPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, new DynamicTableViewSnapshot());
        DynamicTableViewResponse.WritePlan secondPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, new DynamicTableViewSnapshot());

        assertEquals(Arrays.asList(
                "pri-old-field-view",
                "pri-old-section-view",
                "pri-old-normal-view",
                "pri-old-advanced-view",
                "pri-old-limit-view",
                "pri-old-prepared-view",
                "pri-old-before-view",
                "pri-old-system-tab-view",
                "pri-old-item-button-view",
                "pri-old-weixin-view"),
                firstPlan.getPermissionDeletes());
        assertEquals(firstPlan.getPermissionDeletes(), secondPlan.getPermissionDeletes());
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

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.systemButton.create.2.view",
                target.getButtons().getSystem().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.systemButton.create.2.view"));
    }

    @Test
    public void createsKeepsAndDeletesSystemButtonPermissions() {
        DynamicTableViewSnapshot oldKeepTarget = snapshotWithSystemButton(null);
        DynamicTableViewResponse.WritePlan oldKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW",
                        snapshotWithSystemButton("old"), oldKeepTarget);

        assertEquals("pri-old-system-button",
                oldKeepTarget.getButtons().getSystem().get(0).getPermissions().getView());
        assertTrue(oldKeepPlan.getPermissionKeeps().contains("pri-old-system-button"));

        DynamicTableViewSnapshot target = snapshotWithSystemButton("target");
        DynamicTableViewResponse.WritePlan targetKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithSystemButton("old"), target);

        assertEquals("pri-target-system-button",
                target.getButtons().getSystem().get(0).getPermissions().getView());
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-system-button"));

        DynamicTableViewResponse.WritePlan deletePlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW",
                        snapshotWithSystemButton("old"), new DynamicTableViewSnapshot());

        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-system-button"));
    }

    @Test
    public void generatesWeixinPermission() {
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();
        target.setWeixin(new DynamicTableViewSnapshot.Weixin());

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.weixin.view", target.getWeixin().getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.weixin.view"));
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

    @Test
    public void generatesPermissionsForAllScriptAndConfigBlocksWhenAgentOmitsThem() {
        DynamicTableViewSnapshot target = snapshotWithScriptAndConfigBlocks(null);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.sectionLine.basic.view",
                target.getFields().getSectionLines().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.sectionLine.basic.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.normalQuery.customerName.view",
                target.getQueries().getNormal().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.normalQuery.customerName.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.advancedQuery.overdue.view",
                target.getQueries().getAdvanced().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.advancedQuery.overdue.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.processor.before.prepareVo.view",
                target.getProcessors().getBefore().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.processor.before.prepareVo.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.processor.after.syncLog.view",
                target.getProcessors().getAfter().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.processor.after.syncLog.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.preparedVariable.currentUser.view",
                target.getVariables().getPrepared().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.preparedVariable.currentUser.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.parentVariable.parentOrder.view",
                target.getVariables().getParents().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.parentVariable.parentOrder.view"));
    }

    @Test
    public void preservesOldPermissionsForAllScriptAndConfigBlocksByStableKey() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithScriptAndConfigBlocks("old");
        DynamicTableViewSnapshot target = snapshotWithScriptAndConfigBlocks(null);

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-old-section", target.getFields().getSectionLines().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-section"));
        assertEquals("pri-old-normal", target.getQueries().getNormal().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-normal"));
        assertEquals("pri-old-advanced", target.getQueries().getAdvanced().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-advanced"));
        assertEquals("pri-old-before", target.getProcessors().getBefore().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-before"));
        assertEquals("pri-old-after", target.getProcessors().getAfter().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-after"));
        assertEquals("pri-old-prepared", target.getVariables().getPrepared().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-prepared"));
        assertEquals("pri-old-parent", target.getVariables().getParents().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-old-parent"));
    }

    @Test
    public void keepsTargetPermissionsForAllScriptAndConfigBlocksBeforeOldPermissions() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithScriptAndConfigBlocks("old");
        DynamicTableViewSnapshot target = snapshotWithScriptAndConfigBlocks("target");

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertEquals("pri-target-section", target.getFields().getSectionLines().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-section"));
        assertEquals("pri-target-normal", target.getQueries().getNormal().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-normal"));
        assertEquals("pri-target-advanced", target.getQueries().getAdvanced().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-advanced"));
        assertEquals("pri-target-before", target.getProcessors().getBefore().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-before"));
        assertEquals("pri-target-after", target.getProcessors().getAfter().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-after"));
        assertEquals("pri-target-prepared", target.getVariables().getPrepared().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-prepared"));
        assertEquals("pri-target-parent", target.getVariables().getParents().get(0).getPermissions().getView());
        assertTrue(plan.getPermissionKeeps().contains("pri-target-parent"));
    }

    @Test
    public void deletesOldPermissionsForAllRemovedScriptAndConfigBlocks() {
        DynamicTableViewSnapshot oldSnapshot = snapshotWithScriptAndConfigBlocks("old");
        DynamicTableViewSnapshot target = new DynamicTableViewSnapshot();

        DynamicTableViewResponse.WritePlan plan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", oldSnapshot, target);

        assertTrue(plan.getPermissionDeletes().contains("pri-old-section"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-normal"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-advanced"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-before"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-after"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-prepared"));
        assertTrue(plan.getPermissionDeletes().contains("pri-old-parent"));
    }

    @Test
    public void createsKeepsAndDeletesSubviewPermissions() {
        DynamicTableViewSnapshot target = snapshotWithSubviews(null);

        DynamicTableViewResponse.WritePlan createPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.systemTab.log.view",
                target.getSubviews().getSystemTabs().get(0).getPermissions().getView());
        assertTrue(createPlan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.systemTab.log.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.viewTab.orders.view",
                target.getSubviews().getViewTabs().get(0).getPermissions().getView());
        assertTrue(createPlan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.viewTab.orders.view"));

        DynamicTableViewResponse.WritePlan keepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithSubviews("old"), snapshotWithSubviews(null));

        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-system-tab"));
        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-view-tab"));

        DynamicTableViewSnapshot targetKeep = snapshotWithSubviews("target");
        DynamicTableViewResponse.WritePlan targetKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithSubviews("old"), targetKeep);

        assertEquals("pri-target-system-tab",
                targetKeep.getSubviews().getSystemTabs().get(0).getPermissions().getView());
        assertEquals("pri-target-view-tab",
                targetKeep.getSubviews().getViewTabs().get(0).getPermissions().getView());
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-system-tab"));
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-view-tab"));

        DynamicTableViewResponse.WritePlan deletePlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithSubviews("old"), new DynamicTableViewSnapshot());

        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-system-tab"));
        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-view-tab"));
    }

    @Test
    public void createsKeepsAndDeletesCustomButtonPermissions() {
        DynamicTableViewSnapshot target = snapshotWithCustomButtons(null);

        DynamicTableViewResponse.WritePlan createPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.itemButton.approve.view",
                target.getButtons().getItem().get(0).getPermissions().getView());
        assertTrue(createPlan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.itemButton.approve.view"));
        assertEquals("dyn.CRM_CUSTOMER_VIEW.summaryButton.export.view",
                target.getButtons().getSummary().get(0).getPermissions().getView());
        assertTrue(createPlan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.summaryButton.export.view"));

        DynamicTableViewResponse.WritePlan keepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithCustomButtons("old"), snapshotWithCustomButtons(null));

        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-item-button"));
        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-summary-button"));

        DynamicTableViewSnapshot targetKeep = snapshotWithCustomButtons("target");
        DynamicTableViewResponse.WritePlan targetKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithCustomButtons("old"), targetKeep);

        assertEquals("pri-target-item-button",
                targetKeep.getButtons().getItem().get(0).getPermissions().getView());
        assertEquals("pri-target-summary-button",
                targetKeep.getButtons().getSummary().get(0).getPermissions().getView());
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-item-button"));
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-summary-button"));

        DynamicTableViewResponse.WritePlan deletePlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithCustomButtons("old"), new DynamicTableViewSnapshot());

        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-item-button"));
        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-summary-button"));
    }

    @Test
    public void createsAndKeepsLimitPermission() {
        DynamicTableViewSnapshot target = snapshotWithLimit(null);

        DynamicTableViewResponse.WritePlan createPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", null, target);

        assertEquals("dyn.CRM_CUSTOMER_VIEW.limit.own_rows.view", target.getLimits().get(0).getPermissions().getView());
        assertTrue(createPlan.getPermissionCreates().contains("dyn.CRM_CUSTOMER_VIEW.limit.own_rows.view"));

        DynamicTableViewResponse.WritePlan keepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithLimit("old"), snapshotWithLimit(null));

        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-limit"));

        DynamicTableViewSnapshot targetKeep = snapshotWithLimit("target");
        DynamicTableViewResponse.WritePlan targetKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithLimit("old"), targetKeep);

        assertEquals("pri-target-limit", targetKeep.getLimits().get(0).getPermissions().getView());
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-limit"));
    }

    @Test
    public void keepsAndDeletesWeixinPermission() {
        DynamicTableViewResponse.WritePlan keepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithWeixin("old"), snapshotWithWeixin(null));

        assertTrue(keepPlan.getPermissionKeeps().contains("pri-old-weixin"));

        DynamicTableViewSnapshot targetKeep = snapshotWithWeixin("target");
        DynamicTableViewResponse.WritePlan targetKeepPlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithWeixin("old"), targetKeep);

        assertEquals("pri-target-weixin", targetKeep.getWeixin().getPermissions().getView());
        assertTrue(targetKeepPlan.getPermissionKeeps().contains("pri-target-weixin"));

        DynamicTableViewResponse.WritePlan deletePlan =
                new DynamicTableViewPermissionService().apply("CRM_CUSTOMER_VIEW", snapshotWithWeixin("old"), new DynamicTableViewSnapshot());

        assertTrue(deletePlan.getPermissionDeletes().contains("pri-old-weixin"));
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

    private DynamicTableViewSnapshot.AdvancedQuery advancedQuery(String key, String viewPermission) {
        DynamicTableViewSnapshot.AdvancedQuery query = new DynamicTableViewSnapshot.AdvancedQuery();
        query.setKey(key);
        query.getPermissions().setView(viewPermission);
        return query;
    }

    private DynamicTableViewSnapshot.Limit limit(String key, String viewPermission) {
        DynamicTableViewSnapshot.Limit limit = new DynamicTableViewSnapshot.Limit();
        limit.setKey(key);
        limit.getPermissions().setView(viewPermission);
        return limit;
    }

    private DynamicTableViewSnapshot.ViewTab viewTab(String key, String viewPermission) {
        DynamicTableViewSnapshot.ViewTab tab = new DynamicTableViewSnapshot.ViewTab();
        tab.setKey(key);
        tab.getPermissions().setView(viewPermission);
        return tab;
    }

    private DynamicTableViewSnapshot.PreparedVariable preparedVariable(String key, String viewPermission) {
        DynamicTableViewSnapshot.PreparedVariable variable = new DynamicTableViewSnapshot.PreparedVariable();
        variable.setKey(key);
        variable.getPermissions().setView(viewPermission);
        return variable;
    }

    private DynamicTableViewSnapshot snapshotWithScriptAndConfigBlocks(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.SectionLine sectionLine = new DynamicTableViewSnapshot.SectionLine();
        sectionLine.setKey("basic");
        sectionLine.getPermissions().setView(permission("section", permissionSuffix));
        snapshot.getFields().getSectionLines().add(sectionLine);
        snapshot.getQueries().getNormal().add(normalQuery("customerName", permission("normal", permissionSuffix)));
        DynamicTableViewSnapshot.AdvancedQuery advancedQuery = new DynamicTableViewSnapshot.AdvancedQuery();
        advancedQuery.setKey("overdue");
        advancedQuery.getPermissions().setView(permission("advanced", permissionSuffix));
        snapshot.getQueries().getAdvanced().add(advancedQuery);
        DynamicTableViewSnapshot.Processor before = new DynamicTableViewSnapshot.Processor();
        before.setKey("prepareVo");
        before.getPermissions().setView(permission("before", permissionSuffix));
        snapshot.getProcessors().getBefore().add(before);
        DynamicTableViewSnapshot.Processor after = new DynamicTableViewSnapshot.Processor();
        after.setKey("syncLog");
        after.getPermissions().setView(permission("after", permissionSuffix));
        snapshot.getProcessors().getAfter().add(after);
        snapshot.getVariables().getPrepared().add(preparedVariable("currentUser", permission("prepared", permissionSuffix)));
        DynamicTableViewSnapshot.ParentVariable parent = new DynamicTableViewSnapshot.ParentVariable();
        parent.setKey("parentOrder");
        parent.getPermissions().setView(permission("parent", permissionSuffix));
        snapshot.getVariables().getParents().add(parent);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithDeletedPermissionOrderSections() {
        DynamicTableViewSnapshot snapshot = snapshotWithField("CUSTOMER_ID", "pri-old-field-view");
        DynamicTableViewSnapshot.SectionLine sectionLine = new DynamicTableViewSnapshot.SectionLine();
        sectionLine.setKey("basic");
        sectionLine.getPermissions().setView("pri-old-section-view");
        snapshot.getFields().getSectionLines().add(sectionLine);
        snapshot.getQueries().getNormal().add(normalQuery("customerName", "pri-old-normal-view"));
        snapshot.getQueries().getAdvanced().add(advancedQuery("overdue", "pri-old-advanced-view"));
        snapshot.getLimits().add(limit("own_rows", "pri-old-limit-view"));
        snapshot.getVariables().getPrepared().add(preparedVariable("currentUser", "pri-old-prepared-view"));
        DynamicTableViewSnapshot.Processor before = new DynamicTableViewSnapshot.Processor();
        before.setKey("prepareVo");
        before.getPermissions().setView("pri-old-before-view");
        snapshot.getProcessors().getBefore().add(before);
        DynamicTableViewSnapshot.SystemTab systemTab = new DynamicTableViewSnapshot.SystemTab();
        systemTab.setName("log");
        systemTab.getPermissions().setView("pri-old-system-tab-view");
        snapshot.getSubviews().getSystemTabs().add(systemTab);
        DynamicTableViewSnapshot.CustomButton itemButton = new DynamicTableViewSnapshot.CustomButton();
        itemButton.setKey("approve");
        itemButton.getPermissions().setView("pri-old-item-button-view");
        snapshot.getButtons().getItem().add(itemButton);
        DynamicTableViewSnapshot.Weixin weixin = new DynamicTableViewSnapshot.Weixin();
        weixin.getPermissions().setView("pri-old-weixin-view");
        snapshot.setWeixin(weixin);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithSubviews(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.SystemTab systemTab = new DynamicTableViewSnapshot.SystemTab();
        systemTab.setName("log");
        systemTab.getPermissions().setView(permission("system-tab", permissionSuffix));
        snapshot.getSubviews().getSystemTabs().add(systemTab);
        DynamicTableViewSnapshot.ViewTab viewTab = new DynamicTableViewSnapshot.ViewTab();
        viewTab.setKey("orders");
        viewTab.getPermissions().setView(permission("view-tab", permissionSuffix));
        snapshot.getSubviews().getViewTabs().add(viewTab);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithCustomButtons(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.CustomButton itemButton = new DynamicTableViewSnapshot.CustomButton();
        itemButton.setKey("approve");
        itemButton.getPermissions().setView(permission("item-button", permissionSuffix));
        snapshot.getButtons().getItem().add(itemButton);
        DynamicTableViewSnapshot.CustomButton summaryButton = new DynamicTableViewSnapshot.CustomButton();
        summaryButton.setKey("export");
        summaryButton.getPermissions().setView(permission("summary-button", permissionSuffix));
        snapshot.getButtons().getSummary().add(summaryButton);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithSystemButton(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName("create");
        button.setType(Integer.valueOf(2));
        button.getPermissions().setView(permission("system-button", permissionSuffix));
        snapshot.getButtons().getSystem().add(button);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithLimit(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Limit limit = new DynamicTableViewSnapshot.Limit();
        limit.setKey("own_rows");
        limit.getPermissions().setView(permission("limit", permissionSuffix));
        snapshot.getLimits().add(limit);
        return snapshot;
    }

    private DynamicTableViewSnapshot snapshotWithWeixin(String permissionSuffix) {
        DynamicTableViewSnapshot snapshot = new DynamicTableViewSnapshot();
        DynamicTableViewSnapshot.Weixin weixin = new DynamicTableViewSnapshot.Weixin();
        weixin.getPermissions().setView(permission("weixin", permissionSuffix));
        snapshot.setWeixin(weixin);
        return snapshot;
    }

    private String permission(String name, String suffix) {
        if (suffix == null) {
            return null;
        }
        return "pri-" + suffix + "-" + name;
    }
}
