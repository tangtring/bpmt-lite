package com.riversoft.module.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.riversoft.core.exception.SystemRuntimeException;
import com.riversoft.flow.key.VariableKeys;

public class CommonFlowActionIssue10Test {

    @Test
    public void taskListButtonsUseParentHandlerWithDirectFallback() throws Exception {
        String jsp = readWebapp("xhtml/flow/CommonFlowAction/task_list.jsp");

        assertFalse(jsp.contains("Core.fn($zone, 'invokeDetail')(id)"));
        assertFalse(jsp.contains("Core.fn($zone, 'invokeTask')(id)"));
        assertTrue(jsp.contains("var invokeDetail = Core.fn($zone, 'invokeDetail')"));
        assertTrue(jsp.contains("var invokeTask = Core.fn($zone, 'invokeTask')"));
        assertTrue(jsp.contains("$.isFunction(invokeDetail)"));
        assertTrue(jsp.contains("$.isFunction(invokeTask)"));
        assertTrue(jsp.contains("var callHandler = function(handler, id)"));
        assertTrue(jsp.contains("handler.length > 1"));
        assertTrue(jsp.contains("detail.shtml"));
        assertTrue(jsp.contains("form.shtml"));
        assertTrue(jsp.contains("_TASK_ID : id"));
        assertTrue(jsp.contains("Core.fn($win, 'callback', refresh)"));
    }

    @Test
    public void taskOrdIdUsesBusinessKeyFirst() {
        Map<String, Object> variables = new HashMap<>();

        String ordId = CommonFlowAction.resolveTaskOrdId("TASK-1", "ORD-1", variables, new CommonFlowAction.TaskOrdIdHistoryLookup() {
            @Override
            public String findOrdId(String historyTableName, String taskId) {
                throw new AssertionError("history lookup should not be used when businessKey exists");
            }
        });

        assertEquals("ORD-1", ordId);
    }

    @Test
    public void taskOrdIdFallsBackToHistoryTableTaskId() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VariableKeys._ORDER_HISTORY_TABLE_NAME.name(), "WF_HISTORY_A");

        String ordId = CommonFlowAction.resolveTaskOrdId("TASK-1", "", variables, new CommonFlowAction.TaskOrdIdHistoryLookup() {
            @Override
            public String findOrdId(String historyTableName, String taskId) {
                assertEquals("WF_HISTORY_A", historyTableName);
                assertEquals("TASK-1", taskId);
                return "ORD-2";
            }
        });

        assertEquals("ORD-2", ordId);
    }

    @Test(expected = SystemRuntimeException.class)
    public void taskOrdIdThrowsWhenBusinessKeyAndHistoryAreMissing() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VariableKeys._ORDER_HISTORY_TABLE_NAME.name(), "WF_HISTORY_A");

        CommonFlowAction.resolveTaskOrdId("TASK-1", "", variables, new CommonFlowAction.TaskOrdIdHistoryLookup() {
            @Override
            public String findOrdId(String historyTableName, String taskId) {
                return null;
            }
        });
    }

    @Test
    public void formRedirectUrlOmitsNullOrderId() {
        String url = CommonFlowAction.buildFormRedirectUrl("/flow/view/FooAction/form.shtml", "{form:true,pdKey:''}", "TASK-1", null);

        assertFalse(url.contains("_ORD_ID=null"));
        assertTrue(url.contains("_TASK_ID=TASK-1"));
    }

    @Test
    public void detailRedirectUrlIncludesTaskAndFallbackOrderId() {
        String url = CommonFlowAction.buildDetailRedirectUrl("/flow/view/FooAction/detail.shtml", "{detail:true,taskId:'TASK-1'}", "TASK-1", "ORD-1");

        assertTrue(url.contains("_TASK_ID=TASK-1"));
        assertTrue(url.contains("_ORD_ID=ORD-1"));
        assertFalse(url.contains("_ORD_ID=null"));
    }

    private String readWebapp(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/webapp", relativePath)), StandardCharsets.UTF_8);
    }
}
