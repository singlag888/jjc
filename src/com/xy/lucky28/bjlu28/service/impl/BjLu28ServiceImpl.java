package com.xy.lucky28.bjlu28.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apps.Constants;
import com.apps.eff.CacheUtil;
import com.apps.eff.GameHelpUtil;
import com.apps.eff.dto.SessionItem;
import com.apps.model.UserTradeDetailRl;
import com.framework.dao.hibernate.PaginationSupport;
import com.framework.service.impl.BaseService;
import com.framework.util.DateTimeUtil;
import com.framework.util.HQUtils;
import com.framework.util.ParamUtils;
import com.framework.util.StringUtil;
import com.game.GameConstants;
import com.game.model.GaBetDetail;
import com.game.model.GaBetOption;
import com.game.model.GaSessionInfo;
import com.game.service.IGaService;
import com.ram.model.User;
import com.ram.service.user.IUserService;
import com.xy.lucky28.bjlu28.BjLu28Constants;
import com.xy.lucky28.bjlu28.dao.IBjLu28DAO;
import com.xy.lucky28.bjlu28.model.BjLu28GaBet;
import com.xy.lucky28.bjlu28.model.BjLu28GaSession;
import com.xy.lucky28.bjlu28.model.BjLu28GaTrend;
import com.xy.lucky28.bjlu28.model.dto.BjLu28DTO;
import com.xy.lucky28.bjlu28.service.IBjLu28Service;

public class BjLu28ServiceImpl  extends BaseService implements IBjLu28Service {
	private IBjLu28DAO bjLu28DAO;
	private IUserService userService;
	private IGaService gaService;
	
	public void setBjLu28DAO(IBjLu28DAO bjLu28DAO) {
		this.bjLu28DAO = bjLu28DAO;
		super.dao = bjLu28DAO;
		
	}
	public void setUserService(IUserService userService) {
		this.userService = userService;
	}
	public void setGaService(IGaService gaService) {
		this.gaService = gaService;
	}
	
	
	/**
	 * 初始化今天场次
	 */
	public String updateInitTodaySession(String sessionNo1){
//		log.info("___[start]__________________________");
		
		String flag = "fail";//返回状态
		
		//今天日期处理 yyyy-MM-dd
		Date now = DateTimeUtil.getJavaUtilDateNow();
		//String today = DateTimeUtil.DateToString(now);
//		Date dateAfter = DateTimeUtil.getDateAfter(now, 1, "yyyy-MM-dd HH:mm:ss");
		String dateAfterString = DateTimeUtil.DateToString(now);
	
		//今天是否已经初始化场次
		boolean isTodaySessionInit = this.checkTodaySessionInit(now);
		List<BjLu28GaSession> saveList = null;
		if(!isTodaySessionInit){
//			log.info("___[start today]__________________________");
			saveList = new ArrayList<BjLu28GaSession>();
			String startTimeStr = dateAfterString + BjLu28Constants.LUCKY28_START_TIME_S;//开始时间串
			Date startDate = DateTimeUtil.strToDateMul(startTimeStr);//开始时间
			
//			log.info("___[startTimeStr="+startTimeStr+"]");
			HQUtils hq = new HQUtils("from BjLu28GaSession gks where gks.startTime>? and gks.startTime<? order by gks.sessionId desc ");
			String todayYyyymmdd = DateTimeUtil.DateToString(DateTimeUtil.getDateBefore(now, 1));
			Date todayStart = DateTimeUtil.parse(todayYyyymmdd+" 23:00:00");
			Date todayEnd = DateTimeUtil.parse(todayYyyymmdd+" 23:59:59");
			hq.addPars(todayStart);
			hq.addPars(todayEnd);
			List<Object> list=bjLu28DAO.findObjects(hq);
			BjLu28GaSession beforeSession=null;
			if(list!=null&&list.size()>0){
				beforeSession=(BjLu28GaSession) list.get(0);
			}
			for (int i = 0; i < BjLu28Constants.LUCKY28_MAX_PART; i++) {
				//计算出当前场次的时间
				long diffTime = startDate.getTime() + i*BjLu28Constants.LUCKY28_TIME_INTERVAL * 60 * 1000;
				Date curSessionDate = new Date(diffTime);
				long endDiffTime = startDate.getTime() + (i+1)*BjLu28Constants.LUCKY28_TIME_INTERVAL * 60 * 1000;
				Date endSessionDate = new Date(endDiffTime);
				
				//String openResult = BjLu28Constants.getRandomK10Result();//随机生成5个中奖数字[1-21]
				String sessionNo = this.getTodaySessionNo(beforeSession, i+1,sessionNo1);//期号
				log.info("___[start today]__________________________sessionNo:"+sessionNo);
				BjLu28GaSession bjLu28GaSession = new BjLu28GaSession();
				bjLu28GaSession.setSessionNo(sessionNo);
				bjLu28GaSession.setStartTime(curSessionDate);
				bjLu28GaSession.setEndTime(endSessionDate);
				bjLu28GaSession.setOpenStatus(BjLu28Constants.LUCKY28_OPEN_STATUS_INIT);
				//bjLu28GaSession.setOpenResult(openResult);//开奖号由系统抓取获取得
//				bjLu28DAO.saveObject(bjLu28GaSession);
				saveList.add(bjLu28GaSession);
				//log.info("___[out]__[sessionNo="+sessionNo+"][diffTime="+diffTime+"][startDate.getTime()="+startDate.getTime()+"]");
			}
			bjLu28DAO.updateObjectList(saveList, null);
			flag = "success";
//			log.info("___[today init completed]__________________________");
		} else {
			flag = "inited";
//			log.info("___[today has been inited]__________________________");
		}
		return flag;
	}
	
		
	
	/**
	 * 初始化场次
	 */
	public String updateInitSession(){
//		log.info("___[start]__________________________");
		
		String flag = "fail";//返回状态
		
		//今天日期处理 yyyy-MM-dd
		Date now = DateTimeUtil.getJavaUtilDateNow();
		//String today = DateTimeUtil.DateToString(now);
		Date dateAfter = DateTimeUtil.getDateAfter(now, 1, "yyyy-MM-dd HH:mm:ss");
		String dateAfterString = DateTimeUtil.DateToString(dateAfter);
	
		//今天是否已经初始化场次
		boolean isTodaySessionInit = this.checkTodaySessionInit(dateAfter);
		List<BjLu28GaSession> saveList = null;
		if(!isTodaySessionInit){
//			log.info("___[start today]__________________________");
			saveList = new ArrayList<BjLu28GaSession>();
			String startTimeStr = dateAfterString + BjLu28Constants.LUCKY28_START_TIME_S;//开始时间串
			Date startDate = DateTimeUtil.strToDateMul(startTimeStr);//开始时间
			
//			log.info("___[startTimeStr="+startTimeStr+"]");
			HQUtils hq = new HQUtils("from BjLu28GaSession gks where gks.startTime>? and gks.startTime<? order by gks.sessionId desc ");
			String todayYyyymmdd = DateTimeUtil.DateToString(new Date());
			Date todayStart = DateTimeUtil.parse(todayYyyymmdd+" 23:00:00");
			Date todayEnd = DateTimeUtil.parse(todayYyyymmdd+" 23:59:59");
			hq.addPars(todayStart);
			hq.addPars(todayEnd);
			List<Object> list=bjLu28DAO.findObjects(hq);
			BjLu28GaSession beforeSession=null;
			if(list!=null&&list.size()>0){
				beforeSession=(BjLu28GaSession) list.get(0);
			}
			for (int i = 0; i < BjLu28Constants.LUCKY28_MAX_PART; i++) {
				//计算出当前场次的时间
				long diffTime = startDate.getTime() + i*BjLu28Constants.LUCKY28_TIME_INTERVAL * 60 * 1000;
				Date curSessionDate = new Date(diffTime);
				long endDiffTime = startDate.getTime() + (i+1)*BjLu28Constants.LUCKY28_TIME_INTERVAL * 60 * 1000;
				Date endSessionDate = new Date(endDiffTime);
				
				//String openResult = BjLu28Constants.getRandomK10Result();//随机生成5个中奖数字[1-21]
				String sessionNo = this.getTodaySessionNo(beforeSession, i+1,"");//期号
				log.info("___[start today]__________________________sessionNo:"+sessionNo);
				BjLu28GaSession bjLu28GaSession = new BjLu28GaSession();
				bjLu28GaSession.setSessionNo(sessionNo);
				bjLu28GaSession.setStartTime(curSessionDate);
				bjLu28GaSession.setEndTime(endSessionDate);
				bjLu28GaSession.setOpenStatus(BjLu28Constants.LUCKY28_OPEN_STATUS_INIT);
				//bjLu28GaSession.setOpenResult(openResult);//开奖号由系统抓取获取得
//				bjLu28DAO.saveObject(bjLu28GaSession);
				saveList.add(bjLu28GaSession);
				//log.info("___[out]__[sessionNo="+sessionNo+"][diffTime="+diffTime+"][startDate.getTime()="+startDate.getTime()+"]");
			}
			bjLu28DAO.updateObjectList(saveList, null);
			flag = "success";
//			log.info("___[today init completed]__________________________");
		} else {
			flag = "inited";
//			log.info("___[today has been inited]__________________________");
		}
		return flag;
	}
	/**
	 * 检查今天的场次是否已经生成
	 * false=未生成
	 * true=已生成
	 * @return
	 */
	public boolean checkTodaySessionInit(Date now){
		String todayYyyymmdd = DateTimeUtil.DateToString(now);
		Date todayStart = DateTimeUtil.parse(todayYyyymmdd+" 00:00:00");
		Date todayEnd = DateTimeUtil.parse(todayYyyymmdd+" 23:59:59");
		
		HQUtils hq = new HQUtils("from BjLu28GaSession bgs where bgs.startTime>? and bgs.startTime<?");
		hq.addPars(todayStart);
		hq.addPars(todayEnd);
		Integer count = bjLu28DAO.countObjects(hq);
		return ParamUtils.chkInteger(count)?true:false;
	}
	/**
	 * 获取今天的期号，按流水1-50 201523101 - 201523150
	 * @param today
	 * @param index
	 * @return
	 */
	public String getTodaySessionNo(BjLu28GaSession beforeSession,int index,String sessionNo1){
//		if(beforeSession==null){
//			return  (817217+ index)+"";
//		}else
		if(ParamUtils.chkString(sessionNo1.trim())){
			return  (Integer.parseInt(sessionNo1.trim())+ index)+"";
		}else{
			return  (Integer.parseInt(beforeSession.getSessionNo())+ index)+"";
		}
	
	}
	/**
	 * 开奖方法
	 * @param sessionNo
	 * @param result
	 * @return
	 */
	public boolean openBjLu28SessionOpenResultMethod(BjLu28GaSession session,String result){
		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_BJLU28);
		try {
			List<Object> para = new ArrayList<Object>();
			StringBuffer hql = new StringBuffer();
			hql.append(" and ho.gameType=?");
			para.add(Constants.GAME_TYPE_XY_BJLU28);
			hql.append( " and ho.sessionId =?");
			para.add(session.getSessionId());
			hql.append(" and ho.betFlag=?" );//目前固定参数
			para.add("1");
			//hql.append(" order by ho.betTime desc");
			
			Map<String, Boolean> map =null;
			if(session.getCountResult()!=null){
				map = numberCountResult(session.getCountResult());
			}else{
				map = numberResult(result);
			}
			
			//本期全部投注记录
			long startTiming = System.currentTimeMillis();
			List<GaBetDetail> list=gaService.findGaBetDetailList(hql.toString(), para);
			GameHelpUtil.log(gameCode,"BETS ... ["+list.size()+"]["+session.getSessionNo()+"]["+(System.currentTimeMillis()-startTiming)+"ms]");
			
			//本期投注统计表
			BjLu28GaBet bet=new BjLu28GaBet();
			BigDecimal totalPoint=new BigDecimal(0);//总投注
			BigDecimal betCash=new BigDecimal(0);//总中奖
			bet.setSessionId(session.getSessionId());
			bet.setSessionNo(session.getSessionNo());
			
			String countResult= session.getCountResult(); //计算结果
			
			if(countResult == null || countResult.length()<=0){		
				Map<String,String>  coumap=this.openResult(result);
				countResult=coumap.get("number1")+","+coumap.get("number2")+","+coumap.get("number3")+","+coumap.get("number")+","+coumap.get("colour");
			}
			
			if(list!=null && !list.isEmpty()){
				//开奖投注用户ids --by.cuisy.20171209
				List<Integer> userIds = new ArrayList<Integer>();
				startTiming = System.currentTimeMillis();
				for(GaBetDetail betDetail:list){
					//开奖时把需要sum更新余额的用户id统计放入开奖及明细写入后批量更新 --by.cuisy.20171209
					if(!StringUtil.chkListIntContains(userIds, betDetail.getUserId())){
						userIds.add(betDetail.getUserId());
					}//~
					
					betDetail.setOpenResult(countResult);
					
					String optionTitle = betDetail.getOptionTitle();
			
					//中奖
					if(map.get(optionTitle) !=null && map.get(optionTitle)){
						betDetail.setWinResult(GameConstants.WIN);
						//中奖金额
						BigDecimal winCash=GameHelpUtil.round(new BigDecimal(betDetail.getBetMoney()).multiply(betDetail.getBetRate()));
						betDetail.setWinCash(winCash);
						//统计
						totalPoint=totalPoint.add(new BigDecimal(betDetail.getBetMoney()));
						betCash=betCash.add(winCash);
						//盈亏
						betDetail.setPayoff(GameHelpUtil.round(winCash.subtract(new BigDecimal(betDetail.getBetMoney()))));
						//备注
						String remark=GameHelpUtil.getRemark(Constants.CASH_TYPE_CASH_PRIZE, winCash);
						
						//更新
						updateOpenData(betDetail,remark.toString());

					}else{
						totalPoint=totalPoint.add(new BigDecimal(betDetail.getBetMoney()));
						betDetail.setWinCash(new BigDecimal(0));
						betDetail.setWinResult(GameConstants.WIN_NOT);//未中奖
						bjLu28DAO.saveObject(betDetail);
					}
				}
				GameHelpUtil.log(gameCode,"Calc ... ["+(System.currentTimeMillis()-startTiming)+"ms]");
				//批量更新开奖用户实时余额 --by.cuisy.20171209
				startTiming = System.currentTimeMillis();
				userService.updateUserMoney(userIds);
				GameHelpUtil.log(gameCode,"BatM ... uids="+userIds.size()+"["+(System.currentTimeMillis()-startTiming)+"ms]");
				
				//把资金明细里投注记录状态值改为有效
				long timginUtds = System.currentTimeMillis();
				userService.updateUserTradeDetailStatus(session.getSessionNo(), 
						Constants.GAME_TYPE_XY_BJLU28, Constants.PUB_STATUS_OPEN);
				GameHelpUtil.log(gameCode,"BatD ... ["+(System.currentTimeMillis()-timginUtds)+"ms]");
			}		
			bet.setTotalPoint(GameHelpUtil.round(totalPoint));
			bet.setWinCash(GameHelpUtil.round(betCash));
			bjLu28DAO.saveObject(bet);
			return true;
		} catch (Exception e) {
			//e.printStackTrace();
			GameHelpUtil.log(gameCode,"open err::["+session.getSessionNo()+"]=>"+e.getMessage(),e);
			return false;
		}
		
	}
	
//	public boolean openBjLu28SessionOpenResultMethod(BjLu28GaSession lu28session,String result){
////		HQUtils hq = new HQUtils("from BjLu28GaSession bgs where bgs.sessionNo=?");
////		String sessionNo = lu28session.getSessionNo();
////		hq.addPars(sessionNo);
////		BjLu28GaSession session = (BjLu28GaSession)bjLu28DAO.getObject(hq);
//		HQUtils hq1 = new HQUtils("from GaBetDetail gd where gd.sessionId=? and gd.betFlag='1' and gd.gameType='4' ");
//		hq1.addPars(lu28session.getSessionId());
//		String openResult = lu28session.getOpenResult();
//		Map<String, Boolean> map = null;
//		if(lu28session.getCountResult()!=null){
//			map = numberCountResult(lu28session.getCountResult());
//		}else{
//			 map = numberResult(openResult);
//		}
//		List<Object>  list= bjLu28DAO.findObjects(hq1);
//		
//		BjLu28GaBet bet=new BjLu28GaBet();
//		BigDecimal  totalPoint=new BigDecimal(0);
//		BigDecimal  betCash=new BigDecimal(0);
//		bet.setSessionId(lu28session.getSessionId());
//		bet.setSessionNo(lu28session.getSessionNo());
//		String countResult= lu28session.getCountResult(); //计算结果
//		if(countResult == null || countResult.length()<=0){
//			
//			Map<String,String>  coumap=this.openResult(openResult);
//			countResult=coumap.get("number1")+","+coumap.get("number2")+","+coumap.get("number3")+","+coumap.get("number")+","+coumap.get("colour");
//		}
//		if(list!=null && list.size()>0){
//			//开奖投注用户ids --by.cuisy.20171209
//			List<Integer> userIds = new ArrayList<Integer>();
//			
//			for(Object object:list){
//				GaBetDetail betDetail=(GaBetDetail) object;
//				//开奖时把需要sum更新余额的用户id统计放入开奖及明细写入后批量更新 --by.cuisy.20171209
//				if(!StringUtil.chkListIntContains(userIds, betDetail.getUserId())){
//					userIds.add(betDetail.getUserId());
//				}//~
//				
//				String optionTitle = betDetail.getOptionTitle();
//				Integer userId = betDetail.getUserId();
//
//				betDetail.setOpenResult(countResult);
//
//				//中奖
//				if(map.get(optionTitle) !=null && map.get(optionTitle)){
//					betDetail.setWinResult("1");//中奖
////					int winCash = (new BigDecimal(betDetail.getBetMoney())).add(new BigDecimal(new BigDecimal(betDetail.getBetMoney()).multiply(betDetail.getBetRate()).intValue())).intValue();					
////					betDetail.setWinCash(new BigDecimal(new BigDecimal(betDetail.getBetMoney()).multiply(betDetail.getBetRate()).intValue()));
//					BigDecimal wincash=betDetail.getBetRate().multiply(new BigDecimal(betDetail.getBetMoney()).setScale(2, BigDecimal.ROUND_HALF_UP));
//					betDetail.setWinCash(wincash);
//					totalPoint=totalPoint.add(new BigDecimal(betDetail.getBetMoney()));
//					betCash=betCash.add(wincash);
//					betDetail.setPayoff(wincash.subtract(new BigDecimal(betDetail.getBetMoney())).setScale(2, BigDecimal.ROUND_HALF_UP));
//				
////					userService.saveTradeDetail(user,betDetail.getUserId(), Constants.TRADE_TYPE_INCOME, Constants.CASH_TYPE_CASH_BET_BJLU28, wincash, betDetail.getBetDetailId());						
////					userService.savePointDetail(betDetail.getUserId(), Constants.TRADE_TYPE_INCOME, Constants.POINT_TYPE_BJ_LUCKY28, winCash, null);
//					
//					StringBuffer remark=new StringBuffer();
//					remark.append("彩票中奖 奖金 ")
//					    .append(wincash).append("元");
////					user=userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_INCOME,Constants.CASH_TYPE_CASH_PRIZE, wincash, betDetail.getBetDetailId(), Constants.GAME_TYPE_XY_BJLU28,remark.toString());
//					BigDecimal userBal=null;
////					User user = userService.getUser(userId);
////					if(user.getUserBalance()!=null){
////						userBal=user.getUserBalance();
////					}else{
////						userBal=new BigDecimal(0);
////					}
////					user.setUserBalance(userBal.add(wincash).setScale(2, BigDecimal.ROUND_HALF_UP));
//					try {
//						updateOpenData(betDetail,null,remark.toString());
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}else{
//					totalPoint=totalPoint.add(new BigDecimal(betDetail.getBetMoney()));
//					betDetail.setWinResult("2");//未中奖
////					betDetail.setWinCash(new BigDecimal("-"+betDetail.getBetMoney()));
//					betDetail.setWinCash(new BigDecimal(0));
//					try {
//						bjLu28DAO.updateObject(betDetail, null);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}
//			//批量更新开奖用户实时余额 --by.cuisy.20171209
//			userService.updateUserMoney(userIds);
//		}
//		
//		bet.setTotalPoint(totalPoint);
//		bet.setWinCash(betCash);
//		bjLu28DAO.saveObject(bet, null);
//		return true;
//	}
	
	public void updateOpenData(GaBetDetail detail,String remark){
		bjLu28DAO.saveObject(detail);
		userService.saveTradeDetail(null,detail.getUserId(), 
				Constants.TRADE_TYPE_INCOME,
				Constants.CASH_TYPE_CASH_PRIZE, detail.getWinCash(), 
				detail.getBetDetailId(), 
				Constants.GAME_TYPE_XY_BJLU28,
				remark,detail.getSessionNo(),detail.getType(),detail.getLoginName());
	}
	
//	public void updateOpenData(GaBetDetail detail,User user,String remark){
//		bjLu28DAO.saveObject(detail);
//		user=userService.saveTradeDetail(user,detail.getUserId(), Constants.TRADE_TYPE_INCOME,Constants.CASH_TYPE_CASH_PRIZE, detail.getWinCash(), detail.getBetDetailId(), Constants.GAME_TYPE_XY_BJLU28,remark);
//	}

	/**
	 * 中奖结果
	 * @param openResult
	 * @return
	 */
	public Map<String,Boolean>  numberResult(String openResult){
		Map<String,Boolean> map=new HashMap<String,Boolean>();
		String[] split = openResult.split(",");
		int sum1=0;
		int sum2=0;
		int sum3=0;
		for (int i = 0; i < split.length; i++) {
			if(i<6){
				sum1=sum1+Integer.valueOf(split[i]).intValue();
			}else if(i>=6 && i<12){
				sum2=sum2+Integer.valueOf(split[i]).intValue();
			}else if(i>=12 && i<18){
				sum3=sum3+Integer.valueOf(split[i]).intValue();
			}
		}
		int number1=sum1 % 10;
		int number2=sum2 % 10;
		int number3=sum3 % 10;
		int number=number1 + number2 + number3;
		if(number<=13){
			map.put("小", true);
		}else if(number>=14 && number<=27){
			map.put("大", true);
		}
		if(number%2==0){
			map.put("双", true);
		}else if(number%2!=0){
			map.put("单", true);
		}
		if(number>=14 && number%2==0){
			map.put("大双", true);
		}else if(number>14 && number%2!=0){
			map.put("大单", true);
		}
		if(number<14 && number%2==0){
			map.put("小双", true);
		}else if(number<14 && number%2!=0){
			map.put("小单", true);
		}
		if(number>=0 && number<=4){
			map.put("极小", true);
		}else if(number>=23 && number<=27){
			map.put("极大", true);
		}
		if(number!=13 && number%3==1){
			map.put("绿波", true);
		}else if(number!=14 && number%3==2){
			map.put("蓝波", true);
		}else if(number!=0 && number!=27 && number%3==0){
			map.put("红波", true);
		}
		if(number1==number2 && number2==number3){
			map.put("豹子", true);
		}
		map.put(""+number, true);
		return map;
	}
	
	
	/**
	 * 中奖结果
	 * @param openResult
	 * @return
	 */
	public Map<String,Boolean>  numberCountResult(String openResult){
		Map<String,Boolean> map=new HashMap<String,Boolean>();
		String[] split = openResult.split(",");
		int number1=Integer.parseInt(split[0]);
		int number2=Integer.parseInt(split[1]);
		int number3=Integer.parseInt(split[2]);
		int number=number1 + number2 + number3;
		if(number<=13){
			map.put("小", true);
		}else if(number>=14 && number<=27){
			map.put("大", true);
		}
		if(number%2==0){
			map.put("双", true);
		}else if(number%2!=0){
			map.put("单", true);
		}
		if(number>=14 && number%2==0){
			map.put("大双", true);
		}else if(number>14 && number%2!=0){
			map.put("大单", true);
		}
		if(number<14 && number%2==0){
			map.put("小双", true);
		}else if(number<14 && number%2!=0){
			map.put("小单", true);
		}
		if(number>=0 && number<=4){
			map.put("极小", true);
		}else if(number>=23 && number<=27){
			map.put("极大", true);
		}
		if(number!=13 && number%3==1){
			map.put("绿波", true);
		}else if(number!=14 && number%3==2){
			map.put("蓝波", true);
		}else if(number!=0 && number!=27 && number%3==0){
			map.put("红波", true);
		}
		if(number1==number2 && number2==number3){
			map.put("豹子", true);
		}
		map.put(""+number, true);
		return map;
	}
	
	/**
	 * 开奖结果及波色
	 * @param openResult
	 * @return
	 */
	public Map<String,String>  openResult(String openResult){
		Map<String,String> map=new HashMap<String,String>();
		String[] split = openResult.split(",");
		int sum1=0;
		int sum2=0;
		int sum3=0;
		for (int i = 0; i < split.length; i++) {
			if(i<6){
				sum1=sum1+Integer.valueOf(split[i]).intValue();
			}else if(i>=6 && i<12){
				sum2=sum2+Integer.valueOf(split[i]).intValue();
			}else if(i>=12 && i<18){
				sum3=sum3+Integer.valueOf(split[i]).intValue();
			}
		}
		int number1=sum1 % 10;
		int number2=sum2 % 10;
		int number3=sum3 % 10;
		int number=number1 + number2 + number3;
		map.put("number1", number1+"");
		map.put("number2", number2+"");
		map.put("number3", number3+"");
		map.put("number", number+"");
		if(number!=13 && number%3==1){//绿波
			map.put("colour", "1");
		}else if(number!=14 && number%3==2){//蓝波
			map.put("colour", "2");
		}else if(number!=0 && number!=27 && number%3==0){//红波
			map.put("colour", "3");
		}else{
			map.put("colour", "0");//无波色
		}
		if(number<=13){
			map.put("大小", "小");
		}else if(number>=14 && number<=27){
			map.put("大小", "大");
		}
		if(number%2==0){
			map.put("单双", "双");
		}else if(number%2!=0){
			map.put("单双", "单");
		}
		if(number>=0 && number<=4){
			map.put("极值", "极小");
		}else if(number>=23 && number<=27){
			map.put("极值", "极大");
		}else{
			map.put("极值", "-");
		}
		if(number1==number2 && number2==number3){
			map.put("豹子", "豹子");
		}else{
			map.put("豹子", "-");
		}
		return map;
	}
	
	public Map<String,String>  openCountResult(String openResult){
		Map<String,String> map=new HashMap<String,String>();
		String[] split = openResult.split(",");

		int number1=Integer.parseInt(split[0]);
		int number2=Integer.parseInt(split[1]);
		int number3=Integer.parseInt(split[2]);
		int number=number1 + number2 + number3;
		map.put("number1", number1+"");
		map.put("number2", number2+"");
		map.put("number3", number3+"");
		map.put("number", number+"");
		if(number!=13 && number%3==1){//绿波
			map.put("colour", "1");
		}else if(number!=14 && number%3==2){//蓝波
			map.put("colour", "2");
		}else if(number!=0 && number!=27 && number%3==0){//红波
			map.put("colour", "3");
		}else{
			map.put("colour", "0");//无波色
		}
		if(number<=13){
			map.put("大小", "小");
		}else if(number>=14 && number<=27){
			map.put("大小", "大");
		}
		if(number%2==0){
			map.put("单双", "双");
		}else if(number%2!=0){
			map.put("单双", "单");
		}
		if(number>=0 && number<=4){
			map.put("极值", "极小");
		}else if(number>=23 && number<=27){
			map.put("极值", "极大");
		}else{
			map.put("极值", "-");
		}
		if(number1==number2 && number2==number3){
			map.put("豹子", "豹子");
		}else{
			map.put("豹子", "-");
		}
		return map;
	}
	
	/**
	 * 混合结果判断
	 * @param openResult
	 * @return
	 */
	public Map<String,String>  judgeResult(String openResult){
		Map<String,String> map=new HashMap<String,String>();
		String[] split = openResult.split(",");
		int sum1=0;
		int sum2=0;
		int sum3=0;
		for (int i = 0; i < split.length; i++) {
			if(i<6){
				sum1=sum1+Integer.valueOf(split[i]).intValue();
			}else if(i>=6 && i<12){
				sum2=sum2+Integer.valueOf(split[i]).intValue();
			}else if(i>=12 && i<18){
				sum3=sum3+Integer.valueOf(split[i]).intValue();
			}
		}
		int number1=sum1 % 10;
		int number2=sum2 % 10;
		int number3=sum3 % 10;
		int number=number1 + number2 + number3;
		if(number<=13){
			map.put("大小", "小");
		}else if(number>=14 && number<=27){
			map.put("大小", "大");
		}
		if(number%2==0){
			map.put("单双", "双");
		}else if(number%2!=0){
			map.put("单双", "单");
		}
		if(number>=0 && number<=4){
			map.put("极值", "极小");
		}else if(number>=23 && number<=27){
			map.put("极值", "极大");
		}else{
			map.put("极值", "-");
		}
		if(number!=13 && number%3==1){//绿波
			map.put("波色", "绿波");
		}else if(number!=14 && number%3==2){//蓝波
			map.put("波色", "蓝波");
		}else if(number!=0 && number!=27 && number%3==0){//红波
			map.put("波色", "红波");
		}else{
			map.put("波色", "-");//无波色
		}
		if(number1==number2 && number2==number3){
			map.put("number", number1+"");
			map.put("豹子", "豹子");
		}else{
			map.put("豹子", "-");
		}
		return map;
	}
	
	public Map<String,String>  judgeCountResult(String openResult){
		Map<String,String> map=new HashMap<String,String>();
		String[] split = openResult.split(",");

		int number1=Integer.parseInt(split[0]);
		int number2=Integer.parseInt(split[1]);
		int number3=Integer.parseInt(split[2]);
		int number=number1 + number2 + number3;
		if(number<=13){
			map.put("大小", "小");
		}else if(number>=14 && number<=27){
			map.put("大小", "大");
		}
		if(number%2==0){
			map.put("单双", "双");
		}else if(number%2!=0){
			map.put("单双", "单");
		}
		if(number>=0 && number<=4){
			map.put("极值", "极小");
		}else if(number>=23 && number<=27){
			map.put("极值", "极大");
		}else{
			map.put("极值", "-");
		}
		if(number!=13 && number%3==1){//绿波
			map.put("波色", "绿波");
		}else if(number!=14 && number%3==2){//蓝波
			map.put("波色", "蓝波");
		}else if(number!=0 && number!=27 && number%3==0){//红波
			map.put("波色", "红波");
		}else{
			map.put("波色", "-");//无波色
		}
		if(number1==number2 && number2==number3){
			map.put("number", number1+"");
			map.put("豹子", "豹子");
		}else{
			map.put("豹子", "-");
		}
		return map;
	}
	
	public String updateFetchAndOpenResult(Map<String, SessionItem> sessionNoMap) {
		if(sessionNoMap==null || sessionNoMap.isEmpty()) return "fail::no open sessionNo";
		//当前场次及开奖场次处理
		BjLu28GaSession curtSession = bjLu28DAO.getCurrentSession();
		if(curtSession==null) return "fail::no current session";
		BjLu28GaSession lastSession = (BjLu28GaSession)bjLu28DAO.getObject(BjLu28GaSession.class,curtSession.getSessionId()-1);
		if(lastSession==null) return "fail::no last session::id="+curtSession.getSessionId();
		
		//开奖场次期号
		String lastSessionNo = lastSession.getSessionNo();
		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_BJLU28);
		
		//迭代开奖无序
		List<BjLu28GaSession> openedList = new ArrayList<BjLu28GaSession>();//开奖场次
		String sNo;//期号
		SessionItem sessionItem;//开奖结果
		String status;//状态
		String result;//开奖号
		try {
			GameHelpUtil.log(gameCode,"-------- OPENing --------");
			for(Map.Entry<String, SessionItem> entry:sessionNoMap.entrySet()){
				sNo = entry.getKey();sessionItem = entry.getValue();
				BjLu28GaSession session = bjLu28DAO.getPreviousSessionBySessionNo(sNo);
				if(session!=null){//开奖
					status = session.getOpenStatus();
					result = sessionItem.getResult();
					session.setOpenResult(result);//设置开奖号
					if(status.equals(GameConstants.OPEN_STATUS_INIT) || status.equals(GameConstants.OPEN_STATUS_OPENING)){
						GameHelpUtil.log(gameCode,"Start ... ["+sNo+"],status="+status+",result="+result);
						long timingOpen = System.currentTimeMillis();
						boolean flag = openBjLu28SessionOpenResultMethod(session, result);
						if(flag){
							String countResult="";
							Map<String,String>  map=this.openResult(result);
							countResult=map.get("number1")+","+map.get("number2")+","+map.get("number3")+","+map.get("number")+","+map.get("colour");
							session.setCountResult(countResult);
							
							session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());//本系统开奖时间
							session.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
							bjLu28DAO.saveObject(session);
							openedList.add(session);
						}
						GameHelpUtil.log(gameCode,"End ... ["+sNo+"]["+(System.currentTimeMillis()-timingOpen)+"ms],status="+session.getOpenStatus()+",result="+result);
					}
				}else{
					GameHelpUtil.log(gameCode,"opening session is null::"+sNo);
				}
			}
			
			//更新彩种场次状态
			long timingGsi = System.currentTimeMillis();
			GaSessionInfo sessionInfo = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_XY_BJLU28);
			if(sessionInfo!=null){
				SessionItem lastItem = (SessionItem)sessionNoMap.get(lastSessionNo);//上一期
				if(lastItem!=null){
					sessionInfo.setOpenResult(lastItem.getResult());
					sessionInfo.setOpenSessionNo(lastSessionNo);
					sessionInfo.setEndTime(lastSession.getEndTime());
				}
				sessionInfo.setLatestSessionNo(curtSession.getSessionNo());
				bjLu28DAO.saveObject(sessionInfo);
				CacheUtil.updateGameList();
				GameHelpUtil.log(gameCode,"gsi last ... ["+(System.currentTimeMillis()-timingGsi)+"ms]"+lastSessionNo+","+lastItem.getResult());
			}
			
			//更新走势 上面已成功开奖的场次
			long startTrending = System.currentTimeMillis();
			for(BjLu28GaSession session:openedList){
				this.updateTrendResult(session);
			}
			GameHelpUtil.log(gameCode,"trend ... ["+openedList.size()+"]["+(System.currentTimeMillis()-startTrending)+"ms]");
			
			sessionNoMap.clear();
			return "success";
		} catch (Exception e) {
			GameHelpUtil.log(gameCode,"open err::=>"+e.getMessage(),e);
			return "err::throw exception...";
		}
	}
	
//	public void updateFetchAndOpenResult(Map<String, SessionItem> sessionNoMap){
//		BjLu28GaSession currentSession= bjLu28DAO.getCurrentSession();
//		final String lastSessionNo=(Integer.parseInt(currentSession.getSessionNo())-1)+"";
//        
//		if(!sessionNoMap.isEmpty()){
//			for(String key:sessionNoMap.keySet()){
//				HQUtils hq = new HQUtils("from BjLu28GaSession gks where gks.sessionNo=?");
//				hq.addPars(key);
//				BjLu28GaSession session = (BjLu28GaSession)bjLu28DAO.getObject(hq);
//				if(session!=null){
//					String openStatus = session.getOpenStatus();//开奖状态
//					
//					if(openStatus.equals(BjLu28Constants.LUCKY28_OPEN_STATUS_INIT) || openStatus.equals(BjLu28Constants.LUCKY28_OPEN_STATUS_OPENING)){//状态为初始化 或 开奖中 则开始开奖
//						//更新开奖结果
//						SessionItem sessionItem = (SessionItem)sessionNoMap.get(key);
//						String openResult = sessionItem.getResult();
//						session.setOpenResult(openResult);
//						bjLu28DAO.saveObject(session);
//						
//						//进入开奖程序方法
//						boolean flag = this.openBjLu28SessionOpenResultMethod(session, openResult);
//						
//						if(flag){
//							String countResult="";
//							Map<String,String>  map=this.openResult(openResult);
//							countResult=map.get("number1")+","+map.get("number2")+","+map.get("number3")+","+map.get("number")+","+map.get("colour");
//							session.setCountResult(countResult);
//							//更新场次状态，TODO 盈亏数据也需要在开奖
////								session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
//							session.setOpenTime(DateTimeUtil.stringToDate(sessionItem.getTime(), "yyyy-MM-dd HH:mm:ss"));
//							session.setOpenStatus(BjLu28Constants.LUCKY28_OPEN_STATUS_OPENED);
//							bjLu28DAO.saveObject(session);
//						}else{
//							GameHelpUtil.log(Constants.GAME_TYPE_XY_BJLU28,"open flag FAIL -_- ["+session.getSessionNo()+"]");
//						}
//					}
//					
//					// 把资金明细里投注记录状态值改为有效
//					userService.updateUserTradeDetailStatus(session.getSessionNo(), 
//							Constants.GAME_TYPE_XY_BJLU28, Constants.PUB_STATUS_OPEN);
//				}
//			}
//			
//			GaSessionInfo sessionInfo = gaService.findGaSessionInfo(Constants.GAME_TYPE_XY_BJLU28);
//			if(sessionInfo!=null){
//				SessionItem lastItem = (SessionItem)sessionNoMap.get(lastSessionNo);//上一期
//				if(sessionNoMap.get(lastSessionNo)!=null){
//					String countResult="";
//					Map<String,String>  map=this.openResult(lastItem.getResult());
//					countResult=map.get("number1")+","+map.get("number2")+","+map.get("number3")+","+map.get("number")+","+map.get("colour");
//					sessionInfo.setOpenResult(countResult);
//					sessionInfo.setOpenSessionNo(lastSessionNo);
//					sessionInfo.setEndTime(bjLu28DAO.getPreviousSessionBySessionNo(lastSessionNo).getEndTime());
//				}
//				sessionInfo.setLatestSessionNo(currentSession.getSessionNo());
//				
//				bjLu28DAO.saveObject(sessionInfo);
//				CacheUtil.updateGameList();
//			}
//			sessionNoMap.clear();
//		}
//		
//	}
	
//	public void updateFetchAndOpenResult() {
//		BjLu28GaSession currentSession= bjLu28DAO.getCurrentSession();
//		final String lastSessionNo=(Integer.parseInt(currentSession.getSessionNo())-1)+"";
//		final Map<String,String> sessionNoMap=new HashMap<String,String>();
//		final Map<String,String> timeMap=new HashMap<String,String>();
//		Thread t=new Thread(){
//            public void run(){
//         	   int countRun=0;
//         	   out:
//         	   while(true){
//         		   if(countRun==90){
//         			   interrupt();
//         			   break;
//         		   }
//        		    countRun=countRun+1;
//        		    try {
//	               		//从这里 ---------------------------------------------------------------------------
//           		    GaSessionInfo sessionInfo=CacheUtil.getGameOpenUrl().get("xybjlu28");
//           		    String officalURL = "";
//           		    String urlSwitch=sessionInfo.getUrlSwitch();
//           		    if(urlSwitch.equals("1")){//1=开彩网  2=彩票控
//           		    	officalURL = sessionInfo.getKaicaiUrl()+"&timestamp="+System.currentTimeMillis();
//           		    }else if(urlSwitch.equals("2")){
//           		    	officalURL = sessionInfo.getCaipiaoUrl()+"&timestamp="+System.currentTimeMillis();
//           		    }
//
//	               		log.info("___[bjLu28 start fetch result xml data...]________________");
//	               		sleep(3000);
//	               		String resultXML = URLUtil.HttpRequestUTF8(officalURL);
//	               		//到这里 ---------------------------------------------------------------------------		
////	               		log.info("___[fetch result xml data]"+resultXML);
//	               		if(ParamUtils.chkString(resultXML)){
//	               			Document xmlDoc = XmlUtil.getDOMDocumentFromString(resultXML);
//	            		    if(urlSwitch.equals("1")){//1=开彩网  2=彩票控
//		               			//开始解析场次开奖数据
//		               			NodeList nList = xmlDoc.getElementsByTagName("row");
//		               			
//		               			String sessionNo="";//场次号
//		               			String result="";//开奖结果5组数字
//		               			String time="";
//		               			for(int i =0;i<nList.getLength();i++){
//		               				Node node = nList.item(i);
//		               				//数据库来源开彩网
//		               				sessionNo = XmlUtil.getElementAttribute((Element)node, "expect");
//		               				result = XmlUtil.getElementAttribute((Element)node, "opencode");	
//		               				time = XmlUtil.getElementAttribute((Element)node, "opentime");	
//		               				if(ParamUtils.chkString(result)){
//		               					if(result.contains("+")){
//		               						result = result.substring(0, result.indexOf("+"));
//		               					}              					
//		               				}
//	        						if(sessionNoMap.get(sessionNo)==null){
//	        							sessionNoMap.put(sessionNo, result);
//	        							timeMap.put(sessionNo, time);
//	        						}
//	        						if(sessionNo.equals(lastSessionNo)){
//	        							interrupt();
//	        							break out;
//	        						}
//		               			}	            		    
//		               		}else if(urlSwitch.equals("2")){
//		               			//开始解析场次开奖数据
//		               			NodeList nList = xmlDoc.getElementsByTagName("item");
//		               			String sessionNo="";//场次号
//		               			String result="";//开奖结果5组数字
//		               			String time="";
//		               			for(int i =0;i<nList.getLength();i++){
//		               				Node node = nList.item(i);
//		               				//数据库来源    彩票控
//		               				sessionNo = XmlUtil.getElementAttribute((Element)node, "id");//期号
//		               				result = XmlUtil.getNodeTextValue(node);	//开奖结果
//		               				time = XmlUtil.getElementAttribute((Element)node, "dateline");//开奖时间
//		               				if(ParamUtils.chkString(result)){
//		               					result = result.substring(0, result.length()-3);          					
//		               				}
//	        						if(sessionNoMap.get(sessionNo)==null){
//	        							sessionNoMap.put(sessionNo, result);
//	        							timeMap.put(sessionNo, time);
//	        						}
//	        						if(sessionNo.equals(lastSessionNo)){
//	        							interrupt();
//	        							break out;
//	        						}
//		               			}
//	            		    }
//	               		}
//                    }catch (Exception e) {
//
//                    }
//         	   }
//            }
//        };
//        t.start();
//        
//        try {
//			t.join();//该方法是等 t 线程结束以后再执行后面的代码
//			if(!sessionNoMap.isEmpty()){
//				for(String key:sessionNoMap.keySet()){
//					HQUtils hq = new HQUtils("from BjLu28GaSession gks where gks.sessionNo=?");
//					hq.addPars(key);
//					BjLu28GaSession session = (BjLu28GaSession)bjLu28DAO.getObject(hq);
//					if(session!=null){
//						String openStatus = session.getOpenStatus();//开奖状态
//						
//						if(openStatus.equals(BjLu28Constants.LUCKY28_OPEN_STATUS_INIT) || openStatus.equals(BjLu28Constants.LUCKY28_OPEN_STATUS_OPENING)){//状态为初始化 或 开奖中 则开始开奖
//							//更新开奖结果
//							String openResult = sessionNoMap.get(key);
//							session.setOpenResult(openResult);
//							bjLu28DAO.saveObject(session);
//							
//							//进入开奖程序方法
//							boolean flag = this.openBjLu28SessionOpenResultMethod(session, openResult);
//							
//							if(flag){
//								String countResult="";
//								Map<String,String>  map=this.openResult(openResult);
//								countResult=map.get("number1")+","+map.get("number2")+","+map.get("number3")+","+map.get("number")+","+map.get("colour");
//								session.setCountResult(countResult);
//								//更新场次状态，TODO 盈亏数据也需要在开奖
////								session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
//								session.setOpenTime(DateTimeUtil.stringToDate(timeMap.get(key), "yyyy-MM-dd HH:mm:ss"));
//								session.setOpenStatus(BjLu28Constants.LUCKY28_OPEN_STATUS_OPENED);
//								bjLu28DAO.saveObject(session);
////								log.info("___[open result success sessionNO["+sessionNo+"]]");
//							}else{
////								log.info("___[open result fail sessionNO["+sessionNo+"], please check...]");
//							}
//						}
//					}
//				}
//				
//				GaSessionInfo sessionInfo = gaService.findGaSessionInfo(Constants.GAME_TYPE_XY_BJLU28);
//				if(sessionInfo!=null){
//
////					HQUtils hq = new HQUtils("from BjLu28GaSession gks where gks.sessionNo=?");
////					hq.addPars(lastSessionNo);
////					BjLu28GaSession session = (BjLu28GaSession)bjLu28DAO.getObject(hq);
////					sessionInfo.setOpenSessionNo(currentSession.getSessionNo());
//					if(sessionNoMap.get(lastSessionNo)!=null){
//						String countResult="";
//						Map<String,String>  map=this.openResult(sessionNoMap.get(lastSessionNo));
//						countResult=map.get("number1")+","+map.get("number2")+","+map.get("number3")+","+map.get("number")+","+map.get("colour");
//						sessionInfo.setOpenResult(countResult);
//						sessionInfo.setOpenSessionNo(lastSessionNo);
//						sessionInfo.setEndTime(bjLu28DAO.getPreviousSessionBySessionNo(lastSessionNo).getEndTime());
//					}
//					sessionInfo.setLatestSessionNo(currentSession.getSessionNo());
//					
//					bjLu28DAO.saveObject(sessionInfo);
//					CacheUtil.updateGameList();
//				}
//				sessionNoMap.clear();
//				timeMap.clear();
//			}
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	public BjLu28GaSession getCurrentSession() {
		return bjLu28DAO.getCurrentSession();
	}
	
	public BjLu28GaSession getPreviousSessionBySessionNo(String sessionNo) {
		return bjLu28DAO.getPreviousSessionBySessionNo(sessionNo);
	}
	
	public User saveUserBetInfo(String room, Map<Integer, Integer> betMap,
			List<GaBetOption> list, BjLu28GaSession session, User user,
			BigDecimal betAll) {
		//彩种缓存
		GaSessionInfo gsi = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_XY_BJLU28);
		//投注与明细关联
		List<UserTradeDetailRl> rlList = new ArrayList<UserTradeDetailRl>();
		//用户类型
		String userType = user.getUserType();
		
		//迭代投注选项
		for(GaBetOption betOption:list){
			GaBetDetail betDetail=new GaBetDetail();
			betDetail.setBetRate(betOption.getBetRate());
			betDetail.setWinResult(GameConstants.OPEN_STATUS_INIT);//未开奖
			betDetail.setBetFlag(GameConstants.STATUS_1);//有效投注
			betDetail.setSessionId(session.getSessionId());
			betDetail.setSessionNo(session.getSessionNo());
			
			betDetail.setUserId(user.getUserId());
			betDetail.setType(userType);
			betDetail.setLoginName(user.getLoginName());
			
			betDetail.setBetOptionId(betOption.getBetOptionId());
			betDetail.setBetTime(DateTimeUtil.getNow());//投注时间
			betDetail.setBetMoney(betMap.get(betOption.getBetOptionId()));
			
			betDetail.setRoom(room);
			betDetail.setGameName(gsi.getGameTitle());
			betDetail.setGameType(gsi.getGameType());
			
			if(betOption.getPlayType().equals("0")){//两面盘
				betDetail.setPlayName("两面盘");
			}else{
				betDetail.setPlayName("特码");
			}
			betDetail.setBetName(this.getBetNameByOptionType(betOption.getPlayType(),betOption.getOptionType().toString()));
			betDetail.setOptionTitle(betOption.getOptionTitle());
			
			//不能为空字段初始化
			betDetail.setBetId(GameConstants.DEF_ID);//目前未用默认0
			betDetail.setPaperMoney(new BigDecimal(GameConstants.DEF_NUMBER));//目前未用默认0 红包
			betDetail.setWinCash(new BigDecimal(GameConstants.DEF_NUMBER));//初始值
			betDetail.setPayoff(new BigDecimal(GameConstants.DEF_NUMBER));//初始值
			
			bjLu28DAO.saveObject(betDetail);
			
			//关联构造
			UserTradeDetailRl rl = new UserTradeDetailRl();
			rl.setBetDetailId(betDetail.getBetDetailId());
			rlList.add(rl);
		}
		
		//更新账户信息
		String remark = GameHelpUtil.getRemark(Constants.CASH_TYPE_CASH_BUY_LOTO, betAll);
		Integer tradeDetailId = userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY,Constants.CASH_TYPE_CASH_BUY_LOTO, betAll, null, 
				Constants.GAME_TYPE_XY_BJLU28,remark,session.getSessionNo(),user.getUserType(),user.getLoginName());

		//更新用户实时余额  --by.cuisy.20171209
		userService.updateUserMoney(user.getUserId());
		userService.updateUserBanlance(user.getUserId());
		
		//保存关联
		for(UserTradeDetailRl rl:rlList){
			rl.setTradeDetailId(tradeDetailId);
			rl.setGfxy(Constants.GAME_PLAY_CATE_XY);
		}
		bjLu28DAO.updateObjectList(rlList, null);

		return user;
	}
	
//	public User saveUserBetInfo(String room, Map<Integer, Integer> betMap,
//			List<GaBetOption> list, BjLu28GaSession session, User user,
//			BigDecimal betAll) {
//
//		List<GaBetDetail> betDetailList=new ArrayList<GaBetDetail>();
//		BigDecimal paperMoney=user.getUserScore();
//		if(paperMoney==null) paperMoney = new BigDecimal(0);//判空处理
//		BigDecimal tempMoney=new BigDecimal(0);
//		for (int i = 0; i < list.size(); i++) {
//				GaBetOption betOption = list.get(i);
//				GaBetDetail betDetail=new GaBetDetail();
//				if(betOption!=null){
//					betDetail.setBetRate(betOption.getBetRate());
//				}
//				betDetail.setWinResult("0");//未开奖
//				betDetail.setBetFlag("1");//有效投注
//				betDetail.setSessionId(session.getSessionId());
//				betDetail.setUserId(user.getUserId());
//				betDetail.setBetOptionId(betOption.getBetOptionId());
//				betDetail.setBetTime(new Date());
//				betDetail.setBetMoney(betMap.get(betOption.getBetOptionId()));
//				if(paperMoney.compareTo(new BigDecimal(0))==1){
//					if(new BigDecimal(betMap.get(betOption.getBetOptionId())).compareTo(paperMoney)!=-1){
//						betDetail.setPaperMoney(paperMoney);
//						paperMoney=paperMoney.subtract(paperMoney);
//						tempMoney=tempMoney.add(new BigDecimal(betMap.get(betOption.getBetOptionId())));
//					}else{
//						betDetail.setPaperMoney(new BigDecimal(betMap.get(betOption.getBetOptionId())));
//						paperMoney=paperMoney.subtract(new BigDecimal(betMap.get(betOption.getBetOptionId())));
//						tempMoney=tempMoney.add(new BigDecimal(betMap.get(betOption.getBetOptionId())));
//					}
//				}
//				
////				betDetail.setPaperMoney(paperMoney);
//				betDetail.setRoom(room);
//				betDetail.setSessionNo(session.getSessionNo());
//				betDetail.setGameName("PC蛋蛋");
//
//				if(betOption.getPlayType().equals("0")){//两面盘
//					betDetail.setPlayName("两面盘");
//				}else{
//					betDetail.setPlayName("特码");
//				}
//				betDetail.setBetName(this.getBetNameByOptionType(betOption.getPlayType(),betOption.getOptionType().toString()));
//				betDetail.setOptionTitle(betOption.getOptionTitle());
//				betDetail.setGameType(Constants.GAME_TYPE_XY_BJLU28);//游戏类型 0=三份彩  1=北京赛车  2=幸运28  3=重庆时时彩  4=PC蛋蛋  5=广东快乐10分
//				betDetailList.add(betDetail);
//		}
//		
//
//		
//
////			user=userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY, Constants.CASH_TYPE_CASH_BET_BJLU28, betAll.subtract(tempMoney), null);
//		StringBuilder remark = new StringBuilder();
//		remark.append("购买彩票 扣款 ")
//		    .append(betAll.subtract(tempMoney)).append("元");
//		
////		BigDecimal userBalance=user.getUserBalance();
////		if(userBalance==null){
////			userBalance=new BigDecimal(0);
////		}
////		user.setUserBalance(userBalance.subtract(betAll));
////		BigDecimal dayBet=user.getDayBet();
////		if(dayBet==null){
////			dayBet=new BigDecimal(0);
////		}
////		user.setDayBet(dayBet.add(betAll));
////		
//		user=userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY,Constants.CASH_TYPE_CASH_BUY_LOTO, betAll.subtract(tempMoney), null, Constants.GAME_TYPE_XY_BJLU28,remark.toString());
//	
//
//		//更新用户实时余额  --by.cuisy.20171209
//		userService.updateUserMoney(user.getUserId());
//		userService.updateUserBanlance(user.getUserId());
//		
//		bjLu28DAO.updateObjectList(betDetailList, null);
//	
//		return user;
//	}
	
	public List<BjLu28GaTrend> findBjLu28TrendList() {
		return bjLu28DAO.findBjLu28GaTrendList();
	}
	
	public PaginationSupport findBjLu28GaSessionList(String hql,
			List<Object> para, int pageNum, int pageSize) {
		return bjLu28DAO.findBjLu28GaSessionList(hql,para,pageNum,pageSize);
	}
	
	 public String getBetNameByOptionType(String playType,String optionType){
		 if(playType.equals("0")){//两面盘
			 if(optionType.equals("0")){
				 return "双面";
			 }else if(optionType.equals("1")){
				 return "波色/豹子";
			 }
		 }else if(playType.equals("1")){//1-10名
			 if(optionType.equals("0")){
				 return "特码";
			 }
		}
		 return "";
	 }
	 
	 public void updateTrendResult(BjLu28GaSession session){
		if(session.getOpenStatus().equals(GameConstants.OPEN_STATUS_OPENED)){
			List<BjLu28GaTrend> list=bjLu28DAO.findBjLu28GaTrendAllList();
			List<BjLu28GaTrend> savelist=new ArrayList<BjLu28GaTrend>();
			Map<String,Boolean> map=numberResult(session.getOpenResult());
			if(!map.isEmpty()){
				for(int i=0;i<list.size();i++){
					BjLu28GaTrend trend=list.get(i);
					if(map.get(trend.getTrendTitle())!=null&&map.get(trend.getTrendTitle())==true){
						trend.setTrendCount(trend.getTrendCount()+1);				
					}else{
						trend.setTrendCount(0);
					}
					savelist.add(trend);
				}
				bjLu28DAO.updateObjectList(savelist, null);
			}
		}
	}
	 
//	 public void updateFetchAndOpenTrendResult(Integer count){
//			if(count==9){//执行10次
//				count=null;
//				return;
//			}
//			String lastSessionNo=(Integer.parseInt(bjLu28DAO.getCurrentSession().getSessionNo())-1)+"";
//			BjLu28GaSession session =bjLu28DAO.getPreviousSessionBySessionNo(lastSessionNo);
//			if(session.getOpenStatus().equals(BjLu28Constants.LUCKY28_OPEN_STATUS_OPENED)){
//				List<BjLu28GaTrend> list=bjLu28DAO.findBjLu28GaTrendAllList();
//				List<BjLu28GaTrend> savelist=new ArrayList<BjLu28GaTrend>();
//				Map<String,Boolean> map=numberResult(session.getOpenResult());
//				if(!map.isEmpty()){
//					for(int i=0;i<list.size();i++){
//						BjLu28GaTrend trend=list.get(i);
//						if(map.get(trend.getTrendTitle())!=null&&map.get(trend.getTrendTitle())==true){
//							trend.setTrendCount(trend.getTrendCount()+1);				
//						}else{
//							trend.setTrendCount(0);
//						}
//						savelist.add(trend);
//					}
//					bjLu28DAO.updateObjectList(savelist, null);
//					count=null;
//					lastSessionNo=null;
//					session=null;
//					return;
//				}
//
//			}else{
//				Thread t1=new Thread(){
//		            public void run(){
//		               try {
//		            	   sleep(3000);
//		            	   interrupt();
//		               } catch (Exception e) {
//		            	   interrupt();
//		               }
//		            }
//				};
//				t1.start();
//				try {
//					t1.join();
//					t1=null;
//					count++;
//					lastSessionNo=null;
//					session=null;
//					updateFetchAndOpenTrendResult(count);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		}
		public boolean saveOpenResult(BjLu28GaSession session,String openResult){
			String buffer="";
			boolean flag=false;
			if(ParamUtils.chkString(openResult)){
				String array[]=openResult.split(",");
				for(int i=0;i<array.length;i++){
					if(ParamUtils.chkString(array[i].trim())){
						buffer=buffer+array[i].trim()+",";
					}
				}
				buffer=buffer.substring(0, buffer.length()-1);
				int number=Integer.parseInt(buffer.substring(buffer.indexOf(",")));
				if(number!=13 && number%3==1){//绿波
					buffer=buffer+","+1;
				}else if(number!=14 && number%3==2){//蓝波
					buffer=buffer+","+2;
				}else if(number!=0 && number!=27 && number%3==0){//红波
					buffer=buffer+","+3;
				}else{
					buffer=buffer+","+0;
				}
//				session.setOpenResult(openResult);
				session.setCountResult(buffer);
				bjLu28DAO.updateObject(session, null);
				flag=true;
			}
			return flag;
		}
		
		public boolean saveAndOpenResult(BjLu28GaSession session,String openResult){
			String buffer="";
			boolean flag=false;
			if(ParamUtils.chkString(openResult)){
				String regex = ",|，|\\s+";
				String array[]=openResult.split(regex);
				for(int i=0;i<array.length;i++){
					if(ParamUtils.chkString(array[i].trim())){
						buffer=buffer+array[i].trim()+",";
					}
				}
				buffer=buffer.substring(0, buffer.length()-1);
				int number=Integer.parseInt(array[array.length - 1].trim());
				if(number!=13 && number%3==1){//绿波
					buffer=buffer+","+1;
				}else if(number!=14 && number%3==2){//蓝波
					buffer=buffer+","+2;
				}else if(number!=0 && number!=27 && number%3==0){//红波
					buffer=buffer+","+3;
				}else{
					buffer=buffer+","+0;
				}
//				session.setOpenResult(openResult);
				session.setCountResult(buffer);
				boolean flag1 = openBjLu28SessionOpenResultMethod(session, session.getCountResult());
				if(flag1){      
					session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
					session.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
					bjLu28DAO.updateObject(session, null);
					log.info("___[open result success sessionNO["+session.getSessionNo()+"]]");
					flag=true;
				}else{
					log.info("___[open result fail sessionNO["+session.getSessionNo()+"], please check...]");
				}
			}
			return flag;
		}
		@Override
		public PaginationSupport findBjLu28GaBetList(String hql,
				List<Object> para, int pageNum, int pageSize) {
			return bjLu28DAO.findBjLu28GaBetList(hql, para, pageNum, pageSize);
		}

		@Override
		public PaginationSupport findGaBetDetail(String hql, List<Object> para,
				int pageNum, int pageSize) {
			return bjLu28DAO.findGaBetDetail(hql, para, pageNum, pageSize);
		}
		@Override
		public List<BjLu28DTO> findGaBetDetailById(String hql,
				List<Object> para) {
			return bjLu28DAO.findGaBetDetailById(hql, para);
		}
		@Override
		public boolean saveRevokePrize(BjLu28GaSession session) {
			//删除CqSscGaBet表的记录
			List<Object> para = new ArrayList<Object>();
			StringBuffer hql = new StringBuffer();
			hql.append(" and sessionId = ? ");
			para.add(session.getSessionId());
			bjLu28DAO.deleteBjLu28GaBet(hql.toString(),para);

			boolean result = gaService.saveXyRevokePrize(session.getSessionId(), Constants.GAME_TYPE_XY_BJLU28,session.getSessionNo());
			if(result){
				session.setOpenStatus(Constants.OPEN_STATUS_INIT);//未开奖
				gaService.saveObject(session, null);
			}
			return result;
		}

}
