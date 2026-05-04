<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map"%>
<%!
private String escapeHtml(Object value) {
    if (value == null) {
        return "";
    }
    String text = String.valueOf(value);
    StringBuilder escaped = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        switch (ch) {
        case '&':
            escaped.append("&amp;");
            break;
        case '<':
            escaped.append("&lt;");
            break;
        case '>':
            escaped.append("&gt;");
            break;
        case '"':
            escaped.append("&quot;");
            break;
        case '\'':
            escaped.append("&#39;");
            break;
        default:
            escaped.append(ch);
            break;
        }
    }
    return escaped.toString();
}
%>
<%
Map oauthAccessDenied = (Map) request.getAttribute("oauthAccessDenied");
if (oauthAccessDenied == null) {
    oauthAccessDenied = (Map) session.getAttribute("OAUTH_ACCESS_DENIED_CONTEXT");
}
String userId = oauthAccessDenied == null || oauthAccessDenied.get("userId") == null ? "" : String.valueOf(oauthAccessDenied.get("userId"));
String thirdpartName = oauthAccessDenied == null || oauthAccessDenied.get("thirdpartName") == null ? "目标外部系统" : String.valueOf(oauthAccessDenied.get("thirdpartName"));
String requestId = oauthAccessDenied == null || oauthAccessDenied.get("requestId") == null ? "" : String.valueOf(oauthAccessDenied.get("requestId"));
String escapedUserId = escapeHtml(userId);
String escapedThirdpartName = escapeHtml(thirdpartName);
String escapedRequestId = escapeHtml(requestId);
String cp = request.getContextPath();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>当前账号无权访问外部系统</title>
<style>
body {
    margin: 0;
    font-family: Arial, "Microsoft YaHei", sans-serif;
    background: #f5f7fa;
    color: #2f3542;
}
.oauth-denied {
    max-width: 600px;
    margin: 96px auto;
    padding: 32px;
    background: #fff;
    border: 1px solid #dfe4ea;
}
.oauth-denied h1 {
    margin: 0 0 16px;
    font-size: 22px;
}
.oauth-denied p {
    margin: 8px 0;
    line-height: 1.7;
}
.oauth-actions {
    margin-top: 24px;
}
.oauth-actions form {
    display: inline-block;
    margin: 0 8px 8px 0;
}
.oauth-actions button {
    padding: 8px 18px;
    border: 1px solid #c8d0dc;
    background: #fff;
    cursor: pointer;
}
.oauth-actions .primary {
    background: #2f80ed;
    border-color: #2f80ed;
    color: #fff;
}
.request-id {
    color: #747d8c;
    font-size: 12px;
}
</style>
</head>
<body>
<div class="oauth-denied">
    <h1>当前账号无权访问外部系统</h1>
    <p>当前 BPMT 账号 <strong><%= escapedUserId %></strong> 没有访问 <strong><%= escapedThirdpartName %></strong> 的权限。</p>
    <p>你可以退出当前 BPMT 账号并重新登录其他账号，也可以取消本次登录并返回第三方系统。</p>
    <div class="oauth-actions">
        <form action="<%= cp %>/oauth/OAuthAction/switchAccount.shtml" method="post">
            <button type="submit" class="primary">退出当前账号并重新登录</button>
        </form>
        <form action="<%= cp %>/oauth/OAuthAction/cancelAccessDenied.shtml" method="post">
            <button type="submit">取消并返回第三方系统</button>
        </form>
    </div>
    <% if (requestId.length() > 0) { %>
    <p class="request-id">Request ID: <%= escapedRequestId %></p>
    <% } %>
</div>
</body>
</html>
