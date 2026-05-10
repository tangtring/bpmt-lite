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
        assertTrue(root.path("paths").has("/v1/dynamic-tables/{name}/ddl:sync"));
        assertTrue(root.path("paths").has("/v1/dynamic-tables/templates"));
        assertTrue(root.path("paths").has("/v1/database-operations/query"));
        assertTrue(root.path("paths").has("/v1/database-operations/find"));
        assertTrue(root.path("paths").has("/v1/database-operations/save"));
        assertTrue(root.path("paths").has("/v1/database-operations/exec"));
        assertTrue(root.path("components").path("securitySchemes").has("signatureHeader"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-writes-metadata"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-executes-ddl"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("post").has("x-bpmt-risk-level"));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("get").path("parameters").toString().contains("\"sort\""));
        assertTrue(root.path("paths").path("/v1/dynamic-tables").path("get").path("parameters").toString().contains("\"order\""));

        assertTrue(root.path("paths").has("/v1/dynamic-table-views"));
        assertTrue(root.path("paths").has("/v1/dynamic-table-views:validate"));
        assertTrue(root.path("paths").has("/v1/dynamic-table-views/{viewKey}"));
        assertTrue(root.path("paths").has("/v1/dynamic-table-views/{viewKey}/{section}"));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views").path("post").path("parameters").toString().contains("\"dryRun\""));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views/{viewKey}").path("put").path("parameters").toString().contains("\"dryRun\""));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views/{viewKey}/{section}").path("patch").path("parameters").toString().contains("\"dryRun\""));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views/{viewKey}").path("delete").path("parameters").toString().contains("\"confirmViewKey\""));
        assertTrue(root.toString().contains("\"DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED\""));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views").path("post").has("x-bpmt-writes-metadata"));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views").path("post").has("x-bpmt-executes-ddl"));
        assertTrue(root.path("paths").path("/v1/dynamic-table-views").path("post").has("x-bpmt-risk-level"));
    }

    @Test
    public void docsIndexLinksOpenApiJson() throws Exception {
        String html = new String(Files.readAllBytes(new File("src/main/webapp/docs/index.html").toPath()), Charset.forName("UTF-8"));

        assertTrue(html.contains("../openapi.json"));
        assertTrue(html.contains("X-BPMT-Signature"));
        assertTrue(html.contains("动态表视图 API"));
        assertTrue(html.contains("只管理 dyn 动态表视图"));
        assertTrue(html.contains("删除视图不会删除动态表和业务数据"));
    }

    @Test
    public void versionedOpenApiSnapshotMatchesRuntimeOpenApi() throws Exception {
        byte[] runtime = Files.readAllBytes(new File("src/main/webapp/openapi.json").toPath());
        byte[] versioned = Files.readAllBytes(new File("../docs/v1.7.0/openapi.json").toPath());

        assertTrue(new String(runtime, Charset.forName("UTF-8"))
                .equals(new String(versioned, Charset.forName("UTF-8"))));
    }
}
