package com.riversoft.core.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ActionsForwardedUrlTest {

	@Test
	public void contextPathUsesForwardedHttpsHostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/authorize");
		request.setContextPath("");
		request.setRequestURI("/oauth/authorize");
		request.setServerName("bpmt-web");
		request.setServerPort(8080);
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "bpmt.example.com");
		request.addHeader("X-Forwarded-Port", "18443");

		assertEquals("https://bpmt.example.com:18443", Actions.Util.getContextPath(request));
	}

	@Test
	public void contextPathOmitsStandardHttpsPort() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "bpmt.example.com");
		request.addHeader("X-Forwarded-Port", "443");

		assertEquals("https://bpmt.example.com", Actions.Util.getContextPath(request));
	}

	@Test
	public void contextPathUsesPortFromForwardedHostBeforeForwardedPort() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "127.0.0.1:18443");
		request.addHeader("X-Forwarded-Port", "443");

		assertEquals("https://127.0.0.1:18443", Actions.Util.getContextPath(request));
	}

	@Test
	public void contextPathFallsBackToHostHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("Host", "127.0.0.1:18443");

		assertEquals("https://127.0.0.1:18443", Actions.Util.getContextPath(request));
	}

	@Test
	public void contextPathKeepsExistingHttpFallback() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/");
		request.setServerName("localhost");
		request.setServerPort(18080);

		assertEquals("http://localhost:18080", Actions.Util.getContextPath(request));
	}

	@Test
	public void fullUrlRebuildsFromPublicBaseUriAndQuery() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/authorize");
		request.setRequestURI("/oauth/authorize");
		request.setQueryString("response_type=code&client_id=demo-client");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "127.0.0.1:18443");

		assertEquals("https://127.0.0.1:18443/oauth/authorize?response_type=code&client_id=demo-client",
				Actions.Util.getFullURL(request));
	}

	@Test
	public void fullUrlKeepsExplicitFullUrlParameterCompatibility() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/OAuthAction/authorize.shtml");
		request.setRequestURI("/oauth/OAuthAction/authorize.shtml");
		request.setParameter(Actions.Keys.FULL_URL.toString(),
				"https://public.example/oauth/authorize?client_id=demo-client");

		assertEquals("https://public.example/oauth/authorize?client_id=demo-client",
				Actions.Util.getFullURL(request));
	}

	@Test
	public void forwardedHeaderUsesFirstCommaSeparatedValue() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/");
		request.addHeader("X-Forwarded-Proto", "https,http");
		request.addHeader("X-Forwarded-Host", "bpmt.example.com,internal.local");
		request.addHeader("X-Forwarded-Port", "443,8080");

		assertEquals("https://bpmt.example.com", Actions.Util.getContextPath(request));
	}
}
