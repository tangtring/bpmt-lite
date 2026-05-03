<%@ page language="java" pageEncoding="UTF-8"%>

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/common.jsp"%>
<%@ include file="/include/html_head.jsp"%>

<script type="text/javascript">
	$(function() {
		var $zone = $('#${_zone}');
		var refresh = function() {
			var $form = $('#${_form}');
			if ($form.size() > 0) {
				$form.submit();
			}
		};

		var callHandler = function(handler, id) {
			if (handler.length > 1) {
				handler($zone, id);
			} else {
				handler(id);
			}
		};

		var fallbackDetail = function(id) {
			var $win = Ajax.win('${_acp}/detail.shtml', {
				title : '${wpf:lan("#:zh[查看订单]:en[View Order]#")}',
				minWidth : 1024,
				data : {
					_TASK_ID : id
				}
			});
			Core.fn($win, 'callback', refresh);
		};

		var fallbackTask = function(id) {
			var $win = Ajax.win('${_acp}/form.shtml', {
				title : '${wpf:lan("#:zh[处理订单]:en[Handle Order]#")}',
				minWidth : 1024,
				data : {
					_TASK_ID : id
				}
			});
			Core.fn($win, 'callback', refresh);
		};

		$('button[name=detail]', $zone).click(function() {
			var id = $(this).val();
			var invokeDetail = Core.fn($zone, 'invokeDetail');
			if ($.isFunction(invokeDetail)) {
				callHandler(invokeDetail, id);
			} else {
				fallbackDetail(id);
			}
		});

		$('button[name=task]', $zone).click(function() {
			var id = $(this).val();
			var invokeTask = Core.fn($zone, 'invokeTask');
			if ($.isFunction(invokeTask)) {
				callHandler(invokeTask, id);
			} else {
				fallbackTask(id);
			}
		});
	});
</script>

<table class="ws-table" form="${_form}">
	<tr>
		<th style="width: 50px;">${wpf:lan("#:zh[操作]:en[Operation]#")}</th>
		<th>${wpf:lan("#:zh[单号]:en[Order No.]#")}</th>
		<th style="width: 110px;">${wpf:lan("#:zh[所属流程]:en[Belong to the process]#")}</th>
		<th style="min-width: 250px; width: 250px;">${wpf:lan("#:zh[摘要]:en[Abstract]#")}</th>
		<th style="width: 110px;">${wpf:lan("#:zh[处理人]:en[Handler]#")}</th>
		<th style="width: 110px;">${wpf:lan("#:zh[当前节点]:en[Current node]#")}</th>
		<th style="width: 180px;" field="createTime">${wpf:lan("#:zh[开始时间]:en[Start time]#")}</th>
		<th style="width: 110px;">${wpf:lan("#:zh[等待时长]:en[Waiting time]#")}</th>
	</tr>
	<c:forEach items="${list}" var="vo">
		<tr>
			<td class="ws-group center"><button name="detail" type="button" text="false" icon="circle-zoomin" value="${vo.task.id}">${wpf:lan("#:zh[查看]:en[View]#")}</button>
				<button name="task" type="button" text="false" icon="circle-triangle-e" value="${vo.task.id}">${wpf:lan("#:zh[处理]:en[Handle]#")}</button></td>
			<td class="center">${vo.ordId}</td>
			<td class="center">${vo.pdName}</td>
			<td class="left">${vo.order.REMARK}</td>
			<td class="center">${vo.assignee}</td>
			<td class="center">${vo.activity}</td>
			<td class="center">${wcm:widget('date[datetime]',vo.task.createTime)}</td>
			<td class="center">${wpf:formatDuring(wpf:compareDate(_now,vo.task.createTime,'s'))}</td>
		</tr>
	</c:forEach>
</table>

<wcm:page dp="${dp}" form="${_form}" />

<%-- 每个模块页面必须引入 --%>
<%@ include file="/include/html_bottom.jsp"%>
