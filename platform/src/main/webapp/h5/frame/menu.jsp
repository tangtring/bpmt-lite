<%@ page language="java" pageEncoding="UTF-8"%>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/common.jsp"%>
<%@ include file="/include/h5_head.jsp"%>

<div class="bpmt-header">
	<h1 class="bpmt-title">${wpf:lan('#:zh[菜单]:en[Menu]#')}</h1>
	<c:url var="panelUrl" value="${_acp}/panel.shtml">
		<c:param name="domain" value="${domainKey}" />
		<c:param name="_action_mode" value="h5" />
	</c:url>
	<a href="${panelUrl}">${wpf:lan('#:zh[首页]:en[Home]#')}</a>
</div>

<div class="bpmt-page">
	<c:choose>
		<c:when test="${fn:length(menus)>0}">
			<ul class="bpmt-list">
				<c:forEach items="${menus}" var="menu">
					<li class="bpmt-card">
						<c:choose>
							<c:when test="${menu.openType==1 && menu.action!=null && menu.action!=''}">
								<c:url var="menuUrl" value="${_cp}${menu.action}">
									<c:param name="_action_mode" value="h5" />
									<c:param name="_frame_type" value="1" />
									<c:if test="${menu.params!=null && menu.params!=''}">
										<c:param name="_params" value="${menu.params}" />
									</c:if>
								</c:url>
								<a href="${menuUrl}">
									<strong>${wpf:lan(menu.name)}</strong>
								</a>
							</c:when>
							<c:otherwise>
								<strong>${wpf:lan(menu.name)}</strong>
							</c:otherwise>
						</c:choose>
					</li>
				</c:forEach>
			</ul>
		</c:when>
		<c:otherwise>
			<div class="bpmt-card">${wpf:lan('#:zh[没有可访问的菜单]:en[No accessible menu]#')}.</div>
		</c:otherwise>
	</c:choose>
</div>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/h5_bottom.jsp"%>
