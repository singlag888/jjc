package com.xy.k10.gxk10.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.apps.Constants;
import com.apps.eff.CacheUtil;
import com.apps.eff.GameHelpUtil;
import com.framework.util.DateTimeUtil;
import com.game.model.GaSessionInfo;
import com.xy.k10.gxk10.service.IGxK10Service;


public class UpdateGxK10GaTrend extends QuartzJobBean{

	private static IGxK10Service gxK10Service;
	protected final Log log = LogFactory.getLog(getClass());
	
	public static IGxK10Service getGxK10Service() {
		return gxK10Service;
	}
	
	@SuppressWarnings("static-access")
	public void setGxK10Service(IGxK10Service gxK10Service) {
		this.gxK10Service = gxK10Service;
	}
	
	protected void executeInternal(JobExecutionContext arg0)throws JobExecutionException {
//		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_GXK10);
//		if(!Constants.getTimerOpen(gameCode.substring(2)+".trend.xy")) return;
//		try {
//			long t1 = System.currentTimeMillis();
//			
//			GaSessionInfo gaSessionInfo = (GaSessionInfo) CacheUtil.getGameMap().get(Constants.GAME_TYPE_XY_GXK10);
//			if(gaSessionInfo != null && Constants.PUB_STATUS_OPEN.equals(gaSessionInfo.getStatus()) && 
//					Constants.PUB_STATUS_OPEN.equals(gaSessionInfo.getBetAvoid())){
//				gxK10Service.updateGaTrend();
//			}
//			
//			GameHelpUtil.log(gameCode,"trend count["+(System.currentTimeMillis()-t1)+"ms]");
//		} catch (Exception e) {
//			GameHelpUtil.log(gameCode,"trend error:"+e.getMessage());
//			//e.printStackTrace();
//		}
	}

}
