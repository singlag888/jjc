<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ include file ="/common/inc_include.jsp"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ include  file="../common/inc_datepicker.jsp" %>  
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<html:html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>管理平台</title>
<%@ include file = "../common/inc_file.jsp"%>
<script language="javascript">
function formSubmit(){
	var frm = document.paramForm;
	if(frm["param.value"].value.trim().length==0){
		alert("请输入取值");
		frm["param.value"].focus();
		return false;
	}
	if(frm["param.type"].value=="int"){
	    if(!isInt(frm["param.value"].value)){
		alert("请输入整数!");
		frm["param.value"].focus();
		return false;
		}
	}
	
	if(confirm("确认保存？")){
		return true;
	}
	return false;
}
function  isInt(str) {
     var re = /^[0-9]*[1-9][0-9]*$/;
    return re.test(str);
}


function changeStatus(id,status){
	$.post("paramAction.do?method=changeStatus&paramId="+ id , 
		function(data) {
			data = $.trim(data);
			if (data == "success") {
				if(status=='1'){
					$('#status'+id).html('<font color="green"><strong>图形验证码注册</strong></font>&nbsp;&nbsp;<a class="button" href="javascript:void(0);" onclick="changeStatus('+id+',\'0\');">手机验证码注册</a>');
				}else{
					$('#status'+id).html('<font color="green"><strong>手机验证码注册</strong></font>&nbsp;&nbsp;<a class="button" href="javascript:void(0);" onclick="changeStatus('+id+',\'1\');">图形验证码注册</a>');
				}
			}
	});	
}

function changeValue(id,status){
	$.post("paramAction.do?method=changeValue&paramId="+ id , 
		function(data) {
			data = $.trim(data);
			if (data == "success") {
				if(status=='1'){
					$('#status'+id).html('<font color="green"><strong>图形验证码注册</strong></font>&nbsp;&nbsp;<a class="button" href="javascript:void(0);" onclick="changeValue('+id+',\'0\');">手机验证码注册</a>');
				}else{
					$('#status'+id).html('<font color="green"><strong>手机验证码注册</strong></font>&nbsp;&nbsp;<a class="button" href="javascript:void(0);" onclick="changeValue('+id+',\'1\');">图形验证码注册</a>');
				}
			}
	});	
}


window.onload=function(){
}
</script>
</head>
<body>
<div class="bread">系统设置  &raquo;  注册类型设置</div>
<div  style="width:65%">
<table class="tblist" cellpadding="0" cellspacing="0" border="0" >
	<tr>
		<th>名称</th>
		<th>设置</th>
	</tr>
	<tr class="tr-color-body" onmousemove="this.style.background='#EEE'" onmouseout="this.style.background='#FFF'">
	    <td align="center">
	     	<bean:write property="title" name="param"/>
	    </td>
		<td align="center" id="status<bean:write name="param" property="paramId"/>">
					 <logic:equal name="param" property="value" value="1">
						<font color="green"><strong>手机验证码注册</strong></font>&nbsp;&nbsp;&nbsp;&nbsp;
						<a class="button" onclick="changeValue(<bean:write name="param" property="paramId"/>,'1');" href='javascript:void(0);'>图形验证码注册</a>&nbsp;
					</logic:equal>
					<logic:equal name="param" property="value" value="0">
						<font color="green"><strong>图形验证码注册</strong></font>&nbsp;&nbsp;&nbsp;&nbsp;
						<a class="button" onclick="changeValue(<bean:write name="param" property="paramId"/>,'0');" href="javascript:void(0);">手机验证码注册</a>&nbsp;
					</logic:equal>
	    </td>
	</tr>
</table>
</div>
</body>
</html:html>