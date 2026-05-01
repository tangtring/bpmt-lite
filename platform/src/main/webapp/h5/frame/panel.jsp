<%@ page language="java" pageEncoding="UTF-8"%>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/common.jsp"%>
<%@ include file="/include/h5_head.jsp"%>

<div class="bpmt-header">
	<h1 class="bpmt-title">
		<c:choose>
			<c:when test="${vo!=null}">${wpf:lan(vo.busiName)}</c:when>
			<c:otherwise>${wpf:lan('#:zh[首页面板]:en[Home panel]#')}</c:otherwise>
		</c:choose>
	</h1>
	<c:url var="menuUrl" value="${_acp}/menu.shtml">
		<c:param name="domain" value="${param.domain}" />
		<c:param name="_action_mode" value="h5" />
	</c:url>
	<a href="${menuUrl}">${wpf:lan('#:zh[菜单]:en[Menu]#')}</a>
</div>

<div class="bpmt-page">
	<c:choose>
		<c:when test="${fn:length(homes)>0}">
			<ul class="bpmt-list">
				<c:forEach items="${homes}" var="home">
					<li class="bpmt-card">
						<c:choose>
							<c:when test="${home.action!=null && home.action!=''}">
								<c:url var="homeUrl" value="${_cp}${home.action}">
									<c:param name="_action_mode" value="h5" />
									<c:param name="_frame_type" value="2" />
									<c:if test="${home.params!=null && home.params!=''}">
										<c:param name="_params" value="${home.params}" />
									</c:if>
								</c:url>
								<a href="${homeUrl}">
									<strong>${wpf:lan(home.name)}</strong>
								</a>
							</c:when>
							<c:otherwise>
								<strong>${wpf:lan(home.name)}</strong>
							</c:otherwise>
						</c:choose>
					</li>
				</c:forEach>
			</ul>
		</c:when>
		<c:otherwise>
			<div class="bpmt-card">${wpf:lan('#:zh[没有首页标签]:en[No front page tag]#')}.</div>
		</c:otherwise>
	</c:choose>
</div>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/h5_bottom.jsp"%>
