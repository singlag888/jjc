package com.xy.ssc.five.web.action;

import help.base.APIConstants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.json.JSONObject;

import com.apps.Constants;
import com.apps.util.JsonUtil;
import com.framework.dao.hibernate.PaginationSupport;
import com.framework.util.DateTimeUtil;
import com.framework.util.HQUtils;
import com.framework.util.ManagerUtils;
import com.framework.util.ParamUtils;
import com.framework.web.action.BaseDispatchAction;
import com.game.service.IGaService;
import com.ram.RamConstants;
import com.ram.exception.permission.NoFunctionPermissionException;
import com.ram.model.User;
import com.xy.pk10.bjpk10.BjPk10Constants;
import com.xy.pk10.jsft.model.dto.JsftDTO;
import com.xy.ssc.cqssc.model.CqSscGaSession;
import com.xy.ssc.five.model.FiveGaBet;
import com.xy.ssc.five.model.FiveGaSession;
import com.xy.ssc.five.model.dto.FiveDTO;
import com.xy.ssc.five.service.IFiveService;
import com.xy.ssc.five.web.form.FiveForm;

public class FiveAction extends BaseDispatchAction{
	private final IFiveService fiveService = (IFiveService) getService("fiveService");
	private final IGaService gaService = (IGaService) getService("gaService");
	/**
	 * 列表
	 */
	public ActionForward init(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {
		int startIndex = ParamUtils.getIntParameter(request, "pager.offset", 0);
		int pageSize = ParamUtils.getIntParameter(request, "maxPageItems",
				RamConstants.MAXPAGEITEMS);
		List<Object> para = new ArrayList<Object>();
		StringBuffer hqls = new StringBuffer();

		FiveForm fiveForm = (FiveForm) form;
		String startDate = fiveForm.getStartDate();
		String endDate = fiveForm.getEndDate();
		String sessionNo = fiveForm.getSessionNo();
		String status= fiveForm.getStatus();
		
		if (ParamUtils.chkString(sessionNo)) {
			hqls.append(" and ho.sessionNo like ?");
			para.add("%"+sessionNo+"%");
		}
		
		User user = getUser(request);
		String userType = user.getUserType();
		FiveGaSession session = fiveService.getCurrentSession();
		if (session != null) {
			Integer sesionId = 	session.getSessionId();	
			hqls.append(" and ho.sessionId > ? ");
			para.add(sesionId-5);
			hqls.append(" and ho.sessionId < ? ");
			para.add(sesionId+5);
		} else {
			hqls.append(" and ho.openStatus = ? ");
			para.add(Constants.OPEN_STATUS_INIT);
		}
		hqls.append(" order by ho.sessionNo desc ");
		PaginationSupport ps = fiveService.findFiveGaSessionList(hqls.toString(), para,
				startIndex, pageSize);
		List list = ps.getItems();
		request.setAttribute("list", list);
		request.setAttribute("status", status);
		request.setAttribute("count", (ps.getTotalCount() + "").toString());
		fiveForm.setStartIndex(startIndex + "");
		return mapping.findForward("init");
	}
	

	/**
	 * 保存开奖结果
	 */
	public void saveOpenResult(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {
		Integer sessionId = ParamUtils.getIntegerParameter(request, "sessionId");
		String openResult = ParamUtils.getParameter(request, "openResult");
	
		FiveGaSession session = (FiveGaSession)fiveService.getObject(FiveGaSession.class, sessionId);
		String flag = "success";
		String message="";
		if(session!=null&&session.getOpenStatus().equals(BjPk10Constants.BJ_PK10_OPEN_STATUS_INIT)){
			if(ParamUtils.chkString(session.getOpenResult())){
				message="已保存";
				flag = "success";
			}else{
				boolean result=fiveService.saveOpenResult(session,openResult);
				if(result){
					message="保存成功";
					flag = "success";
				}else{
					message="保存出错";
					flag = "false&";
				}
			}		
		}else{
			message="期号不存在或者已开奖";
			flag = "false";
		}
		JsonUtil.AjaxWriter(response, flag);
	}
	
	/**
	 * 保存并开奖
	 */
	public void openResult(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {
		Integer sessionId = ParamUtils.getIntegerParameter(request, "sessionId");
		String openResult = ParamUtils.getParameter(request, "openResult");
		FiveGaSession session = (FiveGaSession)fiveService.getObject(FiveGaSession.class, sessionId);
		String flag = "success";
		String message="";
		User loginUser = (User) request.getSession(false).getAttribute("loginUser");//登录的用户
		String loginUserType = loginUser.getUserType();//登录用户类型
//		if(Constants.USER_TYPE_ADMIN.equals(loginUserType)
//				||Constants.USER_TYPE_SUPERADMIN.equals(loginUserType)){
			if(session!=null&&session.getOpenStatus().equals(BjPk10Constants.BJ_PK10_OPEN_STATUS_INIT)){
				boolean result=fiveService.saveAndOpenResult(session,openResult);
				if(result){
					message="开奖成功";
					flag = "success";
					
					StringBuffer loginText = new StringBuffer();
					loginText.append("五分彩手动开奖：操作人");
					loginText.append(loginUser.getLoginName());
					loginText.append("[");
					loginText.append(loginUser.getUserId());
					loginText.append("]，给期号[");
					loginText.append(session.getSessionNo());
					loginText.append("]开奖，开奖结果为[");
					loginText.append(openResult.trim());
					loginText.append("]");
					userService.updateUserLog(request,loginUser,loginText.toString());
					
				}else{
					message="开奖失败";
					flag = "false&";
				}
			}else{
				message="期号不存在或者已开奖";
				flag = "false";
			}
//		}else{
//			message="暂无权限，请联系管理员";
//			flag = "false";
//		}
		JsonUtil.AjaxWriter(response, flag);
	}
	
	public ActionForward betList(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception{
		int startIndex = ParamUtils.getIntParameter(request, "pager.offset", 0);
		int pageSize = ParamUtils.getIntParameter(request, "maxPageItems", RamConstants.MAXPAGEITEMS);
		List<Object> para = new ArrayList<Object>();
		StringBuffer hqls = new StringBuffer();

		FiveForm fiveForm = (FiveForm) form;
		String sessionNo = fiveForm.getSessionNo();
		String startDate = fiveForm.getStartDate();
		String endDate = fiveForm.getEndDate();

		if (ParamUtils.chkString(sessionNo)) {
			hqls.append(" and ho.sessionNo like ?");
			para.add("%"+sessionNo+"%");
		}
		if (ParamUtils.chkString(startDate)){
			hqls.append(" and ho.startTime >= ? ");
			para.add(DateTimeUtil.parse(startDate + " 00:00:00"));
		}
		if (ParamUtils.chkString(endDate)){
			hqls.append(" and ho.endTime <= ? ");
			para.add(DateTimeUtil.parse(endDate + " 23:59:59"));
		}
		User user = getUser(request);
		String userType = user.getUserType();
		hqls.append(" order by ho.sessionId desc ");
		PaginationSupport ps = fiveService.findFiveGaBetList(hqls.toString(), para,
				startIndex, pageSize);
		List list=ps.getItems();
		
		HQUtils hq2 = new HQUtils();
		hq2.addHsql("select new com.xy.ssc.five.model.dto.FiveDTO(sum(ho.totalPoint),sum(ho.winCash),sum(ho.totalPoint)-sum(ho.winCash)) from FiveGaBet ho where 1=1 ");
		if (ParamUtils.chkString(startDate)){
			hq2.addHsql(" and ho.startTime >= ? ");
			hq2.addPars(DateTimeUtil.parse(startDate + " 00:00:00"));
		}
		if (ParamUtils.chkString(endDate)){
			hq2.addHsql(" and ho.endTime <= ? ");
			hq2.addPars(DateTimeUtil.parse(endDate + " 23:59:59"));
		}
		FiveDTO dto = (FiveDTO) fiveService.getObject(hq2);
		fiveForm.setFiveDto(dto);
		
		request.setAttribute("list", list);
		request.setAttribute("sessionNo", sessionNo);
		request.setAttribute("count", (ps.getTotalCount() + "").toString());
		fiveForm.setStartIndex(startIndex + "");
		return mapping.findForward("betList");
	}
	
	/**
	 * 个人投注记录
	 */
	public ActionForward betManager(ActionMapping mapping, ActionForm form,
			HttpServletRequest request,HttpServletResponse response)throws Exception{
		int startIndex = ParamUtils.getIntParameter(request, "pager.offset", 0);
		int pageSize = ParamUtils.getIntParameter(request, "maxPageItems", RamConstants.MAXPAGEITEMS);
		List<Object> para = new ArrayList<Object>();
		StringBuffer hqls = new StringBuffer();

		FiveForm fiveForm = (FiveForm) form;
		String userName = fiveForm.getUserName();
		String sessionNo = fiveForm.getSessionNo();

		if (ParamUtils.chkString(sessionNo)) {
			hqls.append(" and upper(ga.sessionNo) like ? ");
			para.add("%"+ sessionNo.trim().toUpperCase() +"%");
		}
		if (ParamUtils.chkString(userName)){
			hqls.append(" and (upper(u.userName) = ? OR upper(u.userId) = ? OR upper(u.loginName) = ? ) ");
			para.add(userName.trim().toUpperCase());
			para.add(userName.trim().toUpperCase());
			para.add(userName.trim().toUpperCase());
		}
		User user = getUser(request);
		String userType = user.getUserType();
//		if (userType.equals(Constants.USER_TYPE_ADMIN)) {
	        hqls.append(" and upper(ga.gameType) = ? ");
	        para.add(Constants.GAME_TYPE_XY_FC);
			PaginationSupport ps = fiveService.findGaBetDetail(hqls.toString(), para,
					startIndex, pageSize);
			List list = new ArrayList();
			if(ps != null){
				list = ps.getItems();
			}
			request.setAttribute("list", list);
			request.setAttribute("sessionNo", sessionNo);
			request.setAttribute("endDate", userName);
			request.setAttribute("count", (ps.getTotalCount() + "").toString());
			fiveForm.setStartIndex(startIndex + "");
//		}

		return mapping.findForward("betManager");
	}
	
	/**
	 * 投注详情
	 */
	public ActionForward betDetailManager(ActionMapping mapping,ActionForm form,
			HttpServletRequest request,HttpServletResponse response)throws Exception{
		List<Object> para = new ArrayList<Object>();
		StringBuffer hqls = new StringBuffer();
        String betDetailId = request.getParameter("betDetailId");
        
		if(ParamUtils.chkString(betDetailId)){
			hqls.append(" and upper(ga.betDetailId) like ? ");
			para.add("%" + betDetailId.trim().toUpperCase() + "%");
		}
		List<FiveDTO> list = new ArrayList();
		list = fiveService.findGaBetDetailById(hqls.toString(), para);
		FiveDTO five = new FiveDTO();
        if(list != null && list.size() > 0){
        	five = list.get(0);
        }
		
		request.setAttribute("item", five);
		return mapping.findForward("betDetailManager");

	}
	public void initSessionNo(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {

		String flag = "success";
//		String message="";
		flag=fiveService.updateInitSession(0);
		if(flag.equals("success")||flag.equals("inited")){
			fiveService.updateInitSession(1);
		}
		JsonUtil.AjaxWriter(response, flag);
	}
	
	/**
	 * 退还未开奖的钱
	 */
	public void drawback(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {
		Integer sessionId = ParamUtils.getIntegerParameter(request, "sessionId");
		FiveGaSession session = (FiveGaSession)fiveService.getObject(FiveGaSession.class, sessionId);
		String flag = "success";
		User loginUser = (User) request.getSession(false).getAttribute("loginUser");//登录的用户
		String loginUserType = loginUser.getUserType();//登录用户类型
//		if(Constants.USER_TYPE_ADMIN.equals(loginUserType)
//				||Constants.USER_TYPE_SUPERADMIN.equals(loginUserType)){
			if(session!=null&&session.getOpenStatus().equals(BjPk10Constants.BJ_PK10_OPEN_STATUS_INIT)){
				boolean result=gaService.saveDrawback(session.getSessionId(),Constants.GAME_TYPE_XY_FC);
				if(result){	
					session.setOpenStatus("3");//退款
					gaService.saveObject(session, null);
					
					StringBuffer loginText = new StringBuffer();
					loginText.append("五分彩手动退款：操作人");
					loginText.append(loginUser.getLoginName());
					loginText.append("[");
					loginText.append(loginUser.getUserId());
					loginText.append("]，给期号[");
					loginText.append(session.getSessionNo());
					loginText.append("]退款，时间为[");
					loginText.append(DateTimeUtil.DateToStringAll(new Date()));
					loginText.append("]");
					userService.updateUserLog(request,loginUser,loginText.toString());
					
				}else{
					flag = "false";	
				}
			}else{
				flag = "false";
			}
//		}else{
//			flag = "false";
//		}
		JsonUtil.AjaxWriter(response, flag);
	}	
	/**
	 * 撤回已经派彩的金额
	 */
	public void revokePrize(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception, NoFunctionPermissionException {
		User loginUser = (User) request.getSession(false).getAttribute("loginUser");//登录的用户
		String loginUserType = loginUser.getUserType();//登录用户类型
		JSONObject map = new JSONObject();// 最外层
		String code = "";
		String msg = "";

		//管理员和超级管理员才能查询
		if(ManagerUtils.checkRole(loginUserType)){
			Integer sessionId = ParamUtils.getIntegerParameter(request, "sessionId");
			if(ParamUtils.chkInteger(sessionId)){
				FiveGaSession session = (FiveGaSession)userService.getObject(FiveGaSession.class, sessionId);
				if(session!=null&&session.getOpenStatus().equals(Constants.OPEN_STATUS_OPENED)){
					boolean result = fiveService.saveRevokePrize(session);
					if(result){
						if(!ManagerUtils.checkHidden(loginUserType)){
							StringBuffer loginText = new StringBuffer();
							loginText.append("撤回派彩：操作人");
							loginText.append(loginUser.getLoginName());
							loginText.append("[");
							loginText.append(loginUser.getUserId());
							loginText.append("]，给期号[");
							loginText.append(session.getSessionNo());
							loginText.append("]撤回错误派彩金额。");
							userService.updateUserLog(request,loginUser,loginText.toString());
						}
						msg="撤回派彩成功";
						code = APIConstants.CODE_REQUEST_SUCCESS;
					}else{
						msg="撤回派彩失败";
						code = APIConstants.CODE_SERVER_ERROR;
					}
				}else{
					msg="期号不存在或者未开奖！";
					code = APIConstants.CODE_REQUEST_ERROR;
				}
			}else{
				msg = APIConstants.PARAMS_EMPTY_MSG;
				code = APIConstants.CODE_REQUEST_ERROR;
			}
		}else{
			msg="无权限！请联系管理员！";
			code = APIConstants.CODE_UNAUTHORIZED_ERROR;
		}
		map.put("code", code);
		map.put("msg", msg);
		JsonUtil.AjaxWriter(response, map);
	}
}
