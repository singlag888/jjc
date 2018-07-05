package com.apps.dao;

import java.util.List;

import com.apps.model.Address;
import com.framework.dao.IDAO;
import com.framework.dao.hibernate.PaginationSupport;

/**
 * 优惠活动dao
 * 
 */
public interface IActivityDAO extends IDAO {
	public PaginationSupport findList(String hqls, List<Object> para,
			int startIndex, int pageSize);

	/**
	 * 改变排序
	 * 
	 * @param id
	 * @param flag
	 *            1.升序+1 0.降序-1
	 */
	public void updateSort(Integer id, String flag);

}
