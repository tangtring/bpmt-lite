package com.riversoft.api.context;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApiUserContextTest {

    @Test
    public void resolveActAsUsesAdminForNull() {
        assertEquals("admin", ApiUserContext.resolveActAs(null));
    }

    @Test
    public void resolveActAsUsesAdminForBlank() {
        assertEquals("admin", ApiUserContext.resolveActAs("  "));
    }

    @Test
    public void resolveActAsTrimsConfiguredUser() {
        assertEquals("api_user", ApiUserContext.resolveActAs(" api_user "));
    }
}
