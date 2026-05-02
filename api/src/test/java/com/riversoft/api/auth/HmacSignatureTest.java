package com.riversoft.api.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class HmacSignatureTest {

    @Test
    public void sha256HexReturnsEmptyBodyHash() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb924"
                + "27ae41e4649b934ca495991b7852b855", HmacSignature.sha256Hex(new byte[0]));
    }

    @Test
    public void canonicalReturnsSixLineString() {
        String canonical = HmacSignature.canonical(
                "GET",
                "/v1/dynamic-tables",
                "b=2",
                "1710000000",
                "nonce-1",
                "body-hash");

        assertEquals("GET\n/v1/dynamic-tables\nb=2\n1710000000\nnonce-1\nbody-hash", canonical);
    }

    @Test
    public void sameSecretSignaturePassesConstantTimeEquals() {
        String canonical = HmacSignature.canonical("POST", "/v1/tables", "", "1710000000", "nonce-1", "body-hash");

        String left = HmacSignature.sign("secret", canonical);
        String right = HmacSignature.sign("secret", canonical);

        assertTrue(HmacSignature.constantTimeEquals(left, right));
    }

    @Test
    public void normalizeQuerySortsSingleParameterAsKeyValue() {
        Map<String, String[]> query = new LinkedHashMap<String, String[]>();
        query.put("b", new String[] { "2" });

        assertEquals("b=2", HmacSignature.normalizeQuery(query));
    }
}
