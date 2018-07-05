package com.xy.ssc.bjssc.service.impl;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.apps.Constants;
import com.apps.eff.CacheUtil;
import com.apps.eff.GameHelpUtil;
import com.apps.eff.dto.SessionItem;
import com.apps.model.Param;
import com.framework.util.DateTimeUtil;
import com.framework.util.ParamUtils;
import com.game.GameConstants;
import com.game.model.GaSessionInfo;
import com.xy.ssc.bjssc.model.BjSscGaSession;
import com.xy.ssc.bjssc.service.IBjSscService;
import com.xy.ssc.bjssc.util.BjSscUtil;


public class FetchBjSscResult   extends QuartzJobBean {

	private static IBjSscService bjSscService;
	protected final Log log = LogFactory.getLog(getClass());
	
	public static IBjSscService getBjSscService() {
		return bjSscService;
	}
	
	@SuppressWarnings("static-access")
	public void setBjSscService(IBjSscService bjSscService) {
		this.bjSscService = bjSscService;
	}

//	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
//		if(!Constants.getTimerOpen("bjssc.fetchResult.xy")) return;
//		
//    	try {
//    		Param param=CacheUtil.getParam(Constants.PARAM_BJSSC_OPEN_NUM);
//    		if(param!=null&&param.getValue()!=null){
//    			Integer num=Integer.parseInt(param.getValue());
//    			if(num>0){
//    				BjSscGaSession session = bjSscService.getCurrentSession();
//    				if(!ParamUtils.chkString(session.getEstimateResult())){
//    					session.setEstimateResult(BjSscUtil.getRandomResult(5));
//    					bjSscService.saveObject(session, null);
//    				}
//    			}
//    		}
//    		bjSscService.updateFetchAndOpenResult();	
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	//##定时接口-----------------------------------------------------------------------------------------
	//定时抓取-由SpringXML配置定时器启动
	//启动一个Timer，间隔xx秒执行一次抓取,直到开奖完成，如果一直没有完成，设定一个最大抓取次数
	Timer fetchTimer = null;//抓取定时器
	int fetchCounter = 0;//抓取计数器
	int fetchMaxCount = GameHelpUtil.getFetchMaxCount(Constants.GAME_TYPE_XY_BJSSC);//最大抓取次数 
	int fetchDiffTime = GameHelpUtil.getFetchInterval(Constants.GAME_TYPE_XY_BJSSC);//抓取间隔毫秒
	int delayOpenTime = GameHelpUtil.getDelayOpenTime(Constants.GAME_TYPE_XY_BJSSC);//延迟开奖秒数(系统开)
	String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_BJSSC);
	
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		if(!Constants.getTimerOpen(gameCode.substring(2)+".fetchResult.xy")) return;//彩种定时器开关
		
		//定时有效范围
		if(!GameHelpUtil.checkTimerRange(Constants.GAME_TYPE_XY_BJSSC)){
			GameHelpUtil.log(gameCode, "timer not range .....");
			timerClear();
			return;
		}
		
		//重置定时
		fetchTimer = new Timer();
		GameHelpUtil.log(gameCode,"timer launch .....");
		GameHelpUtil.setFetchTimerMap(Constants.GAME_TYPE_XY_BJSSC, fetchTimer);
		TimerTask task = new TimerTask() {
	        @Override
	        public void run() {
	        	GaSessionInfo gsi = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_XY_BJSSC);//彩种缓存
	        	try {
	        		//当前系统时间
	        		Date now = DateTimeUtil.getJavaUtilDateNow();
	        		//当前时间期
	        		BjSscGaSession curSession = bjSscService.getCurrentSession();
	        		if(curSession==null){
	        			GameHelpUtil.log(gameCode,"not found current session, check session init...");
	        			timerClear();
	        			return;
	        		}
	        		//待开奖期
	        		BjSscGaSession openSession = (BjSscGaSession)bjSscService.getObject(BjSscGaSession.class,curSession.getSessionId()-1);
	        		if(openSession==null){
	        			GameHelpUtil.log(gameCode,"not found opening session, check session init...");
	        			timerClear();
	        			return;
	        		}
	        		//待开奖期号
	        		String openSessionNo = openSession.getSessionNo();
	        		//是否已开奖
	        		if(openSession.getOpenStatus().equals(GameConstants.OPEN_STATUS_OPENED)){
	        			GameHelpUtil.log(gameCode,"Opened   ..... ["+openSessionNo+"],openTime="+DateTimeUtil.DateToStringAll(openSession.getOpenTime()));
	        			timerClear();
	        			return;
	        		}
	        		//待开奖期标准开奖时间
	        		Date openTimeNormal = openSession.getEndTime();
	        		if(now.compareTo(openTimeNormal)<0){//当前时间小于结束时间
	        			GameHelpUtil.log(gameCode,"waiting ***** ["+openSessionNo+"],endTime="+DateTimeUtil.DateToStringAll(openTimeNormal));
	        			timerClear();
	        			return;
	        		}
//	        		//待开奖期实际延迟开奖时间
//	        		Date openTimeActual = DateTimeUtil.getDateTimeOfSeconds(openTimeNormal,delayOpenTime);
	        		fetchCounter++;
	        		//GameHelpUtil.log(gameCode,"fetching ..... ["+openSessionNo+"],openTime="+DateTimeUtil.DateToStringAll(openTimeActual)+",delay="+delayOpenTime+"["+fetchCounter+"/"+fetchMaxCount+"]");
	        		//超出最大次数停止
	        		if(fetchCounter>fetchMaxCount){
	        			GameHelpUtil.log(gameCode,"fetch over maxCount ["+fetchCounter+">"+fetchMaxCount+"]");
	        			timerClear();
	        			return;
	        		}
//	        		if(now.compareTo(openTimeActual)<0){//当前时间小于开奖时间不开奖
//	        			//GameHelpUtil.log(gameCode,"waiting  ..... ["+openSessionNo+"],nowTime ="+DateTimeUtil.DateToStringAll(now));
//	        			return;
//	        		}
	        		//检查开奖期只被执行一次
	        		boolean opening = GameHelpUtil.checkOpening(gsi.getGameType(), openSessionNo);
	        		GameHelpUtil.log(gameCode,"fetching ..... ["+openSessionNo+"][ing="+opening+"],openTime="+DateTimeUtil.DateToStringAll(openTimeNormal)+"["+fetchCounter+"/"+fetchMaxCount+"]");
	        		if(!opening){
	        			//获取开奖结果
	        			//Map<String, SessionItem> sessionNoMap = GameHelpUtil.getOpenResultSys(gsi, openSessionNo, openTimeActual);
	        			Map<String, SessionItem> sessionNoMap = GameHelpUtil.getOpenResultThird(gsi, openSessionNo);
	        			if(sessionNoMap!=null && !sessionNoMap.isEmpty()){
	        				//设置开奖中标志
		        			GameHelpUtil.setOpening(gsi.getGameType(), openSessionNo);
		        			timerClear();//获取到结果停止定时器
		        			long startTiming = System.currentTimeMillis();
	        				String flag = bjSscService.updateFetchAndOpenResult(sessionNoMap);
	        				if(flag.startsWith("success")){
	        					GameHelpUtil.removeOpening(Constants.GAME_TYPE_XY_BJSSC,openSessionNo);//移除开奖标识
	        					GameHelpUtil.log(gameCode,"SUCCESS  _^_^_ ["+openSessionNo+"]["+(System.currentTimeMillis()-startTiming)+"ms]");
	        				}else{
	        					GameHelpUtil.log(gameCode,flag);
	        				}
	        				timerClear();
		        			return;
		        		}else{
		        			//GameHelpUtil.log(gameCode,"FAIL     _-_-_ ");
		        		}
	        		}else{
	        			GameHelpUtil.log(gameCode,"opening NOW ..... ["+openSessionNo+"]");
	        			timerClear();
	        			return;
	        		}
	        	} catch (Exception e) {
	        		GameHelpUtil.log(gameCode,"fetch ERROR ["+e.getMessage()+"]");
	    		}
	        }
	    };
	    fetchTimer.schedule(task, 0, fetchDiffTime);//开始定时抓取任务
	}
	/**
	 * 清除定时器
	 */
	protected void timerClear(){
		fetchCounter = 0;
		delayOpenTime = 0;
		if(fetchTimer!=null){
			fetchTimer.cancel();
			fetchTimer = null;
			GameHelpUtil.log(gameCode,"timer clear .....");
		}
	}
	//------------------------------------------------------------------------------------------------

}
