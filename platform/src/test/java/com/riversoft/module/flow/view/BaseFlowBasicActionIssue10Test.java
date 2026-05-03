package com.riversoft.module.flow.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.riversoft.flow.FlowObject;

public class BaseFlowBasicActionIssue10Test {

    @Test
    public void emptyBusinessKeyDoesNotOverwriteExistingOrderId() {
        FlowObject fo = new FlowObject();
        fo.setOrdId("ORD-FALLBACK");

        BaseFlowBasicAction.applyBusinessKeyIfPresent(fo, "");

        assertEquals("ORD-FALLBACK", fo.getOrdId());
    }

    @Test
    public void nullBusinessKeyDoesNotOverwriteExistingOrderId() {
        FlowObject fo = new FlowObject();
        fo.setOrdId("ORD-FALLBACK");

        BaseFlowBasicAction.applyBusinessKeyIfPresent(fo, null);

        assertEquals("ORD-FALLBACK", fo.getOrdId());
    }

    @Test
    public void nonEmptyBusinessKeyOverwritesExistingOrderId() {
        FlowObject fo = new FlowObject();
        fo.setOrdId("ORD-FALLBACK");

        BaseFlowBasicAction.applyBusinessKeyIfPresent(fo, "ORD-BUSINESS");

        assertEquals("ORD-BUSINESS", fo.getOrdId());
    }
}
