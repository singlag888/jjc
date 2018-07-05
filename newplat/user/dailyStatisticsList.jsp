<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ include file ="../common/inc_include.jsp"%>
<%@ include file = "../common/inc_pageSetting.jsp"%>
<%@ include  file="../common/inc_datepicker.jsp" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title></title>
<%@ include file = "../common/inc_file.jsp"%>
</head>
<body>
<div class="bread">统计管理&raquo;用户统计
</div>
<html:form action="/managerAction.do?method=dailyStatisticsList" method="post">
<table class="tblist" cellpadding="0" cellspacing="0" border="0">
	<tr class="moon">
		<td>
			用户名/ID
			<html:text property="loginName" name="managerForm"/>
			<span>开始时间：<html:text property="startDate" name="managerForm" styleClass="Wdate" styleId="startTime"   onclick="WdatePicker({lang:'zh-cn'})" readonly="readonly"/></span>
			<span>结束时间：<html:text property="endDate" name="managerForm" styleClass="Wdate" styleId="endDate"  onclick="WdatePicker({lang:'zh-cn'})" readonly="readonly"/></span>
			<input type="submit" class="gbutton" value="查询" />
		</td>
	</tr>
</table>
</html:form>	

<%
//分页
String totalCount =request.getAttribute("count")!=null?(String)request.getAttribute("count"):"0";
String userName = (String)request.getAttribute("userName");
%>
<pg:pager 
	url="managerAction.do" 
	index="<%=index%>"
	items="<%=Integer.parseInt(totalCount)%>"
	maxPageItems="<%= maxPageItems %>"
	maxIndexPages="<%= maxIndexPages %>"
	isOffset="<%= isOffset %>"
	export="offset,currentPageNumber=pageNumber"
	scope="request"> 
	<pg:param name="method" value="dailyStatisticsList"/>
	<pg:param name="userName" value="<%=userName %>" />
	<bean:define id="totalLength" value="<%=totalCount%>"/>
	<bean:size id="length" name="list"/>
<logic:notEmpty name="list">
<table class="tblist" border="0" cellpadding="0" cellspacing="0">
	<tr>
		<th class="sel">序号</th>
		<th>ID</th>
		<th>用户名</th>
	    <th>统计时间</th>
	    <th>总充值</th>
	    <th>总提款</th>
	    <th>总打码</th>
	    <th>总中奖</th>
	</tr>
	<logic:iterate id="item" name="list" indexId="num">
	<tr onmousemove="this.style.background='#EEEEEE'" onmouseout="this.style.background='#ffffff'">
		<td class="numbers"><%=num.intValue()+1 %></td>
	    <td align="center">
	        <bean:write name="item" property="userId" />
	    </td>
	    <td align="center">
	    	 <bean:write name="item" property="userName" />
	    </td>
		<td align="center">
	      	<bean:write name="item" property="createTime" format="yyyy-MM-dd"/>
		</td>
		<td align="center">
	        <bean:write name="item" property="totalRecharge"/>
	    </td>
		<td align="center">
	        <bean:write name="item" property="totalDrawMoney"/>
	    </td>
		<td align="center">
	        <bean:write name="item" property="totalBet"/>
	    </td>
		<td align="center">
	        <bean:write name="item" property="totalWin"/>
	    </td>
	</tr>
	</logic:iterate>
</table>
	</logic:notEmpty>
<logic:empty name="list">
<div class="tbl-no-data">暂无数据！</div>
</logic:empty>

<logic:greaterThan name="totalLength" value="<%=maxPageItems+""%>">
	<jsp:include page="../template/pagination_template.jsp" flush="true"/>	    		
</logic:greaterThan>
</pg:pager>

</body>
</html>