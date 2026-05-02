package com.riversoft.api.context;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.riversoft.api.http.ApiException;
import com.riversoft.core.context.RequestContext;
import com.riversoft.core.context.SessionContext;
import com.riversoft.core.context.VariableContext;
import com.riversoft.core.web.Actions;
import com.riversoft.platform.SessionManager;

public final class ApiUserContext {

    private static final String DEFAULT_ACT_AS = "admin";

    private ApiUserContext() {
    }

    public static String resolveActAs(String configuredActAs) {
        if (configuredActAs == null || configuredActAs.trim().length() == 0) {
            return DEFAULT_ACT_AS;
        }
        return configuredActAs.trim();
    }

    public static void init(HttpServletRequest request, String configuredActAs) {
        initRequestContext(request);
        String actAs = resolveActAs(configuredActAs);
        if (!login(request, actAs) && (DEFAULT_ACT_AS.equals(actAs) || !login(request, DEFAULT_ACT_AS))) {
            throw new ApiException(403, "API_ACT_AS_USER_UNAVAILABLE", "API 技术用户不可用。");
        }
        initSessionContext(request.getSession());
        VariableContext.init();
    }

    private static boolean login(HttpServletRequest request, String actAs) {
        try {
            SessionManager.doUserLogin(request, actAs);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void initRequestContext(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameterValues(name));
        }
        params.put(Actions.Keys.CP.toString(), new String[] { Actions.Util.getContextPath(request) });
        params.put(Actions.Keys.ACP.toString(), new String[] { buildAcp(request) });
        params.put(Actions.Keys.ACTION_MODE.toString(), new String[] { "api" });
        RequestContext.init(request, params);
    }

    private static void initSessionContext(HttpSession session) {
        Map<String, Object> params = new HashMap<String, Object>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, session.getAttribute(name));
        }
        SessionContext.init(session, params);
    }

    private static String buildAcp(HttpServletRequest request) {
        String contextPath = Actions.Util.getContextPath(request);
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        return contextPath + (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
    }
}
