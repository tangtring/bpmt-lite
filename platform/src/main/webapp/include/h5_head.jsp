<%@ page language="java" pageEncoding="UTF-8"%>
<c:if test="${(_head==null && param._head != 'false') || (_head!=false && param._head!='false')}">
	<!doctype html>
	<html class="no-js">
	<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
	<meta name="renderer" content="webkit">
	<meta http-equiv="Cache-Control" content="no-siteapp" />
	<meta name="format-detection" content="telephone=no" />
	<c:if test="${_ico!=null&&_ico!=''}">
		<link rel="shortcut icon" href="${_ico}" type="image/x-icon" />
	</c:if>

	<title>${wpf:lan(_title)}</title>

	<script src="${_cp}/js/jquery-1.11.3.min.js"></script>
	<c:if test="${_h5_js=='amaze'||param._h5_js=='amaze'}">
		<link rel="stylesheet" href="${_cp}/css/amazeui.min.css" type="text/css">
		<script src="${_cp}/js/amazeui.min.js"></script>
	</c:if>
	<script src="${_cp}/js/jquery.form.min.js"></script>
	<script src="${_cp}/js/ws-widget.js"></script>
	<link rel="stylesheet" href="${_cp}/h5/assets/bpmt-h5.css" type="text/css">
	<script src="${_cp}/h5/assets/bpmt-h5.js"></script>
	<script src="${_cp}/js/ws-wxui.js"></script>

	</head>
	<body class="bpmt-h5">
</c:if>
