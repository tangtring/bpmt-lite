package com.riversoft.api.http;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import com.riversoft.api.ApiServlet;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiServletTest {

    @Test
    public void successResponseSerializesSuccessTrue() {
        String json = ApiJson.toJson(ApiResponse.success(Collections.singletonMap("name", "RV_TEST")));

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"name\":\"RV_TEST\""));
    }

    @Test
    public void errorResponseSerializesStableCode() {
        ApiError error = new ApiError(
                "DYNAMIC_TABLE_ALREADY_EXISTS",
                "表已存在",
                Collections.<String, Object>emptyMap(),
                "req-1");

        String json = ApiJson.toJson(ApiResponse.error(error));

        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"code\":\"DYNAMIC_TABLE_ALREADY_EXISTS\""));
        assertTrue(json.contains("\"requestId\":\"req-1\""));
    }

    @Test
    public void unknownRouteReturnsJsonError() throws Exception {
        ApiServlet servlet = new ApiServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/not-found");
        request.setPathInfo("/not-found");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service((javax.servlet.ServletRequest) request, (javax.servlet.ServletResponse) response);

        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("\"code\":\"API_ROUTE_NOT_FOUND\""));
        assertTrue(response.getStatus() == 404);
    }

    @Test
    public void unsupportedMethodReturnsJsonError() throws Exception {
        ApiServlet servlet = new ApiServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/v1/dynamic-tables");
        request.setPathInfo("/dynamic-tables");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service((javax.servlet.ServletRequest) request, (javax.servlet.ServletResponse) response);

        assertTrue(response.getContentAsString().contains("\"code\":\"API_METHOD_NOT_ALLOWED\""));
        assertTrue(response.getStatus() == 405);
    }
}
