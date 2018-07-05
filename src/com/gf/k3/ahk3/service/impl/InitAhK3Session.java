package com.gf.k3.ahk3.service.impl;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.apps.Constants;
import com.apps.eff.GameHelpUtil;
import com.framework.util.DateTimeUtil;
import com.gf.k3.ahk3.service.IAhK3Service;

public class InitAhK3Session extends QuartzJobBean{
	private static IAhK3Service gfAhK3Service;
	protected final Log log = LogFactory.getLog(getClass());
	
	public static IAhK3Service getGfAhK3Service() {
		return gfAhK3Service;
	}
	
	@SuppressWarnings("static-access")
	public void setGfAhK3Service(IAhK3Service gfAhK3Service) {
		this.gfAhK3Service = gfAhK3Service;
	}

	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_GF_AHK3);
		if(!Constants.getTimerOpen(gameCode.substring(2)+".initSession.gf")) return;
		long t1 = System.currentTimeMillis();
		
		Date now = DateTimeUtil.getJavaUtilDateNow();//当前时间
		Date next = DateTimeUtil.getDateTimeOfDays(now, 1);//下一天
		String initNextDay = DateTimeUtil.DateToString(next);
		gfAhK3Service.updateInitSession(null);//今天
		gfAhK3Service.updateInitSession(initNextDay);//下一天
		
		GameHelpUtil.log(gameCode,"init session["+(System.currentTimeMillis()-t1)+"ms]");
	}
}
