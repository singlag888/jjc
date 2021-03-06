 package com.xy.ssc.five.service.impl;

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
import com.xy.ssc.five.FiveConstants;
import com.xy.ssc.five.dao.IFiveDAO;
import com.xy.ssc.five.model.FiveGaBet;
import com.xy.ssc.five.model.FiveGaSession;
import com.xy.ssc.five.model.FiveGaTrend;
import com.xy.ssc.five.model.dto.FiveDTO;
import com.xy.ssc.five.service.IFiveService;

public class FiveServiceImpl   extends BaseService implements IFiveService {
	private IFiveDAO fiveDAO;
	private IUserService userService;
	private IGaService gaService;
	public void setFiveDAO(IFiveDAO fivedao) {
		fiveDAO = fivedao;
		super.dao = fiveDAO;
	}

	public void setUserService(IUserService userService) {
		this.userService = userService;
	}
	public void setGaService(IGaService gaService) {
		this.gaService = gaService;
	}
	
	@Override
	public String updateInitSession(int days) {
		String flag = "fail";//返回状态
		
		Date time = DateTimeUtil.getDateTimeOfDays(new Date(), days);//初始化指定日期
	
		//今天是否已经初始化场次
		boolean isSessionInit = this.checkSessionInit(time);
		if(!isSessionInit){
			List<FiveGaSession> saveList = new ArrayList<FiveGaSession>();
			String theDay = DateTimeUtil.DateToString(time);
			String startDateStr = theDay + FiveConstants.FIVE_START_TIME_STR;//开始时间串
			Date startDate = DateTimeUtil.strToDateMul(startDateStr);//开始时间

			String sessionNo = null;
			Date startTime = null;
			Date endTime = null;
			for (int i = 0; i < FiveConstants.FIVE_MAX_PART; i++) {
				startTime = DateTimeUtil.getDateTimeOfSeconds(startDate,i*FiveConstants.FIVE_TIME_INTERVAL);
				endTime = DateTimeUtil.getDateTimeOfSeconds(startDate,(i+1)*FiveConstants.FIVE_TIME_INTERVAL);
				sessionNo = this.getSessionNo(startDate, i+1);//期号
				
				FiveGaSession session = new FiveGaSession();
				session.setSessionNo(sessionNo);
				session.setStartTime(startTime);
				session.setEndTime(endTime);
				session.setOpenStatus(FiveConstants.FIVE_OPEN_STATUS_INIT);
				saveList.add(session);
			}
			fiveDAO.updateObjectList(saveList, null);
			flag = "success";
		} else {
			flag = "inited";
		}
		return flag;
	}
	
	/**
	 * 检查今天的场次是否已经生成
	 * false=未生成
	 * true=已生成
	 * @return
	 */
	public boolean checkSessionInit(Date now){
		String todayYyyymmdd = DateTimeUtil.DateToString(now);
		Date todayStart = DateTimeUtil.parse(todayYyyymmdd+" 00:00:00");
		Date todayEnd = DateTimeUtil.parse(todayYyyymmdd+" 23:59:59");
		
		HQUtils hq = new HQUtils("from FiveGaSession ho where ho.startTime>? and ho.startTime<?");
		hq.addPars(todayStart);
		hq.addPars(todayEnd);
		Integer count = fiveDAO.countObjects(hq);
		return ParamUtils.chkInteger(count)?true:false;
	}
	
	/**
	 * 根据时间及第几期生成期号
	 * @param now
	 * @param index
	 * @return
	 */
	public String getSessionNo(Date now,int index){
		return DateTimeUtil.dateToString(now, "yyMMdd")+String.format("%03d", index);
	}
	
	public String updateFetchAndOpenResult(Map<String, SessionItem> sessionNoMap) {
		if(sessionNoMap==null || sessionNoMap.isEmpty()) return "fail::no open sessionNo";
		//当前场次及开奖场次处理
		FiveGaSession curtSession = fiveDAO.getCurrentSession();
		if(curtSession==null) return "fail::no current session";
		FiveGaSession lastSession = (FiveGaSession)fiveDAO.getObject(FiveGaSession.class,curtSession.getSessionId()-1);
		if(lastSession==null) return "fail::no last session::id="+curtSession.getSessionId();
		
		//开奖场次期号
		String lastSessionNo = lastSession.getSessionNo();
		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_FC);
		
		//迭代开奖无序
		List<FiveGaSession> openedList = new ArrayList<FiveGaSession>();//开奖场次
		String sNo;//期号
		SessionItem sessionItem;//开奖结果
		String status;//状态
		String result;//开奖号
		try {
			GameHelpUtil.log(gameCode,"-------- OPENing --------");
			for(Map.Entry<String, SessionItem> entry:sessionNoMap.entrySet()){
				sNo = entry.getKey();sessionItem = entry.getValue();
				FiveGaSession session = fiveDAO.getPreviousSessionBySessionNo(sNo);
				if(session!=null){//开奖
					status = session.getOpenStatus();
					result = sessionItem.getResult();
					if(status.equals(GameConstants.OPEN_STATUS_INIT) || status.equals(GameConstants.OPEN_STATUS_OPENING)){
						GameHelpUtil.log(gameCode,"Start ... ["+sNo+"],status="+status+",result="+result);
						long timingOpen = System.currentTimeMillis();
						boolean flag = openFiveSessionOpenResultMethod(session, result);
						if(flag){
							session.setOpenResult(result);//设置开奖号
							session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());//本系统开奖时间
							session.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
							fiveDAO.saveObject(session);
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
			GaSessionInfo sessionInfo = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_XY_FC);
			if(sessionInfo!=null){
				SessionItem lastItem = (SessionItem)sessionNoMap.get(lastSessionNo);//上一期
				if(lastItem!=null){
					sessionInfo.setOpenResult(lastItem.getResult());
					sessionInfo.setOpenSessionNo(lastSessionNo);
					sessionInfo.setEndTime(lastSession.getEndTime());
				}
				sessionInfo.setLatestSessionNo(curtSession.getSessionNo());
				fiveDAO.saveObject(sessionInfo);
				CacheUtil.updateGameList();
				GameHelpUtil.log(gameCode,"gsi last ... ["+(System.currentTimeMillis()-timingGsi)+"ms]"+lastSessionNo+","+lastItem.getResult());
			}
			
			//更新走势 上面已成功开奖的场次
			long startTrending = System.currentTimeMillis();
			for(FiveGaSession session:openedList){
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
	
//	public void updateFetchAndOpenResult(Map<String, SessionItem> sessionNoMap) {
//		FiveGaSession crrrentsession =fiveDAO.getCurrentSession();
//		FiveGaSession tempsession =fiveDAO.getPreviousSessionBySessionNo((Integer.parseInt(crrrentsession.getSessionNo())-1)+"");	
//		if(tempsession!=null){
//		}else{
//			tempsession=(FiveGaSession) fiveDAO.getObject(FiveGaSession.class, (crrrentsession.getSessionId()-1));
//		}
//		if(tempsession != null){
//			// 把资金明细里投注记录状态值改为有效
//			userService.updateUserTradeDetailStatus(tempsession.getSessionNo(), 
//					Constants.GAME_TYPE_XY_FC, Constants.PUB_STATUS_OPEN);
//			
//			String openStatus = tempsession.getOpenStatus();//开奖状态
//			if(openStatus.equals(GameConstants.OPEN_STATUS_INIT) || openStatus.equals(GameConstants.OPEN_STATUS_OPENING)){
//				try {
//					//为了开奖时间更像是从接口获取
//					//Thread.sleep(FiveUtil.getRandomSecond()*1000);
//					
//					if(!ParamUtils.chkString(tempsession.getOpenResult())){	
//						String result = "";
//						Param param=CacheUtil.getParam(Constants.PARAM_FIVE_OPEN_NUM);
//						String value=param.getValue();
//						if(ParamUtils.chkString(value)){
//							Integer openNum=Integer.parseInt(value);
//							result = gaService.getMinResult(tempsession.getSessionNo(),Constants.GAME_TYPE_XY_FC,tempsession.getEstimateResult(),openNum);
//						}
//						if(!ParamUtils.chkString(result)){
//							//result = FiveUtil.getRandomResult(1);
//							SessionItem sessionItem = sessionNoMap.get(tempsession.getSessionNo());
//							result = sessionItem.getResult();
//						}
//						tempsession.setOpenResult(result);
//					}
//
//					boolean flag = this.openFiveSessionOpenResultMethod(tempsession, tempsession.getOpenResult());
//					if(flag){
//						tempsession.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
//						tempsession.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
//						fiveDAO.saveObject(tempsession);
//					}else{
//						GameHelpUtil.log(Constants.GAME_TYPE_XY_FC,"open flag FAIL -_- ["+tempsession.getSessionNo()+"]");
//					}
//					
//					GaSessionInfo sessionInfo=gaService.findGaSessionInfo(Constants.GAME_TYPE_XY_FC);
//					if(sessionInfo!=null){
//						sessionInfo.setLatestSessionNo(crrrentsession.getSessionNo());
//						sessionInfo.setOpenResult(tempsession.getOpenResult());
//						sessionInfo.setOpenSessionNo(tempsession.getSessionNo());
//						sessionInfo.setEndTime(tempsession.getEndTime());
//						fiveDAO.updateObject(sessionInfo, null);
//					}
//				} catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//		}
//	}

//	public void updateFetchAndOpenResult() {
//		FiveGaSession crrrentsession =fiveDAO.getCurrentSession();
//		FiveGaSession tempsession =fiveDAO.getPreviousSessionBySessionNo((Integer.parseInt(crrrentsession.getSessionNo())-1)+"");	
//		if(tempsession!=null){
//		}else{
//			tempsession=(FiveGaSession) fiveDAO.getObject(FiveGaSession.class, (crrrentsession.getSessionId()-1));
//		}
//		if(tempsession != null){
//			String openStatus = tempsession.getOpenStatus();//开奖状态
//			if(openStatus.equals(GameConstants.OPEN_STATUS_INIT) || openStatus.equals(GameConstants.OPEN_STATUS_OPENING)){
//				try {
//					//为了开奖时间更像是从接口获取
//					Thread.sleep(FiveUtil.getRandomSecond()*1000);
//					
//					if(!ParamUtils.chkString(tempsession.getOpenResult())){	
//						String result = "";
//						Param param=CacheUtil.getParam(Constants.PARAM_FIVE_OPEN_NUM);
//						String value=param.getValue();
//						if(ParamUtils.chkString(value)){
//							Integer openNum=Integer.parseInt(value);
//							result = gaService.getMinResult(tempsession.getSessionNo(),Constants.GAME_TYPE_XY_FC,tempsession.getEstimateResult(),openNum);
//						}
//						if(!ParamUtils.chkString(result)){
//							result = FiveUtil.getRandomResult(1);
//						}
//						tempsession.setOpenResult(result);
//					}
//
//					boolean flag = this.openFiveSessionOpenResultMethod(tempsession, tempsession.getOpenResult());
//					if(flag){
//						tempsession.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
//						tempsession.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
//						fiveDAO.saveObject(tempsession);
//						log.info("___[open result success sessionNO["+tempsession.getSessionNo()+"]]");
//					}else{
//						log.info("___[open result fail sessionNO["+tempsession.getSessionNo()+"], please check...]");
//					}
//					
//					GaSessionInfo sessionInfo=gaService.findGaSessionInfo(Constants.GAME_TYPE_XY_FC);
//					if(sessionInfo!=null){
//						sessionInfo.setLatestSessionNo(crrrentsession.getSessionNo());
//						sessionInfo.setOpenResult(tempsession.getOpenResult());
//						sessionInfo.setOpenSessionNo(tempsession.getSessionNo());
//						sessionInfo.setEndTime(tempsession.getEndTime());
//						fiveDAO.updateObject(sessionInfo, null);
//					}
//				} catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//		}
//	}
	
	public void updateTrendResult(FiveGaSession session){
		if(session.getOpenStatus().equals(GameConstants.OPEN_STATUS_OPENED)){
			List<FiveGaTrend> list=fiveDAO.findFiveGaTrendAllList();
			List<FiveGaTrend> savelist=new ArrayList<FiveGaTrend>();
			Map<String,Boolean> map=getTrendResult(session.getOpenResult());
			if(!map.isEmpty()){
				for(FiveGaTrend trend:list){
					if(map.get(trend.getTrendTitle())!=null&&map.get(trend.getTrendTitle())==true){
						trend.setTrendCount(trend.getTrendCount()+1);
					}else{
						trend.setTrendCount(0);
					}
					savelist.add(trend);
				}
				fiveDAO.updateObjectList(savelist, null);
			}
		}
	}
	
//	public void updateFetchAndOpenTrend(Integer count){
//		if(count==9){//执行10次
//			count=null;
//			return;
//		}
//		
//		FiveGaSession session =fiveDAO.getPreviousSessionBySessionNo((Integer.parseInt(fiveDAO.getCurrentSession().getSessionNo())-1)+"");	
//		if(session!=null){
//		}else{
//			session=(FiveGaSession) fiveDAO.getObject(FiveGaSession.class, (fiveDAO.getCurrentSession().getSessionId()-1));
//		}
//		if(session.getOpenStatus().equals(GameConstants.OPEN_STATUS_OPENED)){
//			List<FiveGaTrend> list=fiveDAO.findFiveGaTrendAllList();
//			List<FiveGaTrend> savelist=new ArrayList<FiveGaTrend>();
//			Map<String,Boolean> map=getTrendResult(session.getOpenResult());
//			if(!map.isEmpty()){
//				for(int i=0;i<list.size();i++){
//					FiveGaTrend trend=list.get(i);
//					if(map.get(trend.getTrendTitle())!=null&&map.get(trend.getTrendTitle())==true){
//						trend.setTrendCount(trend.getTrendCount()+1);
//					}else{
//						trend.setTrendCount(0);
//					}
//					savelist.add(trend);
//				}
//				fiveDAO.updateObjectList(savelist, null);
//				count=null;
//				list=null;
//				savelist=null;
//				map.clear();
//				map=null;
//				session=null;
//				return;
//			}
//			count=null;
//		}else{
//			Thread t1=new Thread(){
//	            public void run(){
//	               try {
//	            	   sleep(3000);
//	            	   interrupt();
//	               } catch (Exception e) {
//	            	   interrupt();
//	               }
//	            }
//			};
//			t1.start();
//			try {
//				t1.join();
//				t1=null;
//				count++;
//				session=null;
//				updateFetchAndOpenTrend(count);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	public Map<String,Boolean> getTrendResult(String results){
		Map<String,Boolean> map=new HashMap<String,Boolean>();
		if(ParamUtils.chkString(results)){
			String array[]=results.split(",");
			int sum=0;
			for(int i=0;i<array.length;i++){
				if(Integer.parseInt(array[i])%2==0){
						map.put("第"+(i+1)+"球 双", true);
				}else{
						map.put("第"+(i+1)+"球 单", true);
				}		
				if(Integer.parseInt(array[i])>=5){
						map.put("第"+(i+1)+"球 大", true);
				}else{
					map.put("第"+(i+1)+"球 小", true);
				}
				sum=sum+Integer.parseInt(array[i]);
			}
			if(sum%2==0){
				map.put("总和双", true);
			}else{
				map.put("总和单", true);
			}
			if(sum>=23){
				map.put("总和大", true);
			}else{
				map.put("总和小", true);
			}
			if(Integer.parseInt(array[0])==Integer.parseInt(array[4])){
				map.put("和", true);
			}else if(Integer.parseInt(array[0])>Integer.parseInt(array[4])){
				map.put("龙", true);
			}else if(Integer.parseInt(array[0])<Integer.parseInt(array[4])){
				map.put("虎", true);
			}

		}
		return map;
	}
	
	
	/**
	 * 开奖方法，计算所有投注用户的结果并更新相关数据和状态
	 * @param sessionNo
	 * @param result 开奖号10组数字 英文逗号连接
	 * @return
	 */
	public boolean openFiveSessionOpenResultMethod(FiveGaSession session,String result){
		String gameCode = Constants.getGameCodeOfGameType(Constants.GAME_TYPE_XY_FC);
		try{
			List<Object> para = new ArrayList<Object>();
			StringBuffer hql = new StringBuffer();
			hql.append(" and ho.gameType=?");
			para.add(Constants.GAME_TYPE_XY_FC);
			hql.append( " and ho.sessionId =?");
			para.add(session.getSessionId());
			hql.append(" and ho.betFlag=?" );//目前固定参数
			para.add("1");
			//hql.append(" order by ho.betTime desc");
			
			//本期全部投注记录
			long startTiming = System.currentTimeMillis();
			List<GaBetDetail> list=gaService.findGaBetDetailList(hql.toString(), para);
			GameHelpUtil.log(gameCode,"BETS ... ["+list.size()+"]["+session.getSessionNo()+"]["+(System.currentTimeMillis()-startTiming)+"ms]");
			//本期投注统计表
			FiveGaBet bet=new FiveGaBet();
			BigDecimal totalPoint=new BigDecimal(0);//总投注
			BigDecimal betCash=new BigDecimal(0);//总中奖
			bet.setSessionId(session.getSessionId());
			bet.setSessionNo(session.getSessionNo());
			
			if(list!=null && !list.isEmpty()){
				//开奖投注用户ids --by.cuisy.20171209
				List<Integer> userIds = new ArrayList<Integer>();
				startTiming = System.currentTimeMillis();
				for(GaBetDetail detail:list){
					//开奖时把需要sum更新余额的用户id统计放入开奖及明细写入后批量更新 --by.cuisy.20171209
					if(!StringUtil.chkListIntContains(userIds, detail.getUserId())){
						userIds.add(detail.getUserId());
					}//~
					
					BigDecimal wincash = judgeWin(result,detail);//判断中奖 //0未中奖  1打和  2中奖
					detail.setOpenResult(result);
					
					if(wincash.compareTo(new BigDecimal(0)) == 1){//中奖
						detail.setWinResult(GameConstants.WIN);
						//中奖金额
						wincash = GameHelpUtil.round(wincash);
						detail.setWinCash(wincash);
						//统计
						totalPoint = totalPoint.add(new BigDecimal(detail.getBetMoney()));
						betCash = betCash.add(wincash);
						//盈亏
						detail.setPayoff(GameHelpUtil.round(wincash.subtract(new BigDecimal(detail.getBetMoney()))));
						//备注
						String remark=GameHelpUtil.getRemark(Constants.CASH_TYPE_CASH_PRIZE, wincash);
						//更新
						updateOpenData(detail,remark);
						
					}else if(wincash.compareTo(new BigDecimal(0)) == 0){//打和
						detail.setWinResult(GameConstants.WIN_HE);
						//中奖金额
						wincash = GameHelpUtil.round(new BigDecimal(detail.getBetMoney()));//投注总钱数
						detail.setWinCash(wincash);
						//统计
						totalPoint=totalPoint.add(new BigDecimal(detail.getBetMoney()));
						betCash=betCash.add(wincash);
						//盈亏
						detail.setPayoff(new BigDecimal(0));
						//备注
						String remark=GameHelpUtil.getRemark(Constants.CASH_TYPE_CASH_PRIZE, wincash);
						//更新
						updateOpenData(detail,remark.toString());
						
					}else{//未中奖
						//统计
						totalPoint=totalPoint.add(new BigDecimal(detail.getBetMoney()));
						detail.setWinCash(new BigDecimal(0));
						detail.setWinResult(GameConstants.WIN_NOT);//未中奖
						fiveDAO.saveObject(detail);
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
						Constants.GAME_TYPE_XY_FC, Constants.PUB_STATUS_OPEN);
				GameHelpUtil.log(gameCode,"BatD ... ["+(System.currentTimeMillis()-timginUtds)+"ms]");
			}
			bet.setTotalPoint(GameHelpUtil.round(totalPoint));
			bet.setWinCash(GameHelpUtil.round(betCash));
			fiveDAO.saveObject(bet);
			return true;
		}catch(Exception e){
			//e.printStackTrace();
			GameHelpUtil.log(gameCode,"open err::["+session.getSessionNo()+"]=>"+e.getMessage(),e);
			return false;
		}	
	}
//	public boolean openFiveSessionOpenResultMethod(FiveGaSession session,String result){
//		try{
////			FiveGaSession session =fiveDAO.getPreviousSessionBySessionNo(sessionNo);
//			List<Object> para = new ArrayList<Object>();
//			StringBuffer hql = new StringBuffer();
//			hql.append(" and ho.gameType=? ");// 0=三份彩  1=北京赛车  2=幸运28  3=重庆时时彩  4=PC蛋蛋  5=广东快乐10分
//			para.add(Constants.GAME_TYPE_XY_FC);
//			hql.append(" and ho.sessionId=? ");
//			para.add(session.getSessionId());
//			hql.append(" and ho.betFlag=? " );
//			para.add("1");
//			hql.append(" order by ho.betTime desc");
//			List<GaBetDetail> list=gaService.findGaBetDetailList(hql.toString(), para);
//			
//			FiveGaBet bet=new FiveGaBet();
//			BigDecimal  totalPoint=new BigDecimal(0);
//			BigDecimal  betCash=new BigDecimal(0);
//			bet.setSessionId(session.getSessionId());
//			bet.setSessionNo(session.getSessionNo());
//
//			if(list!=null&&list.size()>0){
//				//开奖投注用户ids --by.cuisy.20171209
//				List<Integer> userIds = new ArrayList<Integer>();
//				
//				for(GaBetDetail detail:list){
//					
//					Integer userId = detail.getUserId();
//					//开奖时把需要sum更新余额的用户id统计放入开奖及明细写入后批量更新 --by.cuisy.20171209
//					if(!StringUtil.chkListIntContains(userIds, userId)){
//						userIds.add(userId);
//					}//~
//					
//					User temp=(User) gaService.getObject(User.class, userId);
//					String userType=temp.getUserType();
//
//						
//					
//						BigDecimal wincash = judgeWin(result,detail);//0未中奖  1打和  2中奖
//						detail.setOpenResult(result);
//						if(wincash.compareTo(new BigDecimal(0)) == 1){//中奖
//							detail.setWinResult(FiveConstants.FIVE_WIN);//中奖
//							wincash=wincash.setScale(2, BigDecimal.ROUND_HALF_UP);
//							detail.setWinCash(wincash);
//							totalPoint=totalPoint.add(new BigDecimal(detail.getBetMoney()));
//							betCash=betCash.add(wincash);
//							detail.setPayoff(wincash.subtract(new BigDecimal(detail.getBetMoney())).setScale(2, BigDecimal.ROUND_HALF_UP));
//							BigDecimal userBal=null;
////							User user=(User) gaService.getObject(User.class, detail.getUserId());
////							if(user.getUserBalance()!=null){
////								userBal=user.getUserBalance();
////							}else{
////								userBal=new BigDecimal(0);
////							}
////							user.setUserBalance(userBal.add(wincash).setScale(2, BigDecimal.ROUND_HALF_UP));
////							userService.saveTradeDetail(user,detail.getUserId(), Constants.TRADE_TYPE_INCOME, Constants.CASH_TYPE_CASH_BET_FIVE, wincash, detail.getBetDetailId());			
//							StringBuffer remark=new StringBuffer();
//							remark.append("彩票中奖 奖金 ")
//							    .append(wincash).append("元");
//							try {
//								updateOpenData(detail,null,remark.toString());
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}else if(wincash.compareTo(new BigDecimal(0)) == 0){//打和
//							detail.setWinResult(FiveConstants.FIVE_WIN_HE);//打和
//							wincash=new BigDecimal(detail.getBetMoney());//投注总钱数
////							User user=(User) userService.getObject(User.class, detail.getUserId());
////							BigDecimal userBal=null;
////							if(user.getUserBalance()!=null){
////								userBal=user.getUserBalance();
////							}else{
////								userBal=new BigDecimal(0);
////							}
////							user.setUserBalance(userBal.add(wincash));
//
//							detail.setWinCash(wincash);
////								userService.saveTradeDetail(user,detail.getUserId(), Constants.TRADE_TYPE_INCOME, Constants.CASH_TYPE_CASH_BET_FIVE, wincash, detail.getBetDetailId());	
//							StringBuffer remark=new StringBuffer();
//							remark.append("彩票打和 退款")
//							    .append(wincash).append("元");
//							try {
//								updateOpenData(detail,null,remark.toString());
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//
//							totalPoint=totalPoint.add(new BigDecimal(detail.getBetMoney()));
//							betCash=betCash.add(wincash);
//						}else{//未中奖
//							totalPoint=totalPoint.add(new BigDecimal(detail.getBetMoney()));
//							detail.setWinCash(new BigDecimal(0));
//							detail.setWinResult(FiveConstants.FIVE_WIN_NOT);//未中奖
//							try {
//								fiveDAO.updateObject(detail, null);
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//				}	
//				//批量更新开奖用户实时余额 --by.cuisy.20171209
//				userService.updateUserMoney(userIds);
//			}
//			bet.setTotalPoint(totalPoint);
//			bet.setWinCash(betCash);
//			fiveDAO.saveObject(bet, null);
//			return true;
//		}catch(Exception e){
//			e.printStackTrace();
//			return false;
//		}
//		
//	}
	
	public void updateOpenData(GaBetDetail detail,String remark){
		fiveDAO.saveObject(detail);
		userService.saveTradeDetail(null,detail.getUserId(), 
				Constants.TRADE_TYPE_INCOME,
				Constants.CASH_TYPE_CASH_PRIZE, detail.getWinCash(), 
				detail.getBetDetailId(), 
				Constants.GAME_TYPE_XY_FC,
				remark,detail.getSessionNo(),detail.getType(),detail.getLoginName());
	}
	
//	public void updateOpenData(GaBetDetail detail,User user,String remark){
//		fiveDAO.saveObject(detail);
//		user=userService.saveTradeDetail(user,detail.getUserId(), Constants.TRADE_TYPE_INCOME,Constants.CASH_TYPE_CASH_PRIZE, detail.getWinCash(), detail.getBetDetailId(), Constants.GAME_TYPE_XY_GDK10,remark);
//	}

	public FiveGaSession getCurrentSession(){
		return fiveDAO.getCurrentSession();
	}
	public FiveGaSession getPreviousSessionBySessionNo(String sessionNo){
		return fiveDAO.getPreviousSessionBySessionNo(sessionNo);
	}
	
	public User saveUserBetInfo(String room,Map<String,Integer> betMap,List<GaBetOption> list,FiveGaSession session,User user,BigDecimal betAll){
		//彩种缓存
		GaSessionInfo gsi = CacheUtil.getGaSessionInfo(Constants.GAME_TYPE_XY_FC);
		//投注与明细关联
		List<UserTradeDetailRl> rlList = new ArrayList<UserTradeDetailRl>();
		//用户类型
		String userType = user.getUserType();
		
		String id;//optionId
		Integer p;//money
		String optionTitle;
		GaBetOption betOption = null;
		for(Map.Entry<String, Integer> entry:betMap.entrySet()){
			//初始化
			id = entry.getKey();
			p = entry.getValue();
			optionTitle = "";
			
			String[] arrId = id.split("\\|");// 有可能是0,1|0,1这种形式
			for (String arr : arrId) {// 查找所有的投注号
				List<GaBetOption> optionList = gaService.getGaBetOptionByIds(arr);//此注的所有投注号对象
				betOption = optionList.get(0);
				if (ParamUtils.chkString(optionTitle)) {
					optionTitle = optionTitle + "|";
				}
				for (GaBetOption gbo : optionList) {
					optionTitle = optionTitle + gbo.getOptionTitle()+",";
				}
				optionTitle = optionTitle.substring(0, optionTitle.length()-1);
			}
			
			//投注记录
			GaBetDetail betDetail = new GaBetDetail();
			betDetail.setBetRate(betOption.getBetRate());
			betDetail.setWinResult(GameConstants.OPEN_STATUS_INIT);//未开奖
			betDetail.setBetFlag(GameConstants.STATUS_1);//有效投注
			betDetail.setSessionId(session.getSessionId());
			
			betDetail.setBetOptionId(betOption.getBetOptionId());
			betDetail.setBetTime(DateTimeUtil.getNow());//投注时间
			betDetail.setBetMoney(p);
			betDetail.setUserId(user.getUserId());
			betDetail.setType(userType);
			betDetail.setLoginName(user.getLoginName());
			betDetail.setRoom(room);
			betDetail.setSessionNo(session.getSessionNo());
			betDetail.setGameName(gsi.getGameTitle());
			
			if (betOption.getPlayType().equals("0")) {
				betDetail.setPlayName("两面盘");
			} else if (betOption.getPlayType().equals("1")) {
				betDetail.setPlayName("1-5球");
			} else if (betOption.getPlayType().equals("2")) {
				betDetail.setPlayName("前二直选");
			} else if (betOption.getPlayType().equals("3")) {
				betDetail.setPlayName("后二直选");
			} else if (betOption.getPlayType().equals("4")) {
				betDetail.setPlayName("前三直选");
			} else if (betOption.getPlayType().equals("5")) {
				betDetail.setPlayName("中三直选");
			} else if (betOption.getPlayType().equals("6")) {
				betDetail.setPlayName("后三直选");
			} else if (betOption.getPlayType().equals("7")) {
				betDetail.setPlayName("前二组选");
			} else if (betOption.getPlayType().equals("8")) {
				betDetail.setPlayName("后二组选");
			} else if (betOption.getPlayType().equals("9")) {
				betDetail.setPlayName("前三组三");
			} else if (betOption.getPlayType().equals("10")) {
				betDetail.setPlayName("中三组三");
			} else if (betOption.getPlayType().equals("11")) {
				betDetail.setPlayName("后三组三");
			} else if (betOption.getPlayType().equals("12")) {
				betDetail.setPlayName("前三组六");
			} else if (betOption.getPlayType().equals("13")) {
				betDetail.setPlayName("中三组六");
			} else if (betOption.getPlayType().equals("14")) {
				betDetail.setPlayName("后三组六");
			} else if (betOption.getPlayType().equals("15")) {
				betDetail.setPlayName("前三");
			} else if (betOption.getPlayType().equals("16")) {
				betDetail.setPlayName("中三");
			} else if (betOption.getPlayType().equals("17")) {
				betDetail.setPlayName("后三");
			}
			betDetail.setBetName(betOption.getTitle());
			betDetail.setOptionTitle(optionTitle);
			betDetail.setGameType(Constants.GAME_TYPE_XY_FC);
			
			//不能为空字段初始化
			betDetail.setBetId(GameConstants.DEF_ID);//目前未用默认0
			betDetail.setPaperMoney(new BigDecimal(GameConstants.DEF_NUMBER));//目前未用默认0 红包
			betDetail.setWinCash(new BigDecimal(GameConstants.DEF_NUMBER));//初始值
			betDetail.setPayoff(new BigDecimal(GameConstants.DEF_NUMBER));//初始值
			
			fiveDAO.saveObject(betDetail);
			
			//关联构造
			UserTradeDetailRl rl = new UserTradeDetailRl();
			rl.setBetDetailId(betDetail.getBetDetailId());
			rlList.add(rl);
		}
		
		//更新账户信息
		String remark = GameHelpUtil.getRemark(Constants.CASH_TYPE_CASH_BUY_LOTO, betAll);
		Integer tradeDetailId = userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY,Constants.CASH_TYPE_CASH_BUY_LOTO, betAll, null, 
				Constants.GAME_TYPE_XY_FC,remark,session.getSessionNo(),user.getUserType(),user.getLoginName());

		//更新用户实时余额  --by.cuisy.20171209
		userService.updateUserMoney(user.getUserId());
		userService.updateUserBanlance(user.getUserId());
		
		//保存关联
		for(UserTradeDetailRl rl:rlList){
			rl.setTradeDetailId(tradeDetailId);
			rl.setGfxy(Constants.GAME_PLAY_CATE_XY);
		}
		fiveDAO.updateObjectList(rlList, null);
		
		return  user;
	}
	
//	public User saveUserBetInfo(String room,Map<String,Integer> betMap,List<GaBetOption> list,FiveGaSession gaK10Session,User user,BigDecimal betAll){
//		List<UserTradeDetailRl> rlList=new ArrayList<UserTradeDetailRl>();
//		String estimateResult=gaK10Session.getEstimateResult();//预设结果
//		String[] array = null;//预设开奖结果数组
//		if(ParamUtils.chkString(estimateResult)){
//			array=estimateResult.split("\\|");
//		}
//		String userType = user.getUserType();
//
//		BigDecimal paperMoney=user.getUserScore();
//		if(paperMoney==null) paperMoney = new BigDecimal(0);//判空处理
//		BigDecimal tempMoney=new BigDecimal(0);
//		
//		for (Map.Entry<String, Integer> entry : betMap.entrySet()) { 
//			String id = entry.getKey();//
//			Integer p = entry.getValue();// 投注金额
//			String optionTitle = "";
//			
//			GaBetOption betOption = null;
//			String[] arrId = id.split("\\|");// 有可能是0,1|0,1这种形式
//			for (String arr : arrId) {// 查找所有的投注号
//				List<GaBetOption> optionList = gaService.getGaBetOptionByIds(arr);//此注的所有投注号对象
//				betOption = optionList.get(0);
//				if (ParamUtils.chkString(optionTitle)) {
//					optionTitle = optionTitle + "|";
//				}
//				for (GaBetOption gbo : optionList) {
//					optionTitle = optionTitle + gbo.getOptionTitle()+",";
//				}
//				optionTitle = optionTitle.substring(0, optionTitle.length()-1);
//			}
//			GaBetDetail betDetail=new GaBetDetail();
//			if(betOption!=null){
//				betDetail.setBetRate(betOption.getBetRate());
//			}
//			betDetail.setWinResult("0");//未开奖
//			betDetail.setBetFlag("1");//有效投注
//			betDetail.setSessionId(gaK10Session.getSessionId());
//			
//			betDetail.setBetOptionId(betOption.getBetOptionId());
//			betDetail.setBetTime(new Date());
//			betDetail.setBetMoney(p);
//			
//			betDetail.setUserId(user.getUserId());
//			
//			if(paperMoney.compareTo(new BigDecimal(0))==1){
//				if(new BigDecimal(p).compareTo(paperMoney)!=-1){
//					betDetail.setPaperMoney(paperMoney);
//					paperMoney=paperMoney.subtract(paperMoney);
//					tempMoney=tempMoney.add(new BigDecimal(p));
//				}else{
//					betDetail.setPaperMoney(new BigDecimal(p));
//					paperMoney=paperMoney.subtract(new BigDecimal(p));
//					tempMoney=tempMoney.add(new BigDecimal(p));
//				}
//			}
//			
//			betDetail.setRoom(room);
//			betDetail.setSessionNo(gaK10Session.getSessionNo());
//			betDetail.setGameName("五分彩");
//			if (betOption.getPlayType().equals("0")) {
//				betDetail.setPlayName("两面盘");
//			} else if (betOption.getPlayType().equals("1")) {
//				betDetail.setPlayName("1-5球");
//			} else if (betOption.getPlayType().equals("2")) {
//				betDetail.setPlayName("前二直选");
//			} else if (betOption.getPlayType().equals("3")) {
//				betDetail.setPlayName("后二直选");
//			} else if (betOption.getPlayType().equals("4")) {
//				betDetail.setPlayName("前三直选");
//			} else if (betOption.getPlayType().equals("5")) {
//				betDetail.setPlayName("中三直选");
//			} else if (betOption.getPlayType().equals("6")) {
//				betDetail.setPlayName("后三直选");
//			} else if (betOption.getPlayType().equals("7")) {
//				betDetail.setPlayName("前二组选");
//			} else if (betOption.getPlayType().equals("8")) {
//				betDetail.setPlayName("后二组选");
//			} else if (betOption.getPlayType().equals("9")) {
//				betDetail.setPlayName("前三组三");
//			} else if (betOption.getPlayType().equals("10")) {
//				betDetail.setPlayName("中三组三");
//			} else if (betOption.getPlayType().equals("11")) {
//				betDetail.setPlayName("后三组三");
//			} else if (betOption.getPlayType().equals("12")) {
//				betDetail.setPlayName("前三组六");
//			} else if (betOption.getPlayType().equals("13")) {
//				betDetail.setPlayName("中三组六");
//			} else if (betOption.getPlayType().equals("14")) {
//				betDetail.setPlayName("后三组六");
//			} else if (betOption.getPlayType().equals("15")) {
//				betDetail.setPlayName("前三");
//			} else if (betOption.getPlayType().equals("16")) {
//				betDetail.setPlayName("中三");
//			} else if (betOption.getPlayType().equals("17")) {
//				betDetail.setPlayName("后三");
//			}
//			betDetail.setBetName(betOption.getTitle());
//			betDetail.setOptionTitle(optionTitle);
//			betDetail.setGameType(Constants.GAME_TYPE_XY_FC);
//			
//			if(!Constants.USER_TYPE_TEST.equals(userType)){
//				if(array != null && array.length >0){
//					for(int j=0;j<array.length;j++){
//						String openResult=array[j];
//						BigDecimal wincash=judgeWin(openResult,betDetail);
//						if(wincash.compareTo(new BigDecimal(0)) == 1){//中奖	
//							wincash=wincash.setScale(2, BigDecimal.ROUND_HALF_UP);
//						}else if(wincash.compareTo(new BigDecimal(0)) == 0){//打和
//							wincash=new BigDecimal(betDetail.getBetMoney());	
//						}
//						if(j==0){
//							betDetail.setOneMoney(wincash);
//						}else if(j==1){
//							betDetail.setTwoMoney(wincash);
//						}else if(j==2){
//							betDetail.setThreeMoney(wincash);	
//						}else if(j==3){
//							betDetail.setFourMoney(wincash);
//						}else if(j==4){
//							betDetail.setFiveMoney(wincash);
//						}
//					}
//				}
//			}
//
//			UserTradeDetailRl  rl=new UserTradeDetailRl();
//			betDetail=(GaBetDetail)fiveDAO.saveObjectDB(betDetail);
//			rl.setBetDetailId(betDetail.getBetDetailId());
//			rlList.add(rl);
//		}
//		//更新收益
////		BigDecimal userBalance=user.getUserBalance();
////		if(userBalance==null){
////			userBalance=new BigDecimal(0);
////		}
////		user.setUserBalance(userBalance.subtract(betAll));
////		//更新收益
////		BigDecimal dayBet=user.getDayBet();
////		if(dayBet==null){
////			dayBet=new BigDecimal(0);
////		}
////		user.setDayBet(dayBet.add(betAll));
//		
//		if(paperMoney.compareTo(betAll)==1||paperMoney.compareTo(betAll)==0){//红包足够投注，不用使用余额里面的钱
//			user.setUserScore(user.getUserScore().subtract(tempMoney));
//			fiveDAO.updateObject(user, null);
//		}else{
////			user=userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY, Constants.CASH_TYPE_CASH_BET_FIVE, betAll.subtract(tempMoney), null);
//			StringBuilder remark = new StringBuilder();
//			remark.append("购买彩票 扣款 ")
//			    .append(betAll.subtract(tempMoney)).append("元");
////			user=userService.saveTradeDetail(user,user.getUserId(), Constants.TRADE_TYPE_PAY,Constants.CASH_TYPE_CASH_BUY_LOTO, betAll.subtract(tempMoney), null, Constants.GAME_TYPE_FIVE,remark.toString());
//			Integer tradeDetailId=userService.saveTradeDetail(null,user.getUserId(), Constants.TRADE_TYPE_PAY,Constants.CASH_TYPE_CASH_BUY_LOTO, betAll.subtract(tempMoney), null, 
//					Constants.GAME_TYPE_XY_FC,remark.toString(),gaK10Session.getSessionNo(),user.getUserType(),user.getLoginName());
//
//			//更新用户实时余额  --by.cuisy.20171209
//			userService.updateUserMoney(user.getUserId());
//			userService.updateUserBanlance(user.getUserId());
//			
//			for(UserTradeDetailRl rl:rlList){
//				rl.setTradeDetailId(tradeDetailId);
//	rl.setGfxy(Constants.GAME_PLAY_CATE_XY);
//			}
//			fiveDAO.updateObjectList(rlList, null);
//
//		}
//		return  user;
//	}
	
	 public String getBetNameByOptionType(String playType,String optionType){
		 if(playType.equals("0")){//两面盘
			 if(optionType.equals("0")){
				 return "总和/龙虎";
			 }else{
				 return "第"+(Integer.parseInt(optionType))+"球";
			 }
		 }else{//1-5球
				 return "第"+(Integer.parseInt(optionType)+1)+"球";
		 }
	 }
	 
	public List<FiveGaTrend> findFiveGaTrendList(){
		return fiveDAO.findFiveGaTrendList();
	}
	public PaginationSupport  findFiveGaSessionList(String hql, List<Object> para,int pageNum,int pageSize){
		return fiveDAO.findFiveGaSessionList(hql,para,pageNum,pageSize);
	}
	public PaginationSupport  findFiveGaBetList(String hql, List<Object> para,int pageNum,int pageSize){
		return fiveDAO.findFiveGaBetList(hql,para,pageNum,pageSize);
	}
	/**
	 * 判断用户是否中奖   返回结果      >0中奖    =0打和     <0未中奖
	 * @param results
	 * @param detail
	 * @return
	 */
	public BigDecimal judgeWin(String results,GaBetDetail detail){
		String array[]=results.split(",");//拆分结果
		if(detail.getPlayName().equals("两面盘")){//先用中文比对吧  后续改进
			// 中奖金额
			BigDecimal winMoney = detail.getBetRate().multiply(new BigDecimal(detail.getBetMoney()));
			if(detail.getBetName().equals("总和/龙虎")){
				Map<String,Boolean>  map=getResult(array);
				if(detail.getOptionTitle().equals("龙")||detail.getOptionTitle().equals("虎")){
					if(map.get(detail.getOptionTitle())!=null&&map.get(detail.getOptionTitle())==true){
						return winMoney;
					}else{
						if(map.get("和")!=null&&map.get("和")){
							return new BigDecimal(0);
						}else{
							return new BigDecimal(-1);
						}
					}
				}else{
					if(map.get(detail.getOptionTitle())!=null&&map.get(detail.getOptionTitle())==true){
						return winMoney;
					}else{
						return new BigDecimal(-1);
					}
				}
			}else{
				int seq=Integer.parseInt(detail.getBetName().substring(1, detail.getBetName().length()-1));
				Map<String,Boolean>  map=getSingleBallResult(array[seq-1]);
				if(map.get(detail.getOptionTitle())!=null&&map.get(detail.getOptionTitle())==true){
					return winMoney;
				}else{
					return new BigDecimal(-1);
				}
			}
		} else if (detail.getPlayName().equals("1-5球")) {//第1-5球
			int seq=Integer.parseInt(detail.getBetName().substring(1, detail.getBetName().length()-1));
			int value=Integer.parseInt(array[seq-1]);//第几球的值
			//下注的具体是几号
			int index=Integer.parseInt(detail.getOptionTitle().replaceAll("号", ""));//把几号的号字去掉只保留数字
			if(value==index){
				return detail.getBetRate().multiply(new BigDecimal(detail.getBetMoney()));
			}else{
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("前二直选")) {// 
			// 规则  前二、后二0~9任选2个号进行投注，当开奖结果与所选的号码相同时（且顺序一致），即为中奖。如02749，前二直选02收买中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1|0,1
			String[] optionArr = optionTitle.split("\\|");
			String[] firstArr = optionArr[0].split(",");// 第一位号
			String[] secondArr = optionArr[1].split(",");// 第二位号
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(firstArr.length * secondArr.length));
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			for (String first : firstArr) {
				if (array[0].equals(first)) flag1 = true;
			}
			for (String second : secondArr) {
				if (array[1].equals(second)) flag2 = true;
			}
			if (flag1 && flag2) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("后二直选")) {// 
			// 规则  前二、后二0~9任选2个号进行投注，当开奖结果与所选的号码相同时（且顺序一致），即为中奖。如02749，前二直选02收买中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1|0,1
			String[] optionArr = optionTitle.split("\\|");
			String[] firstArr = optionArr[0].split(",");// 第一位号
			String[] secondArr = optionArr[1].split(",");// 第二位号
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(firstArr.length * secondArr.length));
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			for (String first : firstArr) {
				if (array[3].equals(first)) flag1 = true;
			}
			for (String second : secondArr) {
				if (array[4].equals(second)) flag2 = true;
			}
			if (flag1 && flag2) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("前三直选")) {// 
			// 规则 前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（且顺序一致），
			      //即为中奖。如12356，前三直选买123则为中奖；如13259，则中三买325则为中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1|0,1
			String[] optionArr = optionTitle.split("\\|");
			String[] firstArr = optionArr[0].split(",");// 第一位号
			String[] secondArr = optionArr[1].split(",");// 第二位号
			String[] thirdArr = optionArr[2].split(",");// 第三位号
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(firstArr.length * secondArr.length * thirdArr.length));
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			boolean flag3 = false;// 第三位是否相等
			for (String first : firstArr) {
				if (array[0].equals(first)) flag1 = true;
			}
			for (String second : secondArr) {
				if (array[1].equals(second)) flag2 = true;
			}
			for (String third : thirdArr) {
				if (array[2].equals(third)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("中三直选")) {// 
			// 规则 前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（且顺序一致），
		      //即为中奖。如12356，前三直选买123则为中奖；如13259，则中三买325则为中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1|0,1
			String[] optionArr = optionTitle.split("\\|");
			String[] firstArr = optionArr[0].split(",");// 第一位号
			String[] secondArr = optionArr[1].split(",");// 第二位号
			String[] thirdArr = optionArr[2].split(",");// 第三位号
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(firstArr.length * secondArr.length * thirdArr.length));
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			boolean flag3 = false;// 第三位是否相等
			for (String first : firstArr) {
				if (array[1].equals(first)) flag1 = true;
			}
			for (String second : secondArr) {
				if (array[2].equals(second)) flag2 = true;
			}
			for (String third : thirdArr) {
				if (array[3].equals(third)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("后三直选")) {// 
			// 规则 前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（且顺序一致），
		      //即为中奖。如12356，前三直选买123则为中奖；如13259，则中三买325则为中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1|0,1
			String[] optionArr = optionTitle.split("\\|");
			String[] firstArr = optionArr[0].split(",");// 第一位号
			String[] secondArr = optionArr[1].split(",");// 第二位号
			String[] thirdArr = optionArr[2].split(",");// 第三位号
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(firstArr.length * secondArr.length * thirdArr.length));
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			boolean flag3 = false;// 第三位是否相等
			for (String first : firstArr) {
				if (array[2].equals(first)) flag1 = true;
			}
			for (String second : secondArr) {
				if (array[3].equals(second)) flag2 = true;
			}
			for (String third : thirdArr) {
				if (array[4].equals(third)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("前二组选")) {//
			// 规则 前二、后二0~9任选2个号进行投注，当开奖结果与所选的号码相同时（顺序不限），即为中奖。如02749，前二组选买02或20都中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(getBetNum(optionArr.length, 2)));
			for (String option : optionArr) {
				if (array[0].equals(option)) flag1 = true;
				if (array[1].equals(option)) flag2 = true;
			}
			if (flag1 && flag2) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("后二组选")) {// 
			// 规则 前二、后二0~9任选2个号进行投注，当开奖结果与所选的号码相同时（顺序不限），即为中奖。如02749，前二组选买02或20都中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			boolean flag1 = false;// 第一位是否相等
			boolean flag2 = false;// 第二位是否相等
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(getBetNum(optionArr.length, 2)));
			for (String option : optionArr) {
				if (array[3].equals(option)) flag1 = true;
				if (array[4].equals(option)) flag2 = true;
			}
			if (flag1 && flag2) {// 中奖(此玩法只可能中一注)
				return detail.getBetRate().multiply(oneBetMoney);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("前三组三")) {// 
			// 规则    开奖号码中的3个数字有两个相同，即为组三。前三、中三、后三组三，0~9至少任选2个号进行复式投注，
				 //投注的号码与中奖号码相同（顺序不限），则为中奖。例如，前三组三选择号码4、5 ，将组合成2注（一对4一个5和一对5一个4），
				 //开奖结果为： 44512 、 45412 、 54412、45512、54512、55412 之一皆中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(optionArr.length*(optionArr.length-1)));
			boolean flag1 = false;
			boolean flag2 = false;
			if (array[0].equals(array[1])) {
				for (String option : optionArr) {
					if (option.equals(array[0])) flag1 = true;
					if (option.equals(array[2])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[0].equals(array[2])) {
				for (String option : optionArr) {
					if (option.equals(array[0])) flag1 = true;
					if (option.equals(array[1])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[1].equals(array[2])) {
				for (String option : optionArr) {
					if (option.equals(array[1])) flag1 = true;
					if (option.equals(array[0])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (!flag1 || !flag2) return new BigDecimal(-1);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("中三组三")) {// 
			// 规则    开奖号码中的3个数字有两个相同，即为组三。前三、中三、后三组三，0~9至少任选2个号进行复式投注，
			 //投注的号码与中奖号码相同（顺序不限），则为中奖。例如，前三组三选择号码4、5 ，将组合成2注（一对4一个5和一对5一个4），
			 //开奖结果为： 44512 、 45412 、 54412、45512、54512、55412 之一皆中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(optionArr.length*(optionArr.length-1)));
			boolean flag1 = false;
			boolean flag2 = false;
			if (array[1].equals(array[2])) {
				for (String option : optionArr) {
					if (option.equals(array[1])) flag1 = true;
					if (option.equals(array[3])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[1].equals(array[3])) {
				for (String option : optionArr) {
					if (option.equals(array[1])) flag1 = true;
					if (option.equals(array[2])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[2].equals(array[3])) {
				for (String option : optionArr) {
					if (option.equals(array[2])) flag1 = true;
					if (option.equals(array[1])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (!flag1 || !flag2) return new BigDecimal(-1);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("后三组三")) {// 
			// 规则    开奖号码中的3个数字有两个相同，即为组三。前三、中三、后三组三，0~9至少任选2个号进行复式投注，
			 //投注的号码与中奖号码相同（顺序不限），则为中奖。例如，前三组三选择号码4、5 ，将组合成2注（一对4一个5和一对5一个4），
			 //开奖结果为： 44512 、 45412 、 54412、45512、54512、55412 之一皆中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(optionArr.length*(optionArr.length-1)));
			boolean flag1 = false;
			boolean flag2 = false;
			if (array[2].equals(array[3])) {
				for (String option : optionArr) {
					if (option.equals(array[2])) flag1 = true;
					if (option.equals(array[4])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[2].equals(array[4])) {
				for (String option : optionArr) {
					if (option.equals(array[2])) flag1 = true;
					if (option.equals(array[3])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (array[3].equals(array[4])) {
				for (String option : optionArr) {
					if (option.equals(array[3])) flag1 = true;
					if (option.equals(array[2])) flag2 = true;
				}
				if (flag1 && flag2) return detail.getBetRate().multiply(oneBetMoney);
				else return new BigDecimal(-1);
			}
			if (!flag1 || !flag2) return new BigDecimal(-1);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("前三组六")) {// 
			// 规则   前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（顺序不限），即为中奖。如13579，前三组六买153或153或135都中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(getBetNum(optionArr.length, 3)));
			boolean flag1 = false;
			boolean flag2 = false;
			boolean flag3 = false;
			for (String option : optionArr) {
				if (array[0].equals(option)) flag1 = true;
				if (array[1].equals(option)) flag2 = true;
				if (array[2].equals(option)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) return detail.getBetRate().multiply(oneBetMoney);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("中三组六")) {// 
			// 规则   前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（顺序不限），即为中奖。如13579，前三组六买153或153或135都中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(getBetNum(optionArr.length, 3)));
			boolean flag1 = false;
			boolean flag2 = false;
			boolean flag3 = false;
			for (String option : optionArr) {
				if (array[1].equals(option)) flag1 = true;
				if (array[2].equals(option)) flag2 = true;
				if (array[3].equals(option)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) return detail.getBetRate().multiply(oneBetMoney);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("后三组六")) {// 
			// 规则   前三、中三、后三0~9任选3个号进行投注，当开奖结果与所选的号码相同时（顺序不限），即为中奖。如13579，前三组六买153或153或135都中奖。
			String optionTitle = detail.getOptionTitle();// 格式  0,1
			String[] optionArr = optionTitle.split(",");
			// 每注投的钱数=总投注金额/总投注数
			BigDecimal oneBetMoney = new BigDecimal(detail.getBetMoney()).
					divide(new BigDecimal(getBetNum(optionArr.length, 3)));
			boolean flag1 = false;
			boolean flag2 = false;
			boolean flag3 = false;
			for (String option : optionArr) {
				if (array[2].equals(option)) flag1 = true;
				if (array[3].equals(option)) flag2 = true;
				if (array[4].equals(option)) flag3 = true;
			}
			if (flag1 && flag2 && flag3) return detail.getBetRate().multiply(oneBetMoney);
			else return new BigDecimal(-1);
		} else if (detail.getPlayName().equals("前三")) {// 
			// 规则    豹子：如号码为24448，则中三为豹子，如号码为34666，则后三为豹子
			//顺子：开奖的三位数相连则为顺子，如号码为23487，则前三为顺子，如号码为28978则中三和后三都为顺子（注：0和1为相连的数，0和9不是相连的数）
			//对子：指相连的三位数有任意2位数相同（豹子除外）为对子。如号码为46686，则前三，中三，后三为对子，如号码为11261则前三为对子。
			//半顺：指三位数中有任意2位数相连为半顺（不包括顺子，对子，数字09不算相连之数），如号码为29387则前三（23相连）中三（89）相连 后三（87），为半顺。
			Integer first = Integer.valueOf(array[0]);
			Integer second = Integer.valueOf(array[1]);
			Integer third = Integer.valueOf(array[2]);
			String optionTitle = detail.getOptionTitle();
			// 中奖金额
			BigDecimal winMoney = detail.getBetRate().multiply(new BigDecimal(detail.getBetMoney()));
			if ("豹子".equals(optionTitle)) {
				if (array[0].equals(array[1]) && array[1].equals(array[2])) return winMoney;
				else return new BigDecimal(-1);
			} else if ("顺子".equals(optionTitle)) {
				boolean flag = checkShunZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("对子".equals(optionTitle)) {
				boolean flag = checkDuiZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("半顺".equals(optionTitle)) {
				boolean flag = checkBanShun(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("中三")) {// 
			// 规则    豹子：如号码为24448，则中三为豹子，如号码为34666，则后三为豹子
			//顺子：开奖的三位数相连则为顺子，如号码为23487，则前三为顺子，如号码为28978则中三和后三都为顺子（注：0和1为相连的数，0和9不是相连的数）
			//对子：指相连的三位数有任意2位数相同（豹子除外）为对子。如号码为46686，则前三，中三，后三为对子，如号码为11261则前三为对子。
			//半顺：指三位数中有任意2位数相连为半顺（不包括顺子，对子，数字09不算相连之数），如号码为29387则前三（23相连）中三（89）相连 后三（87），为半顺。
			Integer first = Integer.valueOf(array[1]);
			Integer second = Integer.valueOf(array[2]);
			Integer third = Integer.valueOf(array[3]);
			String optionTitle = detail.getOptionTitle();
			// 中奖金额
			BigDecimal winMoney = detail.getBetRate().multiply(new BigDecimal(detail.getBetMoney()));
			if ("豹子".equals(optionTitle)) {
				if (array[0].equals(array[1]) && array[1].equals(array[2])) return winMoney;
				else return new BigDecimal(-1);
			} else if ("顺子".equals(optionTitle)) {
				boolean flag = checkShunZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("对子".equals(optionTitle)) {
				boolean flag = checkDuiZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("半顺".equals(optionTitle)) {
				boolean flag = checkBanShun(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else {
				return new BigDecimal(-1);
			}
		} else if (detail.getPlayName().equals("后三")) {// 
			// 规则    豹子：如号码为24448，则中三为豹子，如号码为34666，则后三为豹子
			//顺子：开奖的三位数相连则为顺子，如号码为23487，则前三为顺子，如号码为28978则中三和后三都为顺子（注：0和1为相连的数，0和9不是相连的数）
			//对子：指相连的三位数有任意2位数相同（豹子除外）为对子。如号码为46686，则前三，中三，后三为对子，如号码为11261则前三为对子。
			//半顺：指三位数中有任意2位数相连为半顺（不包括顺子，对子，数字09不算相连之数），如号码为29387则前三（23相连）中三（89）相连 后三（87），为半顺。
			Integer first = Integer.valueOf(array[2]);
			Integer second = Integer.valueOf(array[3]);
			Integer third = Integer.valueOf(array[4]);
			String optionTitle = detail.getOptionTitle();
			// 中奖金额
			BigDecimal winMoney = detail.getBetRate().multiply(new BigDecimal(detail.getBetMoney()));
			if ("豹子".equals(optionTitle)) {
				if (array[0].equals(array[1]) && array[1].equals(array[2])) return winMoney;
				else return new BigDecimal(-1);
			} else if ("顺子".equals(optionTitle)) {
				boolean flag = checkShunZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("对子".equals(optionTitle)) {
				boolean flag = checkDuiZi(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else if ("半顺".equals(optionTitle)) {
				boolean flag = checkBanShun(first, second, third);
				if (flag) return winMoney;
				else return new BigDecimal(-1);
			} else {
				return new BigDecimal(-1);
			}
		} else {
			return new BigDecimal(-1);
		}	
	}
	
	/**
	 * 判断这3个数是否为顺子（213也算顺子）
	 * @param num1
	 * @param num2
	 * @param num3
	 * @return
	 */
	public static boolean checkShunZi(Integer num1, Integer num2, Integer num3) {
		if (Math.abs(num1 - num2) == 1) {
			if (Math.abs(num1 - num3) == 1 || Math.abs(num1 - num3) == 2) {
				if (Math.abs(num1 - num3) == 1) {
					if (Math.abs(num2 - num3) == 2) return true;
					else return false;
				} else {
					if (Math.abs(num2 - num3) == 1) return true;
					else return false;
				}
			} else {
				return false;
			}
		} else if (Math.abs(num1 - num2) == 2) {
			if (Math.abs(num1 - num3) == 1 && Math.abs(num2 - num3) == 1) return true;
			else return false;
		} else {
			return false;
		}
	}
	/**
	 * 判断这3个数是否为对子
	 * @param num1
	 * @param num2
	 * @param num3
	 * @return
	 */
	public static boolean checkDuiZi(Integer num1, Integer num2, Integer num3) {
		if (num1 == num2 && num1 != num3) return true;
		else if (num1 == num3 && num1 != num2) return true;
		else if (num2 == num3 && num2 != num1) return true;
		else return false;
	}
	
	/**
	 * 判断这3个数是否为半顺
	 * @param num1
	 * @param num2
	 * @param num3
	 * @return
	 */
	public static boolean checkBanShun(Integer num1, Integer num2, Integer num3) {
		if (Math.abs(num1 - num2) == 1 || Math.abs(num1 - num3) == 1
				|| Math.abs(num2 - num3) == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 获取m个号选n的组合
	 */
	public static int getBetNum(int m, int n) {
		int fenzi=1;
		int fenmu=1;
		for(int k=m;k>=(m-n+1);k--){
			fenzi=fenzi*k;
		}
		for(int k=1;k<=n;k++){
			fenmu=fenmu*k;
		}
		return fenzi/fenmu;
	}

	/**
	 *  两面盘 第1-8球的中奖结果
	 * secondNum可传0，主要是判断1-5龙虎用的这个
	 */
	public Map<String,Boolean> getSingleBallResult(String value){
		Map<String,Boolean> map=new HashMap<String,Boolean>();
		int sum=Integer.parseInt(value);
		if(sum%2==0){
			map.put("双", true);
		}else{
			map.put("单", true);
		}
		if(sum>=5){
			map.put("大", true);
		}else if(sum<=4){
			map.put("小", true);
		}
		return map;
	}
	
	
	
	/**
	 *  总和龙虎
	 * 
	 */
	public Map<String,Boolean> getResult(String[]  array){
		Map<String,Boolean> map=new HashMap<String,Boolean>();
		int sum=0;
		for(int i=0;i<array.length;i++){
			sum=sum+Integer.parseInt(array[i]);
		}
		if(sum%2==0){
			map.put("总和双", true);
		}else{
			map.put("总和单", true);
		}
		if(sum>=23){
			map.put("总和大", true);
		}else if(sum<=22){
			map.put("总和小", true);
		}

		if(Integer.parseInt(array[0])>Integer.parseInt(array[4])){
			map.put("龙", true);
		}else if(Integer.parseInt(array[0])<Integer.parseInt(array[4])){
			map.put("虎", true);
		} else{
			map.put("和", true);
		}
		return map;
	}
	
	public boolean saveOpenResult(FiveGaSession session,String openResult){
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
			session.setOpenResult(openResult);
			fiveDAO.updateObject(session, null);
			flag=true;
		}
		return flag;
		
	}
	
	public boolean saveAndOpenResult(FiveGaSession session,String openResult){
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
			session.setOpenResult(openResult);		
			boolean flag1 = openFiveSessionOpenResultMethod(session, session.getOpenResult());
			if(flag1){
				session.setOpenTime(DateTimeUtil.getJavaUtilDateNow());
				session.setOpenStatus(GameConstants.OPEN_STATUS_OPENED);
				fiveDAO.updateObject(session, null);
				log.info("___[open result success sessionNO["+session.getSessionNo()+"]]");
				flag=true;
			}else{
				log.info("___[open result fail sessionNO["+session.getSessionNo()+"], please check...]");
			}
		}
		return flag;
	}
	
	@Override
	public PaginationSupport findGaBetDetail(String hql, List<Object> para,
			int pageNum, int pageSize) {
		return fiveDAO.findGaBetDetail(hql, para, pageNum, pageSize);
	}

	@Override
	public List<FiveDTO> findGaBetDetailById(String hql, List<Object> para) {
		return fiveDAO.findGaBetDetailById(hql, para);
	}
	@Override
	public boolean saveRevokePrize(FiveGaSession session) {
		//删除FiveGaBet表的记录
		List<Object> para = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer();
		hql.append(" and sessionId = ? ");
		para.add(session.getSessionId());
		fiveDAO.deleteFiveGaBet(hql.toString(),para);

		boolean result = gaService.saveXyRevokePrize(session.getSessionId(), Constants.GAME_TYPE_XY_FC,session.getSessionNo());
		if(result){
			session.setOpenStatus(Constants.OPEN_STATUS_INIT);//未开奖
			gaService.saveObject(session, null);
		}
		return result;
	}
}
