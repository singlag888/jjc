<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ include file ="../common/inc_include.jsp"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ include file = "../common/inc_pageSetting.jsp"%>
<%@ include  file="../common/inc_datepicker.jsp" %>  
<html:html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title></title>
<%@ include file = "../common/inc_file.jsp"%>
<script type="text/javascript">
/**
 * 修改
 */
function modify(id){
	document.forms[0].action = "noticeAction.do?method=preAdd&action=modif&id="+id;
	document.forms[0].submit();
}
</script>
</head>
<body>
<div class="bread">广告/通知管理&raquo;公告管理
<c:if test = "${type==1}">优惠活动  </c:if>
<c:if test = "${type==2}">代理制度 </c:if>
<c:if test = "${type==3}">重要通知 </c:if>
<c:if test = "${type==4}">充值公告 </c:if>
</div>
<html:form action="/noticeAction.do?method=init">
	<table class="tblist" cellpadding="0" cellspacing="0" border="0">
		<tr class="moon">
			<td>			
				标题：<html:text property="title" name="noticeForm"/>
				<span>创建时间：<html:text property="startDate" name="noticeForm" styleClass="Wdate" styleId="startTime"   onclick="WdatePicker({lang:'zh-cn'})" readonly="readonly"/>
				——&nbsp;&nbsp;<html:text property="endDate" name="noticeForm" styleClass="Wdate" styleId="endDate"  onclick="WdatePicker({lang:'zh-cn'})" readonly="readonly"/></span>
	    		&nbsp;<input type="submit" value="查询" class="gbutton"/>
	    		&nbsp;<a href="noticeAction.do?method=preAdd" class="gbutton">新增</a>
			</td>
		</tr>
	</table>
</html:form>
<%
int number=1;
//分页
String totalCount =request.getAttribute("count")!=null?(String)request.getAttribute("count"):"0";
String startDate=(String)request.getAttribute("startDate");
String endDate=(String)request.getAttribute("endDate");
String adType=(String)request.getAttribute("adType");
%>
<pg:pager 
	url="noticeAction.do" 
	index="<%=index%>"
	items="<%=Integer.parseInt(totalCount)%>"
	maxPageItems="<%= maxPageItems %>"
	maxIndexPages="<%= maxIndexPages %>"
	isOffset="<%= isOffset %>"
	export="offset,currentPageNumber=pageNumber"
	scope="request"> 
	<pg:param name="method" value="init"/>
	<pg:param name="startDate" value="<%=startDate%>"/>
	<pg:param name="endDate" value="<%=endDate%>"/>
	<pg:param name="adType" value="<%=adType%>"/>
	<bean:define id="totalLength" value="<%=totalCount%>"/>

<bean:size id="length" name="list"/>
<logic:notEmpty name="list">
<form class="layui-form" action="#" lay-filter="reform">
<table class="tblist" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<th class="sel">序号</th>
		<th>标题</th>
		<th width="120">创建时间</th>
		<th width="80">类型</th>
		<th width="60">状态</th>
		<!-- <th>排序</th> -->
	    <th width="40">操作</th>
	</tr>
	<logic:iterate id="item" name="list" indexId="num">
	<pg:item>
	<tr class="tr-color-body" onmousemove="this.style.background='#EEE'" onmouseout="this.style.background='#FFF'">
	    <td class="numbers"><%=number++ %></td>
	    <td align="center">
	     	<bean:write name="item" property="title"/>
	    </td>
	    <td align="center">
	     	<bean:write name="item" property="createTime" format="yyyy-MM-dd HH:mm"/>
	    </td>
	    <td align="center">
	    	<c:if test="${item.type == '3'}">
	    		首页公告
	    	</c:if>
	    	<c:if test="${item.type == '4'}">
	    		充值公告
	    	</c:if>
	    </td>
	    <td align="center">
	    	<c:if test="${item.status == '1'}">
				<input type="checkbox" checked name="switch" lay-skin="switch" lay-text="开|关" lay-filter="statusSwitch" value="1" data="<bean:write name="item" property="id"/>" id="status<bean:write name="item" property="id"/>"/>
			</c:if>
			<c:if test="${item.status == '0'}">
				<input type="checkbox" name="switch" lay-skin="switch" lay-text="开|关" lay-filter="statusSwitch" value="0" data="<bean:write name="item" property="id"/>" id="status<bean:write name="item" property="id"/>"/>
			</c:if>
	    </td>
<!-- 	    <td align="center"> -->
<%-- 	    	（<bean:write name="item" property="notice.sort"/>） --%>
<%-- 	    	<a href="javascript:if(confirm('确实要向上吗？')){window.location='noticeAction.do?method=sotr&id=<bean:write name="item" property="notice.id"/>&adType=<bean:write name="noticeForm" property="adType"/>&flag=1'}" class="button">上</a> --%>
<%-- 	    	<a href="javascript:if(confirm('确实要向下吗？')){window.location='noticeAction.do?method=sotr&id=<bean:write name="item" property="notice.id"/>&adType=<bean:write name="noticeForm" property="adType"/>&flag=0'}" class="button">下</a> --%>
<!-- 	    </td> -->
		<td align="center">
			<a href='javascript:void(0);' onclick="modify(<bean:write name="item" property="id"/>);"  ><img class="xtbImg" src="../images/modify.png"/></a>
		</td>
	</tr>
	</pg:item>
	</logic:iterate>
</table>
</form>
</logic:notEmpty>
<logic:empty name="list">
<div class="tbl-no-data">暂无数据！</div>
</logic:empty>

<logic:greaterThan name="totalLength" value="<%=maxPageItems+""%>">
	<jsp:include page="/template/pagination_template.jsp" flush="true"/>	    		
</logic:greaterThan>
</pg:pager>
<script type="text/javascript">
layui.use('element', function(){
});
layui.use('form', function(){
	var form = layui.form;
	form.on('switch(statusSwitch)', function(data){
		var loadObj = layer.load();
		var obj=data.elem;
		var id=$(obj).attr('data');
		var checked=data.elem.checked;		
		var val='0';
		if(checked){
			val='1';
		}
		$.post("noticeAction.do?method=changeStatus", 
			{'id':id},
			function(data){
				layer.close(loadObj);
				var code=data.code;
				if(code=='200'){
					layer.close(loadObj);
					$('#status'+id).val(val);
				}else{
					layer.close(loadObj);
					//失败的话
					if(val=='1'){
						$('#status'+id).val('0');
						$('#status'+id).removeAttr('checked');
					}else{
						$('#status'+id).val('1');
						$('#status'+id).attr('checked',true);
					}
					form.render('checkbox');
					layer.alert(data.msg);
				}
		}, "json");
	});  
});
</script>
</body>
</html:html>