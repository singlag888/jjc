package com.gf.pick11.shpick11.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apps.eff.dto.SessionItem;
import com.framework.dao.hibernate.PaginationSupport;
import com.framework.service.IService;
import com.game.model.GaBetOption;
import com.gf.pick11.gdpick11.model.GfGdPick11GaSession;
import com.gf.pick11.shpick11.model.GfShPick11GaOmit;
import com.gf.pick11.shpick11.model.GfShPick11GaSession;
import com.gf.pick11.shpick11.model.GfShPick11GaTrend;
import com.gf.pick11.shpick11.model.dto.GfShPick11DTO;
import com.ram.model.User;

public interface IShPick11Service extends IService {

	/**
	 * 更新场次
	 * @return
	 */
	public String updateInitSession(String initDay);
	
	/**
	 * 开奖
	 * @return
	 */
	public List<GfShPick11GaSession> updateAndOpenResult(Map<String, SessionItem> sessionNoMap);
	
	/**
	 * 获取当前场次，根据系统时间从数据库查询
	 * @return
	 */
	public GfShPick11GaSession getCurrentSession();
	/**
	 * 根据期号获取当前期
	 * @param sessionNo
	 * @return
	 */
	public GfShPick11GaSession getPreviousSessionBySessionNo(String sessionNo);
	/**
	 * 保存用户投注信息
	 */
	public void saveUserBetInfo(String room,Map<Integer,Integer> betMap,List<GaBetOption> list,GfShPick11GaSession gaPick11Session,User user,BigDecimal betAll);
	/**
	 * 冷热排行列表
	 * @return
	 */
	public List<GfShPick11GaTrend> findGfShPick11GaTrendList();
	/**
	 * 开奖列表
	 */
	public PaginationSupport  findGfShPick11GaSessionList(String hql, List<Object> para,int pageNum,int pageSize);
	/**
	 * 更新冷热排行榜
	 */
	public void updateGaTrend();
	
	/**
	 * 保存单期开奖结果
	 */
	public boolean saveOpenResult(GfShPick11GaSession session,String openResult);
	
	/**
	 * 保存单期开奖结果
	 */
	public boolean saveAndOpenResult(GfShPick11GaSession session,String openResult);
	/**
	 * 统计每期开奖盈亏
	 */
	public PaginationSupport  findShPick11GaBetList(String hql, List<Object> para,int pageNum,int pageSize);
	/**
	 * 获取投注详细信息
	 * @param hql
	 * @param para
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	public PaginationSupport findGaBetDetail(String hql,
			List<Object> para, int pageNum, int pageSize);
	
	/**
	 * 根据id查询投注详情
	 * @param string
	 * @param para
	 * @return
	 */
	public List<GfShPick11DTO> findGaBetDetailById(String string, List<Object> para);
	/**
	 * 保存合买投注
	 * @param u 用户
	 * @param list 投注号码列表
	 * @param seMap 期号对应倍数
	 * @param num 总共分的份数
	 * @param buyNum 购买份数
	 * @param isGuaranteed 是否保底
	 * @param guNum 保底份数
	 *  @param betMon  本次自己需要花费金额(包括认购和保底)
	 *  @param needMoney  方案总金额
	 *  @param betNum 总注数
	 *  @param currentSession 本期期号
	 *  @param buyMoney  认购份额花费金额
	 * @return
	 */
	public User saveSponsorBet(User u,List<String> list,Map<String,Integer>seMap,int num,int buyNum,String isGuaranteed,int gaNum,BigDecimal betMon,
			BigDecimal needMoney,int betNum,GfShPick11GaSession currentSession,BigDecimal buyMoney,String isPrivacy);
	/**
	 * 保存代购投注
	 * @param user
	 * @param list
	 * @param seMap 期号对应倍数 key期号 value倍数
	 * @param isAddNo 是否追号
	 * @param isWinStop 是否中奖后停止
	 * @return
	 */
	public User saveProcurementServiceBet(User user, List<String> list,
			Map<String, Integer> seMap, String isAddNo, String isWinStop,BigDecimal betMon,int betNum,GfShPick11GaSession currentSession);
	/**
	 * 更新开奖结果
	 */
	public String updateShPick11SessionOpenResultMethod(
			GfShPick11GaSession session, String openResult,String orderNum);
	/**
	 * 根据日期查找日期中的第一期
	 * @param string
	 * @return
	 */
	public GfShPick11GaSession getFirstSessionByDate(String string);
	/**
	 * 根据日期查找日期中的最后期
	 * @param string
	 * @return
	 */
	public GfShPick11GaSession getEndSessionByDate(String string);
	/**
	 * 查询遗漏表数据  num是最大查询数量
	 */
	public List<GfShPick11GaOmit> findGfShPick11GaOmitList(String string,
			List<Object> para, int num);
	
	public void updateFetchAndOpenOmit(GfShPick11GaSession session);
	
	/**
	 * 计算合买
	 */
	public void updateCountJointBet(String sessionNo);
	/**
	 * 统计每日盈亏、每期盈亏。
	 */
	public void updateDayBetCount(GfShPick11GaSession session);
	
	/**
	 * 派彩错误，撤回已经派彩的金额
	 * @param session
	 * @return
	 */
	public boolean saveRevokePrize(GfShPick11GaSession session);
}
