<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ include file ="../common/inc_include.jsp"%>
<%@ include file = "../common/inc_pageSetting.jsp"%>
<%@ include  file="../common/inc_datepicker.jsp" %> 
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>远程网络教学平台</title>
<%@ include file = "../common/inc_file.jsp"%>
<script src="../js/manager.js?t=3" type="text/javascript"></script>
<script type="text/javascript">
$(document).ready(function(){
	managerObj.drawingResult();
});
function changeSchedule1(type){
	if(type!=''){
		 $.post("agentAction.do?method=findSecondGaOptionList&gameType="+type, 
					function(data){
					    changeTypeList1(data);
				});
	}else{
		var shijiSelect = document.all('optionTitle');
	    shijiSelect.innerHTML="";
		
	 	var option0 = document.createElement("OPTION");
	 	option0.innerHTML = "全部";
	 	option0.value = "";
	 	shijiSelect.appendChild(option0);
	}
}
function changeTypeList1(data){
    var jsonData = JSON.parse(data);
    var code = jsonData.code;
    if (code == "200") {
   	    var shijiSelect = document.all('optionTitle');
	    shijiSelect.innerHTML="";
		
	 	var option0 = document.createElement("OPTION");
	 	option0.innerHTML = "全部";
	 	option0.value = "";
	 	shijiSelect.appendChild(option0);
	    
		var data=jsonData.data;
		var items=data.items;
    	for(var i = 0; i < items.length; i++){
			var option = document.createElement("OPTION");
			var obj=items[i];
        	option.innerHTML = obj.title;
       		option.value = obj.title;
  			shijiSelect.appendChild(option);
    	}
    }
}

</script>
</head>
<body>
<html:form action="/agentAction.do?method=betDetailList" method="post">
<table class="tblist" cellpadding="0" cellspacing="0" border="0">
	<tr class="moon">
		<td>
			<span>开始时间：<html:text property="startDate" name="agentForm" styleClass="Wdate" styleId="startTime"   onclick="WdatePicker({lang:'zh-cn',dateFmt: 'yyyy-MM-dd HH:mm:ss'})" readonly="readonly"/></span>
			<span>结束时间：<html:text property="endDate" name="agentForm" styleClass="Wdate" styleId="endDate"  onclick="WdatePicker({lang:'zh-cn',dateFmt: 'yyyy-MM-dd HH:mm:ss'})" readonly="readonly"/></span>
			<span>
			状态： 
		    <html:select property="winResult" name="agentForm" styleClass="sele select">
			    <html:option value="">全部</html:option>
			    <html:option value="0">未开奖</html:option>
			    <html:option value="2">未中奖</html:option>
			    <html:option value="1">中奖</html:option>
			</html:select>
			</span>
			<span>用户名/ID：<html:text property="loginName" name="agentForm"/></span>
			<span>
			<input type="submit" class="gbutton" value="查询" />
		</td>
	</tr>
</table>
</html:form>	

<%
//分页
String totalCount =request.getAttribute("count")!=null?(String)request.getAttribute("count"):"0";
String startDate =request.getAttribute("startDate")!=null?(String)request.getAttribute("startDate"):"";
String endDate =request.getAttribute("endDate")!=null?(String)request.getAttribute("endDate"):"";
String loginName =request.getAttribute("loginName")!=null?(String)request.getAttribute("loginName"):"";
String winResult =request.getAttribute("winResult")!=null?(String)request.getAttribute("winResult"):"";
String gameType =request.getAttribute("gameType")!=null?(String)request.getAttribute("gameType"):"";
String optionTitle =request.getAttribute("optionTitle")!=null?(String)request.getAttribute("optionTitle"):"";

%>
<pg:pager 
	url="agentAction.do" 
	index="<%=index%>"
	items="<%=Integer.parseInt(totalCount)%>"
	maxPageItems="<%= maxPageItems %>"
	maxIndexPages="<%= maxIndexPages %>"
	isOffset="<%= isOffset %>"
	export="offset,currentPageNumber=pageNumber"
	scope="request"> 
	<pg:param name="method" value="betDetailList"/>
	<pg:param name="startDate" value="<%=startDate%>"/>
	<pg:param name="endDate" value="<%=endDate%>"/>
	<pg:param name="loginName" value="<%=loginName%>"/>
	<pg:param name="winResult" value="<%=winResult%>"/>
	<pg:param name="gameType" value="<%=gameType%>"/>
	<pg:param name="optionTitle" value="<%=optionTitle%>"/>
	<bean:define id="totalLength" value="<%=totalCount%>"/>
<bean:size id="length" name="list"/>
<logic:notEmpty name="list">
<table class="tblist" border="0" cellpadding="0" cellspacing="0">
	<tr>
		<th class="sel">序号</th>
		<th>用户ID</th>
		<th>用户名</th>
		<th>分类</th>
	    <th>投注时间</th>
	    <th>赛事</th>
	    <th>比赛局</th>
	    <th>投注项</th>
	    <th>开奖结果</th>
	    <th>投注值</th>
	    <th>赔率</th>
	    <th>中奖状态</th>
	    <th>亏盈</th>
	</tr>
	<logic:iterate id="item" name="list" indexId="num">
	<pg:item>
	<tr onmousemove="this.style.background='#EEEEEE'" onmouseout="this.style.background='#ffffff'">
<%-- 		<td class="numbers"><%=num.intValue()+1 %></td> --%>
		<td align="center">
		    <bean:write name="item" property="gaBetDetail.betDetailId"/>
		</td>
	    <td align="center">
	        <bean:write name="item" property="user.userId" />
	    </td>
	    <td align="center">
	        <bean:write name="item" property="user.loginName" />
	    </td>
	    <td align="center">
		    <bean:write name="item" property="gaBetDetail.gameName"/>
		</td>
	    <td align="center">
	      	<bean:write name="item" property="gaBetDetail.betTime" format="yyyy-MM-dd HH:mm:ss"/>
		</td>
		<td align="center">
	    	<bean:write name="item" property="gaBetDetail.playName"/>
	    </td>
	    <td align="center">
	    	<bean:write name="item" property="gaBetDetail.room"/>
	    </td>
	    <td style="word-break:break-all;" align="center">
	    	<bean:write name="item" property="gaBetDetail.betName"/>&nbsp;
	    	<bean:write name="item" property="gaBetDetail.optionTitle"/>
	    </td>
	    <td align="center">
	    	<c:if test="${item.gaBetDetail.betId==0}"><input class="result" type="hidden"  value="<bean:write name="item" property="gaBetDetail.openResult"/>" data="<bean:write name="item" property="gaBetDetail.gameType"/>"/></c:if>
	    	<c:if test="${item.gaBetDetail.betId!=0}"><bean:write name="item" property="gaBetDetail.openResult"/></c:if>
	    </td>
	    <td align="center">
	    	<bean:write name="item" property="gaBetDetail.betMoney"/>
	    </td>
	    <td align="center">
	    	<bean:write name="item" property="gaBetDetail.betRate"/>
	    </td>
	    <td align="center">
	        <logic:equal name="item" property="gaBetDetail.betFlag" value="0">
     			<strong><font color ="purple">撤单</font></strong> 
			</logic:equal>
            <logic:equal name="item" property="gaBetDetail.betFlag" value="1">
     		<c:if test="${item.gaBetDetail.winResult =='0'}">
		    	<strong><font color ="blue">未开奖</font></strong> 
		    </c:if>
	        <c:if test="${item.gaBetDetail.winResult =='1'}">
		    	<strong><font color ="red">中奖</font></strong> 
		    </c:if>
	        <c:if test="${item.gaBetDetail.winResult =='2'}">
		    	<strong><font color ="green">未中奖</font></strong>
		    </c:if>
	        <c:if test="${item.gaBetDetail.winResult =='3'}">
		    	<strong><font color ="purple">打和</font></strong> 
		    </c:if>
			</logic:equal>
	    </td>
	    <td align="center">
	        <bean:write name="item" property="gaBetDetail.winCash" format="#.####"/>
	    </td>
	</tr>
	</pg:item>
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