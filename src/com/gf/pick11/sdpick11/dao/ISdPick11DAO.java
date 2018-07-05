package com.gf.pick11.sdpick11.dao;

import java.util.List;

import com.framework.dao.IDAO;
import com.framework.dao.hibernate.PaginationSupport;
import com.gf.pick11.sdpick11.model.GfSdPick11GaOmit;
import com.gf.pick11.sdpick11.model.GfSdPick11GaSession;
import com.gf.pick11.sdpick11.model.GfSdPick11GaTrend;
import com.gf.pick11.sdpick11.model.dto.GfSdPick11DTO;

public interface ISdPick11DAO extends IDAO {

	/**
	 * 获取当前场次，根据系统时间从数据库查询
	 * @return
	 */
	public GfSdPick11GaSession getCurrentSession();
	/**
	 * 根据期号获取当前期
	 * @param sessionNo
	 * @return
	 */
	public GfSdPick11GaSession getPreviousSessionBySessionNo(String sessionNo);
	/**
	 * 冷热排行列表
	 * @return
	 */
	public List<GfSdPick11GaTrend> findGfSdPick11GaTrendList();
	/**
	 * 开奖列表
	 */
	public PaginationSupport  findGfSdPick11GaSessionList(String hql, List<Object> para,int pageNum,int pageSize);
	/**
	 * 统计每期开奖盈亏
	 */
	public PaginationSupport  findSdPick11GaBetList(String hql, List<Object> para,int pageNum,int pageSize);
	
	/**
	 * 投注详细信息
	 * @param hql
	 * @param para
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	public PaginationSupport findGaBetDetail(String hql, List<Object> para,
			int pageNum, int pageSize);
	
	/**
	 * 根据id查询投注详情
	 * @param hql
	 * @param para
	 * @return
	 */
	public List<GfSdPick11DTO> findGaBetDetailById(String hql, List<Object> para);
	
	/**
	 * 查询遗漏
	 */
	public GfSdPick11GaOmit getSdPick11OmitBySessionNo(String sessionNo);
	/**
	 * 根据日期查找日期中的第一期
	 */
	public GfSdPick11GaSession getFirstSessionByDate(String date);
	/**
	 * 根据日期查找日期中的最后一期
	 */
	public GfSdPick11GaSession getEndSessionByDate(String date);
	/**
	 * 查询遗漏表数据  num是最大查询数量
	 */
	public List<GfSdPick11GaOmit> findGfSdPick11GaOmitList(String string,
			List<Object> para, int num);
}
