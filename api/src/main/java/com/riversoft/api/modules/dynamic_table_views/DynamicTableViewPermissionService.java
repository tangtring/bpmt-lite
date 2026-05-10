package com.riversoft.api.modules.dynamic_table_views;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicTableViewPermissionService {
    public DynamicTableViewResponse.WritePlan apply(String viewKey,
                                                    DynamicTableViewSnapshot oldSnapshot,
                                                    DynamicTableViewSnapshot target) {
        DynamicTableViewResponse.WritePlan plan = new DynamicTableViewResponse.WritePlan();
        apply(viewKey, oldSnapshot, target, plan);
        return plan;
    }

    public DynamicTableViewResponse.WritePlan apply(String viewKey,
                                                    DynamicTableViewSnapshot oldSnapshot,
                                                    DynamicTableViewSnapshot target,
                                                    DynamicTableViewResponse.WritePlan plan) {
        DynamicTableViewResponse.WritePlan effectivePlan =
                plan == null ? new DynamicTableViewResponse.WritePlan() : plan;
        Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions =
                new LinkedHashMap<String, DynamicTableViewSnapshot.PermissionSet>();
        collectPermissionSets(oldSnapshot, oldPermissions);

        Set<String> targetStableKeys = new LinkedHashSet<String>();
        applyTargetPermissions(viewKey, target, oldPermissions, targetStableKeys, effectivePlan);
        collectStableKeysWithoutPermissions(target, targetStableKeys);
        collectDeletedPermissions(oldSnapshot, targetStableKeys, effectivePlan);
        return effectivePlan;
    }

    private void applyTargetPermissions(String viewKey,
                                        DynamicTableViewSnapshot snapshot,
                                        Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                        Set<String> targetStableKeys,
                                        DynamicTableViewResponse.WritePlan plan) {
        if (snapshot == null) {
            return;
        }
        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        if (fields != null) {
            applyFieldPermissions(viewKey, fields.getSystemFields(), "systemField", oldPermissions, targetStableKeys, plan);
            applyFieldPermissions(viewKey, fields.getComputedFields(), "computedField", oldPermissions, targetStableKeys, plan);
            applyFieldPermissions(viewKey, fields.getFormFields(), "formField", oldPermissions, targetStableKeys, plan);
            applySectionLinePermissions(viewKey, fields.getSectionLines(), oldPermissions, targetStableKeys, plan);
        }
        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        if (queries != null) {
            applyNormalQueryPermissions(viewKey, queries.getNormal(), oldPermissions, targetStableKeys, plan);
            applyAdvancedQueryPermissions(viewKey, queries.getAdvanced(), oldPermissions, targetStableKeys, plan);
        }
        applyLimitPermissions(viewKey, snapshot.getLimits(), oldPermissions, targetStableKeys, plan);
        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        if (variables != null) {
            applyPreparedVariablePermissions(viewKey, variables.getPrepared(), oldPermissions, targetStableKeys, plan);
            applyParentVariablePermissions(viewKey, variables.getParents(), oldPermissions, targetStableKeys, plan);
        }
        DynamicTableViewSnapshot.Processors processors = snapshot.getProcessors();
        if (processors != null) {
            applyProcessorPermissions(viewKey, processors.getBefore(), "before", oldPermissions, targetStableKeys, plan);
            applyProcessorPermissions(viewKey, processors.getAfter(), "after", oldPermissions, targetStableKeys, plan);
        }
        DynamicTableViewSnapshot.Subviews subviews = snapshot.getSubviews();
        if (subviews != null) {
            applySystemTabPermissions(viewKey, subviews.getSystemTabs(), oldPermissions, targetStableKeys, plan);
            applyViewTabPermissions(viewKey, subviews.getViewTabs(), oldPermissions, targetStableKeys, plan);
        }
        DynamicTableViewSnapshot.Buttons buttons = snapshot.getButtons();
        if (buttons != null) {
            applySystemButtonPermissions(viewKey, buttons.getSystem(), oldPermissions, targetStableKeys, plan);
            applyCustomButtonPermissions(viewKey, buttons.getItem(), "itemButton", oldPermissions, targetStableKeys, plan);
            applyCustomButtonPermissions(viewKey, buttons.getSummary(), "summaryButton", oldPermissions, targetStableKeys, plan);
        }
        DynamicTableViewSnapshot.Weixin weixin = snapshot.getWeixin();
        if (weixin != null) {
            String stableKey = "weixin";
            targetStableKeys.add(stableKey);
            if (weixin.getPermissions() == null) {
                weixin.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(weixin.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".weixin.view", plan);
        }
    }

    private void applyFieldPermissions(String viewKey,
                                       List<DynamicTableViewSnapshot.Field> fields,
                                       String stablePrefix,
                                       Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                       Set<String> targetStableKeys,
                                       DynamicTableViewResponse.WritePlan plan) {
        if (fields == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Field field : fields) {
            if (field == null) {
                continue;
            }
            String stableKey = stablePrefix + ":" + stableFieldId(field, stablePrefix);
            if (!hasText(stableFieldId(field, stablePrefix))) {
                continue;
            }
            targetStableKeys.add(stableKey);
            if (field.getPermissions() == null) {
                field.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            String permissionId = permissionFieldId(field);
            if (!hasText(permissionId)) {
                continue;
            }
            DynamicTableViewSnapshot.PermissionSet old = oldPermissions.get(stableKey);
            applyPermission(field.getPermissions(), old, "view", "dyn." + viewKey + ".field." + permissionId + ".view", plan);
            applyPermission(field.getPermissions(), old, "create", "dyn." + viewKey + ".field." + permissionId + ".create", plan);
            applyPermission(field.getPermissions(), old, "update", "dyn." + viewKey + ".field." + permissionId + ".update", plan);
        }
    }

    private void applySectionLinePermissions(String viewKey,
                                             List<DynamicTableViewSnapshot.SectionLine> lines,
                                             Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                             Set<String> targetStableKeys,
                                             DynamicTableViewResponse.WritePlan plan) {
        if (lines == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SectionLine line : lines) {
            if (line == null || !hasText(line.getKey())) {
                continue;
            }
            String stableKey = "sectionLine:" + line.getKey();
            targetStableKeys.add(stableKey);
            if (line.getPermissions() == null) {
                line.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(line.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".sectionLine." + line.getKey() + ".view", plan);
        }
    }

    private void applyNormalQueryPermissions(String viewKey,
                                             List<DynamicTableViewSnapshot.NormalQuery> queries,
                                             Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                             Set<String> targetStableKeys,
                                             DynamicTableViewResponse.WritePlan plan) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.NormalQuery query : queries) {
            if (query == null || !hasText(query.getKey())) {
                continue;
            }
            String stableKey = "normalQuery:" + query.getKey();
            targetStableKeys.add(stableKey);
            if (query.getPermissions() == null) {
                query.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(query.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".normalQuery." + query.getKey() + ".view", plan);
        }
    }

    private void applyAdvancedQueryPermissions(String viewKey,
                                               List<DynamicTableViewSnapshot.AdvancedQuery> queries,
                                               Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                               Set<String> targetStableKeys,
                                               DynamicTableViewResponse.WritePlan plan) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.AdvancedQuery query : queries) {
            if (query == null || !hasText(query.getKey())) {
                continue;
            }
            String stableKey = "advancedQuery:" + query.getKey();
            targetStableKeys.add(stableKey);
            if (query.getPermissions() == null) {
                query.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(query.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".advancedQuery." + query.getKey() + ".view", plan);
        }
    }

    private void applyLimitPermissions(String viewKey,
                                       List<DynamicTableViewSnapshot.Limit> limits,
                                       Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                       Set<String> targetStableKeys,
                                       DynamicTableViewResponse.WritePlan plan) {
        if (limits == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Limit limit : limits) {
            if (limit == null || !hasText(limit.getKey())) {
                continue;
            }
            String stableKey = "limit:" + limit.getKey();
            targetStableKeys.add(stableKey);
            if (limit.getPermissions() == null) {
                limit.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(limit.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".limit." + limit.getKey() + ".view", plan);
        }
    }

    private void applyPreparedVariablePermissions(String viewKey,
                                                  List<DynamicTableViewSnapshot.PreparedVariable> variables,
                                                  Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                                  Set<String> targetStableKeys,
                                                  DynamicTableViewResponse.WritePlan plan) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.PreparedVariable variable : variables) {
            if (variable == null || !hasText(variable.getKey())) {
                continue;
            }
            String stableKey = "preparedVariable:" + variable.getKey();
            targetStableKeys.add(stableKey);
            if (variable.getPermissions() == null) {
                variable.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(variable.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".preparedVariable." + variable.getKey() + ".view", plan);
        }
    }

    private void applyParentVariablePermissions(String viewKey,
                                                List<DynamicTableViewSnapshot.ParentVariable> variables,
                                                Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                                Set<String> targetStableKeys,
                                                DynamicTableViewResponse.WritePlan plan) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.ParentVariable variable : variables) {
            if (variable == null || !hasText(variable.getKey())) {
                continue;
            }
            String stableKey = "parentVariable:" + variable.getKey();
            targetStableKeys.add(stableKey);
            if (variable.getPermissions() == null) {
                variable.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(variable.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".parentVariable." + variable.getKey() + ".view", plan);
        }
    }

    private void applyProcessorPermissions(String viewKey,
                                           List<DynamicTableViewSnapshot.Processor> processors,
                                           String phase,
                                           Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                           Set<String> targetStableKeys,
                                           DynamicTableViewResponse.WritePlan plan) {
        if (processors == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Processor processor : processors) {
            if (processor == null || !hasText(processor.getKey())) {
                continue;
            }
            String stableKey = "processor:" + phase + ":" + processor.getKey();
            targetStableKeys.add(stableKey);
            if (processor.getPermissions() == null) {
                processor.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(processor.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".processor." + phase + "." + processor.getKey() + ".view", plan);
        }
    }

    private void applySystemTabPermissions(String viewKey,
                                           List<DynamicTableViewSnapshot.SystemTab> tabs,
                                           Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                           Set<String> targetStableKeys,
                                           DynamicTableViewResponse.WritePlan plan) {
        if (tabs == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SystemTab tab : tabs) {
            if (tab == null || !hasText(tab.getName())) {
                continue;
            }
            String stableKey = "systemTab:" + tab.getName();
            targetStableKeys.add(stableKey);
            if (tab.getPermissions() == null) {
                tab.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(tab.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".systemTab." + tab.getName() + ".view", plan);
        }
    }

    private void applyViewTabPermissions(String viewKey,
                                         List<DynamicTableViewSnapshot.ViewTab> tabs,
                                         Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                         Set<String> targetStableKeys,
                                         DynamicTableViewResponse.WritePlan plan) {
        if (tabs == null) {
            return;
        }
        for (DynamicTableViewSnapshot.ViewTab tab : tabs) {
            if (tab == null || !hasText(tab.getKey())) {
                continue;
            }
            String stableKey = "viewTab:" + tab.getKey();
            targetStableKeys.add(stableKey);
            if (tab.getPermissions() == null) {
                tab.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(tab.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".viewTab." + tab.getKey() + ".view", plan);
        }
    }

    private void applySystemButtonPermissions(String viewKey,
                                              List<DynamicTableViewSnapshot.SystemButton> buttons,
                                              Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                              Set<String> targetStableKeys,
                                              DynamicTableViewResponse.WritePlan plan) {
        if (buttons == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SystemButton button : buttons) {
            if (button == null || !hasText(button.getName()) || button.getType() == null) {
                continue;
            }
            String stableKey = "systemButton:" + button.getName() + ":" + button.getType();
            targetStableKeys.add(stableKey);
            if (button.getPermissions() == null) {
                button.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(button.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + ".systemButton." + button.getName() + "." + button.getType() + ".view", plan);
        }
    }

    private void applyCustomButtonPermissions(String viewKey,
                                              List<DynamicTableViewSnapshot.CustomButton> buttons,
                                              String stablePrefix,
                                              Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions,
                                              Set<String> targetStableKeys,
                                              DynamicTableViewResponse.WritePlan plan) {
        if (buttons == null) {
            return;
        }
        for (DynamicTableViewSnapshot.CustomButton button : buttons) {
            if (button == null || !hasText(button.getKey())) {
                continue;
            }
            String stableKey = stablePrefix + ":" + button.getKey();
            targetStableKeys.add(stableKey);
            if (button.getPermissions() == null) {
                button.setPermissions(new DynamicTableViewSnapshot.PermissionSet());
            }
            applyViewPermission(button.getPermissions(), oldPermissions.get(stableKey),
                    "dyn." + viewKey + "." + stablePrefix + "." + button.getKey() + ".view", plan);
        }
    }

    private void applyViewPermission(DynamicTableViewSnapshot.PermissionSet target,
                                     DynamicTableViewSnapshot.PermissionSet old,
                                     String generated,
                                     DynamicTableViewResponse.WritePlan plan) {
        applyPermission(target, old, "view", generated, plan);
    }

    private void applyPermission(DynamicTableViewSnapshot.PermissionSet target,
                                 DynamicTableViewSnapshot.PermissionSet old,
                                 String property,
                                 String generated,
                                 DynamicTableViewResponse.WritePlan plan) {
        String existing = getPermission(target, property);
        if (hasText(existing)) {
            add(plan.getPermissionKeeps(), existing);
            return;
        }
        String oldValue = old == null ? null : getPermission(old, property);
        if (hasText(oldValue)) {
            setPermission(target, property, oldValue);
            add(plan.getPermissionKeeps(), oldValue);
            return;
        }
        setPermission(target, property, generated);
        add(plan.getPermissionCreates(), generated);
    }

    private void collectPermissionSets(DynamicTableViewSnapshot snapshot,
                                       Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (snapshot == null) {
            return;
        }
        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        if (fields != null) {
            collectFieldPermissions(fields.getSystemFields(), "systemField", permissions);
            collectFieldPermissions(fields.getComputedFields(), "computedField", permissions);
            collectFieldPermissions(fields.getFormFields(), "formField", permissions);
            collectSectionLinePermissions(fields.getSectionLines(), permissions);
        }
        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        if (queries != null) {
            collectNormalQueryPermissions(queries.getNormal(), permissions);
            collectAdvancedQueryPermissions(queries.getAdvanced(), permissions);
        }
        collectLimitPermissions(snapshot.getLimits(), permissions);
        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        if (variables != null) {
            collectPreparedVariablePermissions(variables.getPrepared(), permissions);
            collectParentVariablePermissions(variables.getParents(), permissions);
        }
        DynamicTableViewSnapshot.Processors processors = snapshot.getProcessors();
        if (processors != null) {
            collectProcessorPermissions(processors.getBefore(), "before", permissions);
            collectProcessorPermissions(processors.getAfter(), "after", permissions);
        }
        DynamicTableViewSnapshot.Subviews subviews = snapshot.getSubviews();
        if (subviews != null) {
            collectSystemTabPermissions(subviews.getSystemTabs(), permissions);
            collectViewTabPermissions(subviews.getViewTabs(), permissions);
        }
        DynamicTableViewSnapshot.Buttons buttons = snapshot.getButtons();
        if (buttons != null) {
            collectSystemButtonPermissions(buttons.getSystem(), permissions);
            collectCustomButtonPermissions(buttons.getItem(), "itemButton", permissions);
            collectCustomButtonPermissions(buttons.getSummary(), "summaryButton", permissions);
        }
        if (snapshot.getWeixin() != null && snapshot.getWeixin().getPermissions() != null) {
            permissions.put("weixin", snapshot.getWeixin().getPermissions());
        }
    }

    private void collectFieldPermissions(List<DynamicTableViewSnapshot.Field> fields,
                                         String stablePrefix,
                                         Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (fields == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Field field : fields) {
            String id = stableFieldId(field, stablePrefix);
            if (hasText(id) && field.getPermissions() != null) {
                permissions.put(stablePrefix + ":" + id, field.getPermissions());
            }
        }
    }

    private void collectSectionLinePermissions(List<DynamicTableViewSnapshot.SectionLine> lines,
                                               Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (lines == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SectionLine line : lines) {
            if (line != null && hasText(line.getKey()) && line.getPermissions() != null) {
                permissions.put("sectionLine:" + line.getKey(), line.getPermissions());
            }
        }
    }

    private void collectNormalQueryPermissions(List<DynamicTableViewSnapshot.NormalQuery> queries,
                                               Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.NormalQuery query : queries) {
            if (query != null && hasText(query.getKey()) && query.getPermissions() != null) {
                permissions.put("normalQuery:" + query.getKey(), query.getPermissions());
            }
        }
    }

    private void collectAdvancedQueryPermissions(List<DynamicTableViewSnapshot.AdvancedQuery> queries,
                                                 Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.AdvancedQuery query : queries) {
            if (query != null && hasText(query.getKey()) && query.getPermissions() != null) {
                permissions.put("advancedQuery:" + query.getKey(), query.getPermissions());
            }
        }
    }

    private void collectLimitPermissions(List<DynamicTableViewSnapshot.Limit> limits,
                                         Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (limits == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Limit limit : limits) {
            if (limit != null && hasText(limit.getKey()) && limit.getPermissions() != null) {
                permissions.put("limit:" + limit.getKey(), limit.getPermissions());
            }
        }
    }

    private void collectSystemTabPermissions(List<DynamicTableViewSnapshot.SystemTab> tabs,
                                             Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (tabs == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SystemTab tab : tabs) {
            if (tab != null && hasText(tab.getName()) && tab.getPermissions() != null) {
                permissions.put("systemTab:" + tab.getName(), tab.getPermissions());
            }
        }
    }

    private void collectViewTabPermissions(List<DynamicTableViewSnapshot.ViewTab> tabs,
                                           Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (tabs == null) {
            return;
        }
        for (DynamicTableViewSnapshot.ViewTab tab : tabs) {
            if (tab != null && hasText(tab.getKey()) && tab.getPermissions() != null) {
                permissions.put("viewTab:" + tab.getKey(), tab.getPermissions());
            }
        }
    }

    private void collectSystemButtonPermissions(List<DynamicTableViewSnapshot.SystemButton> buttons,
                                                Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (buttons == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SystemButton button : buttons) {
            if (button != null && hasText(button.getName()) && button.getType() != null && button.getPermissions() != null) {
                permissions.put("systemButton:" + button.getName() + ":" + button.getType(), button.getPermissions());
            }
        }
    }

    private void collectCustomButtonPermissions(List<DynamicTableViewSnapshot.CustomButton> buttons,
                                                String stablePrefix,
                                                Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (buttons == null) {
            return;
        }
        for (DynamicTableViewSnapshot.CustomButton button : buttons) {
            if (button != null && hasText(button.getKey()) && button.getPermissions() != null) {
                permissions.put(stablePrefix + ":" + button.getKey(), button.getPermissions());
            }
        }
    }

    private void collectPreparedVariablePermissions(List<DynamicTableViewSnapshot.PreparedVariable> variables,
                                                    Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.PreparedVariable variable : variables) {
            if (variable != null && hasText(variable.getKey()) && variable.getPermissions() != null) {
                permissions.put("preparedVariable:" + variable.getKey(), variable.getPermissions());
            }
        }
    }

    private void collectParentVariablePermissions(List<DynamicTableViewSnapshot.ParentVariable> variables,
                                                  Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.ParentVariable variable : variables) {
            if (variable != null && hasText(variable.getKey()) && variable.getPermissions() != null) {
                permissions.put("parentVariable:" + variable.getKey(), variable.getPermissions());
            }
        }
    }

    private void collectProcessorPermissions(List<DynamicTableViewSnapshot.Processor> processors,
                                             String phase,
                                             Map<String, DynamicTableViewSnapshot.PermissionSet> permissions) {
        if (processors == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Processor processor : processors) {
            if (processor != null && hasText(processor.getKey()) && processor.getPermissions() != null) {
                permissions.put("processor:" + phase + ":" + processor.getKey(), processor.getPermissions());
            }
        }
    }

    private void collectStableKeysWithoutPermissions(DynamicTableViewSnapshot snapshot, Set<String> stableKeys) {
        if (snapshot == null) {
            return;
        }
        DynamicTableViewSnapshot.Fields fields = snapshot.getFields();
        if (fields != null) {
            collectSectionLineStableKeys(fields.getSectionLines(), stableKeys);
        }
        DynamicTableViewSnapshot.Queries queries = snapshot.getQueries();
        if (queries != null) {
            collectNormalQueryStableKeys(queries.getNormal(), stableKeys);
            collectAdvancedQueryStableKeys(queries.getAdvanced(), stableKeys);
        }
        DynamicTableViewSnapshot.Processors processors = snapshot.getProcessors();
        if (processors != null) {
            collectProcessorStableKeys(processors.getBefore(), "before", stableKeys);
            collectProcessorStableKeys(processors.getAfter(), "after", stableKeys);
        }
        DynamicTableViewSnapshot.Variables variables = snapshot.getVariables();
        if (variables != null) {
            collectPreparedVariableStableKeys(variables.getPrepared(), stableKeys);
            collectParentVariableStableKeys(variables.getParents(), stableKeys);
        }
    }

    private void collectDeletedPermissions(DynamicTableViewSnapshot oldSnapshot,
                                           Set<String> targetStableKeys,
                                           DynamicTableViewResponse.WritePlan plan) {
        Map<String, DynamicTableViewSnapshot.PermissionSet> oldPermissions =
                new LinkedHashMap<String, DynamicTableViewSnapshot.PermissionSet>();
        collectPermissionSets(oldSnapshot, oldPermissions);
        Set<String> activePermissions = new LinkedHashSet<String>();
        activePermissions.addAll(plan.getPermissionCreates());
        activePermissions.addAll(plan.getPermissionKeeps());
        for (Map.Entry<String, DynamicTableViewSnapshot.PermissionSet> entry : oldPermissions.entrySet()) {
            if (!targetStableKeys.contains(entry.getKey())) {
                addPermissionDeletes(entry.getValue(), activePermissions, plan);
            }
        }
    }

    private void addPermissionDeletes(DynamicTableViewSnapshot.PermissionSet permissions,
                                      Set<String> activePermissions,
                                      DynamicTableViewResponse.WritePlan plan) {
        if (permissions == null) {
            return;
        }
        addDeleteIfInactive(plan.getPermissionDeletes(), activePermissions, permissions.getView());
        addDeleteIfInactive(plan.getPermissionDeletes(), activePermissions, permissions.getCreate());
        addDeleteIfInactive(plan.getPermissionDeletes(), activePermissions, permissions.getUpdate());
    }

    private void collectSectionLineStableKeys(List<DynamicTableViewSnapshot.SectionLine> lines, Set<String> stableKeys) {
        if (lines == null) {
            return;
        }
        for (DynamicTableViewSnapshot.SectionLine line : lines) {
            if (line != null && hasText(line.getKey())) {
                stableKeys.add("sectionLine:" + line.getKey());
            }
        }
    }

    private void collectNormalQueryStableKeys(List<DynamicTableViewSnapshot.NormalQuery> queries, Set<String> stableKeys) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.NormalQuery query : queries) {
            if (query != null && hasText(query.getKey())) {
                stableKeys.add("normalQuery:" + query.getKey());
            }
        }
    }

    private void collectAdvancedQueryStableKeys(List<DynamicTableViewSnapshot.AdvancedQuery> queries, Set<String> stableKeys) {
        if (queries == null) {
            return;
        }
        for (DynamicTableViewSnapshot.AdvancedQuery query : queries) {
            if (query != null && hasText(query.getKey())) {
                stableKeys.add("advancedQuery:" + query.getKey());
            }
        }
    }

    private void collectProcessorStableKeys(List<DynamicTableViewSnapshot.Processor> processors,
                                            String phase,
                                            Set<String> stableKeys) {
        if (processors == null) {
            return;
        }
        for (DynamicTableViewSnapshot.Processor processor : processors) {
            if (processor != null && hasText(processor.getKey())) {
                stableKeys.add("processor:" + phase + ":" + processor.getKey());
            }
        }
    }

    private void collectPreparedVariableStableKeys(List<DynamicTableViewSnapshot.PreparedVariable> variables,
                                                   Set<String> stableKeys) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.PreparedVariable variable : variables) {
            if (variable != null && hasText(variable.getKey())) {
                stableKeys.add("preparedVariable:" + variable.getKey());
            }
        }
    }

    private void collectParentVariableStableKeys(List<DynamicTableViewSnapshot.ParentVariable> variables,
                                                 Set<String> stableKeys) {
        if (variables == null) {
            return;
        }
        for (DynamicTableViewSnapshot.ParentVariable variable : variables) {
            if (variable != null && hasText(variable.getKey())) {
                stableKeys.add("parentVariable:" + variable.getKey());
            }
        }
    }

    private String stableFieldId(DynamicTableViewSnapshot.Field field, String stablePrefix) {
        if (field == null) {
            return null;
        }
        if ("systemField".equals(stablePrefix)) {
            return field.getName();
        }
        return field.getKey();
    }

    private String permissionFieldId(DynamicTableViewSnapshot.Field field) {
        if (hasText(field.getName())) {
            return field.getName();
        }
        return field.getKey();
    }

    private String getPermission(DynamicTableViewSnapshot.PermissionSet permissions, String property) {
        if ("view".equals(property)) {
            return permissions.getView();
        }
        if ("create".equals(property)) {
            return permissions.getCreate();
        }
        if ("update".equals(property)) {
            return permissions.getUpdate();
        }
        return null;
    }

    private void setPermission(DynamicTableViewSnapshot.PermissionSet permissions, String property, String value) {
        if ("view".equals(property)) {
            permissions.setView(value);
        } else if ("create".equals(property)) {
            permissions.setCreate(value);
        } else if ("update".equals(property)) {
            permissions.setUpdate(value);
        }
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private void addIfText(List<String> values, String value) {
        if (hasText(value)) {
            add(values, value);
        }
    }

    private void addDeleteIfInactive(List<String> values, Set<String> activePermissions, String value) {
        if (hasText(value) && !activePermissions.contains(value)) {
            add(values, value);
        }
    }

    private void add(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }
}
