package com.riversoft.api.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public class ApiDocsContractTest {

    @Test
    public void openApiJsonPublishesDynamicTableRoutes() throws Exception {
        JsonNode root = new ObjectMapper().readTree(new File("src/main/webapp/openapi.json"));

        assertTrue(root.path("paths").has("/v1/dynamic-tables"));
        assertTrue(root.path("paths").has("/v1/dynamic-tables/{name}"));
        assertTrue(root.path("paths").has("/v1/dynamic-tables/{name}/sync-ddl"));
        assertTrue(root.path("paths").has("/v1/dynamic-table-templates"));
        assertTrue(root.path("components").path("securitySchemes").has("signatureHeader"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-writes-metadata"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-executes-ddl"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-risk-level"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("get").path("parameters").toString().contains("\"sort\""));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("get").path("parameters").toString().contains("\"order\""));
    }

    @Test
    public void docsIndexLinksOpenApiJson() throws Exception {
        String html = new String(Files.readAllBytes(new File("src/main/webapp/docs/index.html").toPath()), Charset.forName("UTF-8"));

        assertTrue(html.contains("../openapi.json"));
        assertTrue(html.contains("X-BPMT-Signature"));
    }
}
