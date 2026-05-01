<%@ page language="java" pageEncoding="UTF-8"%>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/common.jsp"%>
<%@ include file="/include/h5_head.jsp"%>

<div class="bpmt-header">
	<h1 class="bpmt-title">
		<c:choose>
			<c:when test="${domain!=null}">${wpf:lan(domain.busiName)}</c:when>
			<c:otherwise>${wpf:lan('#:zh[首页]:en[Home page]#')}</c:otherwise>
		</c:choose>
	</h1>
	<a href="${_cp}/frame/LoginAction/logout.shtml">${wpf:lan('#:zh[退出]:en[Logout]#')}</a>
</div>

<div class="bpmt-page">
	<c:choose>
		<c:when test="${fn:length(domains)>0}">
			<ul class="bpmt-list">
				<c:forEach items="${domains}" var="item">
					<c:url var="domainUrl" value="${_acp}/domain.shtml">
						<c:param name="domain" value="${item.domainKey}" />
						<c:param name="_action_mode" value="h5" />
					</c:url>
					<li class="bpmt-card">
						<a href="${domainUrl}">
							<strong>${wpf:lan(item.busiName)}</strong>
						</a>
					</li>
				</c:forEach>
			</ul>
		</c:when>
		<c:otherwise>
			<div class="bpmt-card">
				<c:choose>
					<c:when test="${domain!=null}">
						<p>${wpf:lan(domain.busiName)}</p>
					</c:when>
					<c:otherwise>
						<p>${wpf:lan('#:zh[没有可访问的域]:en[No accessible domain]#')}.</p>
					</c:otherwise>
				</c:choose>
			</div>
		</c:otherwise>
	</c:choose>

	<c:if test="${domain!=null}">
		<c:url var="menuUrl" value="${_acp}/menu.shtml">
			<c:param name="domain" value="${domain.domainKey}" />
			<c:param name="_action_mode" value="h5" />
		</c:url>
		<c:url var="panelUrl" value="${_acp}/panel.shtml">
			<c:param name="domain" value="${domain.domainKey}" />
			<c:param name="_action_mode" value="h5" />
		</c:url>
		<div class="bpmt-card">
			<a class="am-btn am-btn-block" href="${menuUrl}">${wpf:lan('#:zh[菜单]:en[Menu]#')}</a>
			<a class="am-btn am-btn-block" href="${panelUrl}">${wpf:lan('#:zh[首页面板]:en[Home panel]#')}</a>
		</div>
	</c:if>
</div>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/h5_bottom.jsp"%>
