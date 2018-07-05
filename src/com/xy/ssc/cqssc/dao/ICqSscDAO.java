package com.xy.ssc.cqssc.dao;

import java.util.List;

import com.framework.dao.IDAO;
import com.framework.dao.hibernate.PaginationSupport;
import com.xy.ssc.cqssc.model.CqSscGaSession;
import com.xy.ssc.cqssc.model.CqSscGaTrend;
import com.xy.ssc.cqssc.model.dto.CqSscDTO;


public interface ICqSscDAO  extends IDAO {
	/**
	 * 获取当前场次，根据系统时间从数据库查询
	 * @return
	 */
	public CqSscGaSession getCurrentSession();
	/**
	 * 根据期号获取当前期
	 * @param sessionNo
	 * @return
	 */
	public CqSscGaSession getPreviousSessionBySessionNo(String sessionNo);
	/**
	 * 冷热排行列表
	 * @return
	 */
	public List<CqSscGaTrend> findCqSscGaTrendList();
	/**
	 * 冷热排行所有数据
	 * @return
	 */
	public List<CqSscGaTrend> findCqSscGaTrendAllList();
	/**
	 * 开奖列表
	 */
	public PaginationSupport  findCqSscGaSessionList(String hql, List<Object> para,int pageNum,int pageSize);
	/**
	 * 统计每期数据
	 * @return
	 */
	public PaginationSupport  findCqSscGaBetList(String hql, List<Object> para,int pageNum,int pageSize);

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
	public List<CqSscDTO> findGaBetDetailById(String hql, List<Object> para);
	
	/**
	 * 删除CqSscGaBet表数据
	 * @param hql
	 * @param para
	 */
	public void deleteCqSscGaBet(String hql, List<Object> para);

}
