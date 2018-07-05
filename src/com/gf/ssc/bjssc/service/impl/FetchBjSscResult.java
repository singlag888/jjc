package com.gf.ssc.bjssc.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
import com.framework.util.DateTimeUtil;
import com.framework.util.ParamUtils;
import com.game.GameConstants;
import com.game.model.GaSessionInfo;
import com.gf.ssc.bjssc.model.GfBjSscGaSession;
import com.gf.ssc.bjssc.service.IBjSscService;



/**
 * 定时去官网抓取开奖数据
 * @author hpz
 */
public class FetchBjSscResult extends QuartzJobBean{
    private static IBjSscService gfBjSscService;
    protected final Log log = LogFactory.getLog(getClass());
    
    public static IBjSscService getGfBjSscService(){
    	return gfBjSscService;
    }
    
    @SuppressWarnings("static-access")
    public void setGfBjSscService(IBjSscService gfBjSscService){
    	this.gfBjSscService = gfBjSscService;
    }
    
//    protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
//    	
//    	if(!Constants.getTimerOpen("bjssc.fetchResult.gf")) return;
//    	
//    	log.info("-------重庆时时彩 ---开始开奖---"+DateTimeUtil.DateToStringAll(new Date()));
//    	
//		List<GfBjSscGaSession> list=gfBjSscService.updateAndOpenResult();
//
//		Collections.sort(list,new Comparator<GfBjSscGaSession>(){
//			public int compare(GfBjSscGaSession o1, GfBjSscGaSession o2){
//		    	if(o1.getSessionId() < o2.getSessionId()){return -1;}  
//		    	if(o1.getSessionId() == o2.getSessionId()){return 0;}  
//		    	return 1;
//			}  
//		});  
//
//		List<GfBjSscGaSession> savelist=new ArrayList<GfBjSscGaSession>();
//		for(GfBjSscGaSession session:list){
//			ManageFile.writeTextToFile("[sessionNo]"+session.getSessionNo()+"\n", Constants.getWebRootPath()+"/gamelogo/bjsscjilu.txt", true);
////			List<Object> para = new ArrayList<Object>();
//			
//			gfBjSscService.updateFetchAndOpenTrendResult(session);
//
//			String flag = gfBjSscService.updateBjSscSessionOpenResultMethod(session, session.getOpenResult(),null);
//			
//			if(ParamUtils.chkString(flag)){
//				session.setOpenSuccess(Constants.PUB_STATUS_OPEN);//这些期开奖计算出错
//				savelist.add(session);
//			}else{
//				gfBjSscService.updateDayBetCount(session);
//			}
//			gfBjSscService.updateFetchAndOpenOmit(session);
//		}
//		gfBjSscService.updateObjectList(savelist, null);
//    	log.info("-------重庆时时彩 ---开奖结束---"+DateTimeUtil.DateToStringAll(new Date()));
//    }
    
    //##定时接口-----------------------------------------------------------------------------------------
	//定时抓取-由SpringXML配置定时器启动
	//启动一个Timer，间隔xx秒执行一次抓取,直到开奖完成，如果一直没有完成，设定一个最大抓取次数
	Timer fetchTimer = null;//抓取定时器
	int fetchCounter = 0;//抓取计数器
	int fetchMaxCount = GameHelpUtil.getFetchMaxCount(Constants.GAME_TYPE_GF_BJSSC);//最大抓取次数 
	int fetchDiffTime = GameHelpUtil.getFetchInterval(Constants.GAME_TYPE_GF_BJSSC);//抓取间隔毫秒
	int delayOpenTime = GameHelpUtil.getDelayOpenTime(Constants.GAME_TYPE_GF_BJSSC);//延迟开奖秒数(系统开) 
	String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_GF_BJSSC);
	
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		if(!Constants.getTimerOpen(gameCode.substring(2)+".fetchResult.gf")) return;//彩种定时器开关
		
		//定时有效范围
		if(!GameHelpUtil.checkTimerRange(Constants.GAME_TYPE_GF_BJSSC)){
			GameHelpUtil.log(gameCode, "timer not range .....");
			timerClear();
			return;
		}
		
		GameHelpUtil.log(gameCode,"timer launch .....");
		//清除之前的定时
		Timer preTimer = GameHelpUtil.getFetchTimerMap(Constants.GAME_TYPE_GF_BJSSC);
		if(preTimer!=null){
			preTimer.cancel();
		}
		//重置定时
		fetchTimer = new Timer();
		TimerTask task = new TimerTask() {
	        @Override
	        public void run() {
	        	GaSessionInfo gsi = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_GF_BJSSC);//彩种缓存
	        	try {
	        		//当前系统时间
	        		Date now = DateTimeUtil.getJavaUtilDateNow();
	        		//当前时间期
	        		GfBjSscGaSession curSession = gfBjSscService.getCurrentSession();
	        		if(curSession==null){
	        			GameHelpUtil.log(gameCode,"not found current session, check session init...");
	        			timerClear();
	        			return;
	        		}
	        		//待开奖期
	        		GfBjSscGaSession openSession = (GfBjSscGaSession)gfBjSscService.getObject(GfBjSscGaSession.class,curSession.getSessionId()-1);
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
	        		fetchCounter++;
	        		GameHelpUtil.log(gameCode,"fetching ..... ["+openSessionNo+"],openTime="+DateTimeUtil.DateToStringAll(openTimeNormal)+"["+fetchCounter+"/"+fetchMaxCount+"]");
	        		//超出最大次数停止
	        		if(fetchCounter>fetchMaxCount){
	        			GameHelpUtil.log(gameCode,"fetch over maxCount ["+fetchCounter+">"+fetchMaxCount+"]");
	        			timerClear();
	        			return;
	        		}
	        		//检查开奖期只被执行一次
	        		boolean opening = GameHelpUtil.checkOpening(gsi.getGameType(), openSessionNo);
	        		//GameHelpUtil.log(gameCode,"CHECKING ..... ["+openSessionNo+"],opening="+opening);
	        		if(!opening){
	        			//获取开奖结果
	        			Map<String, SessionItem> sessionNoMap = GameHelpUtil.getOpenResultThird(gsi, openSessionNo);
	        			if(sessionNoMap!=null){
	        				//设置开奖中标志
		        			GameHelpUtil.setOpening(gsi.getGameType(), openSessionNo);
	        				long startTiming = System.currentTimeMillis();
	        				
	        				List<GfBjSscGaSession> list = (List<GfBjSscGaSession>) gfBjSscService.updateAndOpenResult(sessionNoMap);
		        			Collections.sort(list,new Comparator<GfBjSscGaSession>(){
		        				public int compare(GfBjSscGaSession o1, GfBjSscGaSession o2){
		        			    	if(o1.getSessionId() < o2.getSessionId()){return -1;}  
		        			    	if(o1.getSessionId() == o2.getSessionId()){return 0;}  
		        			    	return 1;
		        				}  
		        			});  
		        			List<GfBjSscGaSession> savelist=new ArrayList<GfBjSscGaSession>();
		        			for(GfBjSscGaSession session:list){
		        				gfBjSscService.updateFetchAndOpenTrendResult(session);
		        				String flag = gfBjSscService.updateBjSscSessionOpenResultMethod(session, session.getOpenResult(),null);
		        				if(ParamUtils.chkString(flag)){
		        					session.setOpenSuccess(Constants.PUB_STATUS_OPEN);//这些期开奖计算出错
		        					savelist.add(session);
		        				}else{
		        					gfBjSscService.updateDayBetCount(session);
		        				}
		        				gfBjSscService.updateFetchAndOpenOmit(session);
		        			}
		        			gfBjSscService.updateObjectList(savelist, null);
		        			
	        				GameHelpUtil.log(gameCode,"SUCCESS  _^_^_ ["+openSessionNo+"]["+(System.currentTimeMillis()-startTiming)+"ms]");
		        			timerClear();
		        		}else{
		        			//GameHelpUtil.log(gameCode,"FAIL     _-_-_ ");
		        		}
	        		}else{
	        			GameHelpUtil.log(gameCode,"opening NOW ..... ["+openSessionNo+"]");
	        		}
	        		
	        	} catch (Exception e) {
	        		GameHelpUtil.log(gameCode,"fetch ERROR ["+e.getMessage()+"]");
	    			//e.printStackTrace();
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
		if(fetchTimer!=null) fetchTimer.cancel();
	}
	//------------------------------------------------------------------------------------------------

}
