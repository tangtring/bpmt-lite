package com.riversoft.api.modules.dynamic_table_views;

import java.util.ArrayList;
import java.util.List;

public class DynamicTableViewDefaults {
    public DynamicTableViewSnapshot normalize(DynamicTableViewSnapshot snapshot) {
        DynamicTableViewSnapshot normalized = snapshot == null ? new DynamicTableViewSnapshot() : snapshot;
        if (normalized.getBase() == null) {
            normalized.setBase(new DynamicTableViewSnapshot.Base());
        }
        if (normalized.getFields() == null) {
            normalized.setFields(new DynamicTableViewSnapshot.Fields());
        }
        if (normalized.getQueries() == null) {
            normalized.setQueries(new DynamicTableViewSnapshot.Queries());
        }
        if (normalized.getLimits() == null) {
            normalized.setLimits(new ArrayList<DynamicTableViewSnapshot.Limit>());
        }
        if (normalized.getVariables() == null) {
            normalized.setVariables(new DynamicTableViewSnapshot.Variables());
        }
        if (normalized.getProcessors() == null) {
            normalized.setProcessors(new DynamicTableViewSnapshot.Processors());
        }
        if (normalized.getSubviews() == null) {
            normalized.setSubviews(new DynamicTableViewSnapshot.Subviews());
        }
        if (normalized.getButtons() == null) {
            normalized.setButtons(new DynamicTableViewSnapshot.Buttons());
        }
        if (normalized.getScripts() == null) {
            normalized.setScripts(new DynamicTableViewSnapshot.Scripts());
        }

        normalizeBase(normalized.getBase());
        normalizeFields(normalized.getFields());
        normalizeQueries(normalized.getQueries());
        normalizeLimits(normalized.getLimits());
        normalizeVariables(normalized.getVariables());
        normalizeProcessors(normalized.getProcessors());
        normalizeSubviews(normalized.getSubviews());
        normalizeButtons(normalized.getButtons());
        return normalized;
    }

    private void normalizeBase(DynamicTableViewSnapshot.Base base) {
        if (base.getLayoutColumns() == null) {
            base.setLayoutColumns(Integer.valueOf(2));
        }
        if (base.getInitQuery() == null) {
            base.setInitQuery(Boolean.TRUE);
        }
        if (base.getPageLimit() == null) {
            base.setPageLimit(Integer.valueOf(20));
        }
        if (base.getDefaultSort() == null) {
            base.setDefaultSort(new DynamicTableViewSnapshot.Sort());
        }
    }

    private void normalizeFields(DynamicTableViewSnapshot.Fields fields) {
        if (fields.getSystemFields() == null) {
            fields.setSystemFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getComputedFields() == null) {
            fields.setComputedFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getFormFields() == null) {
            fields.setFormFields(new ArrayList<DynamicTableViewSnapshot.Field>());
        }
        if (fields.getSectionLines() == null) {
            fields.setSectionLines(new ArrayList<DynamicTableViewSnapshot.SectionLine>());
        }
        if (fields.getListOrder() == null) {
            fields.setListOrder(new ArrayList<String>());
        }
        fillFieldKeys(fields.getComputedFields(), "computedField");
        fillFieldKeys(fields.getFormFields(), "formField");
        fillSectionLineKeys(fields.getSectionLines());
    }

    private void normalizeQueries(DynamicTableViewSnapshot.Queries queries) {
        if (queries.getNormal() == null) {
            queries.setNormal(new ArrayList<DynamicTableViewSnapshot.NormalQuery>());
        }
        if (queries.getAdvanced() == null) {
            queries.setAdvanced(new ArrayList<DynamicTableViewSnapshot.AdvancedQuery>());
        }
        for (int i = 0; i < queries.getNormal().size(); i++) {
            DynamicTableViewSnapshot.NormalQuery query = queries.getNormal().get(i);
            if (query != null && isBlank(query.getKey())) {
                query.setKey("normalQuery-" + (i + 1));
            }
        }
        for (int i = 0; i < queries.getAdvanced().size(); i++) {
            DynamicTableViewSnapshot.AdvancedQuery query = queries.getAdvanced().get(i);
            if (query != null && isBlank(query.getKey())) {
                query.setKey("advancedQuery-" + (i + 1));
            }
        }
    }

    private void normalizeLimits(List<DynamicTableViewSnapshot.Limit> limits) {
        for (int i = 0; i < limits.size(); i++) {
            DynamicTableViewSnapshot.Limit limit = limits.get(i);
            if (limit != null && isBlank(limit.getKey())) {
                limit.setKey("limit-" + (i + 1));
            }
        }
    }

    private void normalizeVariables(DynamicTableViewSnapshot.Variables variables) {
        if (variables.getPrepared() == null) {
            variables.setPrepared(new ArrayList<DynamicTableViewSnapshot.PreparedVariable>());
        }
        if (variables.getParents() == null) {
            variables.setParents(new ArrayList<DynamicTableViewSnapshot.ParentVariable>());
        }
        for (int i = 0; i < variables.getPrepared().size(); i++) {
            DynamicTableViewSnapshot.PreparedVariable variable = variables.getPrepared().get(i);
            if (variable != null && isBlank(variable.getKey())) {
                variable.setKey("preparedVariable-" + (i + 1));
            }
        }
        for (int i = 0; i < variables.getParents().size(); i++) {
            DynamicTableViewSnapshot.ParentVariable variable = variables.getParents().get(i);
            if (variable != null && isBlank(variable.getKey())) {
                variable.setKey("parentVariable-" + (i + 1));
            }
            if (variable != null && variable.getForeigns() == null) {
                variable.setForeigns(new ArrayList<DynamicTableViewSnapshot.Foreign>());
            }
        }
    }

    private void normalizeProcessors(DynamicTableViewSnapshot.Processors processors) {
        if (processors.getBefore() == null) {
            processors.setBefore(new ArrayList<DynamicTableViewSnapshot.Processor>());
        }
        if (processors.getAfter() == null) {
            processors.setAfter(new ArrayList<DynamicTableViewSnapshot.Processor>());
        }
        for (int i = 0; i < processors.getBefore().size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.getBefore().get(i);
            if (processor != null && isBlank(processor.getKey())) {
                processor.setKey("processor-before-" + (i + 1));
            }
        }
        for (int i = 0; i < processors.getAfter().size(); i++) {
            DynamicTableViewSnapshot.Processor processor = processors.getAfter().get(i);
            if (processor != null && isBlank(processor.getKey())) {
                processor.setKey("processor-after-" + (i + 1));
            }
        }
    }

    private void normalizeSubviews(DynamicTableViewSnapshot.Subviews subviews) {
        if (subviews.getSystemTabs() == null) {
            subviews.setSystemTabs(new ArrayList<DynamicTableViewSnapshot.SystemTab>());
        }
        if (subviews.getViewTabs() == null) {
            subviews.setViewTabs(new ArrayList<DynamicTableViewSnapshot.ViewTab>());
        }
        for (int i = 0; i < subviews.getViewTabs().size(); i++) {
            DynamicTableViewSnapshot.ViewTab tab = subviews.getViewTabs().get(i);
            if (tab != null && isBlank(tab.getKey())) {
                tab.setKey("viewTab-" + (i + 1));
            }
        }
    }

    private void normalizeButtons(DynamicTableViewSnapshot.Buttons buttons) {
        if (buttons.getSystem() == null || buttons.getSystem().isEmpty()) {
            buttons.setSystem(defaultSystemButtons());
        }
        if (buttons.getItem() == null) {
            buttons.setItem(new ArrayList<DynamicTableViewSnapshot.CustomButton>());
        }
        if (buttons.getSummary() == null) {
            buttons.setSummary(new ArrayList<DynamicTableViewSnapshot.CustomButton>());
        }
        for (int i = 0; i < buttons.getItem().size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.getItem().get(i);
            if (button != null && isBlank(button.getKey())) {
                button.setKey("itemButton-" + (i + 1));
            }
        }
        for (int i = 0; i < buttons.getSummary().size(); i++) {
            DynamicTableViewSnapshot.CustomButton button = buttons.getSummary().get(i);
            if (button != null && isBlank(button.getKey())) {
                button.setKey("summaryButton-" + (i + 1));
            }
        }
    }

    private List<DynamicTableViewSnapshot.SystemButton> defaultSystemButtons() {
        List<DynamicTableViewSnapshot.SystemButton> buttons = new ArrayList<DynamicTableViewSnapshot.SystemButton>();
        buttons.add(systemButton("CREATE"));
        buttons.add(systemButton("EDIT"));
        buttons.add(systemButton("DELETE"));
        buttons.add(systemButton("VIEW"));
        return buttons;
    }

    private DynamicTableViewSnapshot.SystemButton systemButton(String name) {
        DynamicTableViewSnapshot.SystemButton button = new DynamicTableViewSnapshot.SystemButton();
        button.setName(name);
        return button;
    }

    private void fillFieldKeys(List<DynamicTableViewSnapshot.Field> fields, String prefix) {
        for (int i = 0; i < fields.size(); i++) {
            DynamicTableViewSnapshot.Field field = fields.get(i);
            if (field != null && isBlank(field.getKey())) {
                field.setKey(prefix + "-" + (i + 1));
            }
        }
    }

    private void fillSectionLineKeys(List<DynamicTableViewSnapshot.SectionLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            DynamicTableViewSnapshot.SectionLine line = lines.get(i);
            if (line != null && isBlank(line.getKey())) {
                line.setKey("sectionLine-" + (i + 1));
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
