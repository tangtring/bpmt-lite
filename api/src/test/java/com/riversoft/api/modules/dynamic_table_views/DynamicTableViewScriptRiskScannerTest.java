package com.riversoft.api.modules.dynamic_table_views;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DynamicTableViewScriptRiskScannerTest {
    @Test
    public void scannerMarksSqlAndMutationRiskWithoutReturningScriptText() {
        DynamicTableViewScriptRiskScanner scanner = new DynamicTableViewScriptRiskScanner();
        List<DynamicTableViewResponse.Warning> warnings =
                scanner.scan("processors.before[0].exec.script", Integer.valueOf(1), "ORMService.getInstance().remove(vo);");
        assertEquals("HIGH", warnings.get(0).getLevel());
        assertEquals("SCRIPT_RISK_MUTATION", warnings.get(0).getCode());
        assertFalse(warnings.get(0).getMessage().contains("ORMService"));
    }

    @Test
    public void scannerMarksSqlPathAndKeywordRisk() {
        DynamicTableViewScriptRiskScanner scanner = new DynamicTableViewScriptRiskScanner();

        List<DynamicTableViewResponse.Warning> warnings =
                scanner.scan("queries.advanced[0].sql.script", Integer.valueOf(1), "select * from CRM_CUSTOMER");

        assertEquals("SCRIPT_RISK_SQL", warnings.get(0).getCode());
    }

    @Test
    public void scannerMarksFrontendPathRisk() {
        DynamicTableViewScriptRiskScanner scanner = new DynamicTableViewScriptRiskScanner();

        List<DynamicTableViewResponse.Warning> warnings =
                scanner.scan("scripts.list.script", Integer.valueOf(1), "return vo.NAME;");

        assertEquals("SCRIPT_RISK_FRONTEND", warnings.get(0).getCode());
    }

    @Test
    public void scannerMarksReflectionAndProcessRiskCaseInsensitive() {
        DynamicTableViewScriptRiskScanner scanner = new DynamicTableViewScriptRiskScanner();

        List<DynamicTableViewResponse.Warning> warnings =
                scanner.scan("fields.systemFields[0].content.script", Integer.valueOf(1),
                        "class.forname(name); new processbuilder(cmd);");

        assertTrue(containsCode(warnings, "SCRIPT_RISK_REFLECTION"));
        assertTrue(containsCode(warnings, "SCRIPT_RISK_PROCESS"));
    }

    private boolean containsCode(List<DynamicTableViewResponse.Warning> warnings, String code) {
        for (DynamicTableViewResponse.Warning warning : warnings) {
            if (code.equals(warning.getCode())) {
                return true;
            }
        }
        return false;
    }
}
