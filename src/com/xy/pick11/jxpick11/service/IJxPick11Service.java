package com.xy.pick11.jxpick11.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.apps.eff.dto.SessionItem;
import com.framework.dao.hibernate.PaginationSupport;
import com.framework.service.IService;
import com.game.model.GaBetOption;
import com.ram.model.User;
import com.xy.pick11.jxpick11.model.JxPick11GaSession;
import com.xy.pick11.jxpick11.model.JxPick11GaTrend;
import com.xy.pick11.jxpick11.model.dto.JxPick11DTO;

public interface IJxPick11Service extends IService {

	/**
	 * 更新场次
	 * @return
	 */
	public String updateInitSession();
	/**
	 * 开奖
	 * @return
	 */
	public String updateFetchAndOpenResult(Map<String, SessionItem> sessionNoMap);
	
	/**
	 * 获取当前场次，根据系统时间从数据库查询
	 * @return
	 */
	public JxPick11GaSession getCurrentSession();
	/**
	 * 根据期号获取当前期
	 * @param sessionNo
	 * @return
	 */
	public JxPick11GaSession getPreviousSessionBySessionNo(String sessionNo);
	/**
	 * 保存用户投注信息
	 */
	public void saveUserBetInfo(String room,Map<String,Integer> betMap,List<GaBetOption> list,JxPick11GaSession gaPick11Session,User user,BigDecimal betAll);
	/**
	 * 冷热排行列表
	 * @return
	 */
	public List<JxPick11GaTrend> findJxPick11GaTrendList();
	/**
	 * 开奖列表
	 */
	public PaginationSupport  findJxPick11GaSessionList(String hql, List<Object> para,int pageNum,int pageSize);
	/**
	 * 更新冷热排行榜
	 */
//	public void updateGaTrend();
	
	/**
	 * 初始化明天的场次数据。
	 */
	public String updateTomorrowSession();
	/**
	 * 保存单期开奖结果
	 */
	public boolean saveOpenResult(JxPick11GaSession session,String openResult);
	
	/**
	 * 保存单期开奖结果
	 */
	public boolean saveAndOpenResult(JxPick11GaSession session,String openResult);
	/**
	 * 统计每期开奖盈亏
	 */
	public PaginationSupport  findJxPick11GaBetList(String hql, List<Object> para,int pageNum,int pageSize);
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
	public List<JxPick11DTO> findGaBetDetailById(String string, List<Object> para);
	/**
	 * 派彩错误，撤回已经派彩的金额
	 * @param session
	 * @return
	 */
	public boolean saveRevokePrize(JxPick11GaSession session);

}
