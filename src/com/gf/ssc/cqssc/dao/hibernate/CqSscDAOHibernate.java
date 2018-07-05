package com.gf.ssc.cqssc.dao.hibernate;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;

import com.apps.Constants;
import com.framework.dao.hibernate.AbstractBaseDAOHibernate;
import com.framework.dao.hibernate.PaginationSupport;
import com.framework.util.DateTimeUtil;
import com.game.model.dto.WinCoDTO;
import com.gf.pick11.gdpick11.model.GfGdPick11GaOmit;
import com.gf.ssc.cqssc.CqSscConstants;
import com.gf.ssc.cqssc.dao.ICqSscDAO;
import com.gf.ssc.cqssc.model.GfCqSscGaOmit;
import com.gf.ssc.cqssc.model.GfCqSscGaSession;
import com.gf.ssc.cqssc.model.GfCqSscGaTrend;

public class CqSscDAOHibernate extends AbstractBaseDAOHibernate 
implements ICqSscDAO {

	@Override
	public GfCqSscGaSession getCurrentSession() {
		Date now = DateTimeUtil.getJavaUtilDateNow();//当前系统时间
		String todayYyyymmdd = DateTimeUtil.DateToString(now);
		Date todayStart = DateTimeUtil.parse(todayYyyymmdd+" 01:55:00");
		Date todayEnd = DateTimeUtil.parse(todayYyyymmdd+" 09:50:00");
		
		Date todayStart1 = DateTimeUtil.parse(todayYyyymmdd+" 21:50:00");
		Date todayEnd1 = DateTimeUtil.parse(todayYyyymmdd+" 21:55:00");
		
		if(now.compareTo(todayStart)>=0&&now.compareTo(todayEnd)==-1){
			now=DateTimeUtil.parse(todayYyyymmdd+" 09:51:00");
		}else if(now.compareTo(todayStart1)>=0&&now.compareTo(todayEnd1)==-1){
			now=DateTimeUtil.parse(todayYyyymmdd+" 21:56:00");
		}
		
		
		List<GfCqSscGaSession> list = getSession().createQuery("from GfCqSscGaSession gks where gks.startTime<=? and gks.endTime>? and (gks.openStatus=? or gks.openStatus=?) order by gks.sessionId")
		.setTimestamp(0, now)
		.setTimestamp(1, now)
		.setString(2, Constants.OPEN_STATUS_INIT)
		.setString(3, Constants.OPEN_STATUS_OPENING)
		.setMaxResults(1)
		.list();
		if(list!=null && list.size()>0){
			return (GfCqSscGaSession)list.get(0);
		}
		return null;
	}


	@Override
	public GfCqSscGaSession getPreviousSessionBySessionNo(String sessionNo) {
		String  hql=" from GfCqSscGaSession bj3 where bj3.sessionNo=? ";
		Query record =getSession().createQuery(hql);
		record.setString(0, sessionNo);
		List list=record.list();
		if(list!=null && list.size()>0){
			return (GfCqSscGaSession)list.get(0);
		}
		return null;
	}

	@Override
	public List<GfCqSscGaTrend> findGaTrendList() {
		String hql = " from GfCqSscGaTrend trend order by trend.trendId desc";
		List<GfCqSscGaTrend> list;
		list = getSession().createQuery(hql).list();
		return list;
	}

	@Override
	public PaginationSupport findGaSessionList(String hqsl,List<Object> para,int pageNum,int pageSize) {
		Query query = makeQuerySQL( " from GfCqSscGaSession ho where 1=1 "+hqsl, para);
		query.setFirstResult(pageNum);
		query.setMaxResults(pageSize);
		List queList = query.list();
		Query count = makeQuerySQL("select count(ho.sessionId) from GfCqSscGaSession ho where 1=1 "+hqsl, para);
		Integer totalCount = (Integer) count.uniqueResult();
		return new PaginationSupport(queList, totalCount.intValue());
	}
	
	public Query makeQuerySQL(String hsql, List param) {
		Query q = getSession().createQuery(hsql);
		Iterator<Object> it = param.iterator();
		int i = 0;
		Object obj;
		while (it.hasNext()) {
			obj = it.next();
			if (obj instanceof String) {
				q.setString(i, (String) obj);
			} else if (obj instanceof Date) {
				q.setTimestamp(i, (Date) obj);
			} else if (obj instanceof Integer) {
				q.setInteger(i, ((Integer) obj).intValue());
			} else if (obj instanceof Integer[]) {
				q.setParameterList("ids", (Integer[]) obj);
			}
			i++;
		}
		return q;
	}

//	@Override
//	public List<GfCqSscGaSession> getCurrentSession(int start, int end) {
//		SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd"); //时间格式为年月日
//		SimpleDateFormat hmsFormat = new SimpleDateFormat("HH:mm:ss"); // 时间时分秒格式
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Long nowStamp = null; // 现在时分秒的时间戳
//		Long sStamp = null; // 夜场结束时分秒的时间戳
//		
//		Long eStamp = null; // 正常开始时分秒的时间戳
//		String nowS; // 当前时间String类型
//		Date now = null; //当前时间Date类型
//		Date now2 = null;//新增
//		Date now3 = null;//新增
//		Date now4 = null; // 新增
//		
//		try {
//			nowStamp = hmsFormat.parse(hmsFormat.format(new Date())).getTime();
//			sStamp = hmsFormat.parse(GameConstants.BJ3_EVENING_END_TIME).getTime();
//			eStamp = hmsFormat.parse(GameConstants.BJ3_NORMAL_START_TIME).getTime();
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		nowS = sdf.format(new Date()); // 当前系统时间
//		ParsePosition pos = new ParsePosition(0);
//		now= sdf.parse(nowS, pos);
//		
//		// 如果当前时间是6点到9点时间，把当前时间设为9点，这样就能查到下一期彩票。
//		if(sStamp < nowStamp && nowStamp < eStamp){
//			try {
//				now = sdf.parse(ymdFormat.format(new Date()) + GameConstants.BJ3_NORMAL_START_TIME);
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		now3 = new Date( now.getTime() + end* 60 *1000 *GameConstants.BJ3_TIME_INTERVAL);
//		now2 = new Date( now.getTime() - start * 60 *1000*GameConstants.BJ3_TIME_INTERVAL);
//		try {
//			now4 = sdf.parse(ymdFormat.format(new Date()) + GameConstants.BJ3_NORMAL_START_TIME);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if(now2.getTime() < now4.getTime()){
//			now2 = new Date( now2.getTime()-GameConstants.BJ3_PAUSE_PART *60*60 *1000);
//		}
//		
//		List<GfCqSscGaSession> list = getSession().createQuery("from GfCqSscGaSession bj3s where bj3s.startTime>? and "
//				+ "bj3s.endTime <= ? order by bj3s.sessionId")
//				.setTimestamp(0, now2)
//				.setTimestamp(1, now3)
////				.setMaxResults(1)
//				.list();
//		return list;
//	}

//	@Override
//	public PaginationSupport findFcGaBetList(String hql, List<Object> para,
//			int pageNum, int pageSize) {
//		Query query = makeQuerySQL( " select new com.bj3.model.dto.FcBetDTO(se.sessionNo,ho.totalPoint,ho.winCash,se.startTime,se.endTime,ho.totalPoint-ho.winCash) from FcGaBet ho,GfCqSscGaSession se where 1=1 "
//				+ " and se.sessionId = ho.sessionId "+hql+" group by ho.sessionId order by ho.sessionId desc ", para);
//		query.setFirstResult(pageNum);
//		query.setMaxResults(pageSize);
//		List queList = query.list();
//		Query count = makeQuerySQL("select count(ho.betId) from FcGaBet ho, GfCqSscGaSession se where 1=1" +
//				" and  se.sessionId = ho.sessionId "+hql, para);
//		Integer totalCount = (Integer) count.uniqueResult();
//		return new PaginationSupport(queList, totalCount.intValue());
//	}

//	@Override
//	public PaginationSupport findGaBetDetail(String hql, List<Object> para,
//			int pageNum, int pageSize) {
//		Query query = makeQuerySQL( " select new com.bj3.model.dto.FcDTO(ga,u) from GaBetDetail ga,User u where 1=1 "
//				+ " and ga.userId = u.userId "+hql+" order by ga.betDetailId desc ", para);
//		query.setFirstResult(pageNum);
//		query.setMaxResults(pageSize);
//		List queList = query.list();
//		Query count = makeQuerySQL("select count(ga.betDetailId) from GaBetDetail ga, User u where 1=1" +
//				" and  ga.userId = u.userId "+hql, para);
//		Integer totalCount = (Integer) count.uniqueResult();
//		return new PaginationSupport(queList, totalCount.intValue());
//	}

//	@Override
//	public List<FcDTO> findGaBetDetailById(String hql, List<Object> para) {
//	    Query query = getSession().createQuery(" select new com.bj3.model.dto.FcDTO(ga,u) from GaBetDetail ga,User u where 1=1 and ga.userId = u.userId "
//	    		+ hql)
//	    		.setParameter(0, para.get(0));
//	    List<FcDTO> list = query.list();
//		return list;
//	}

	@Override
	public List<WinCoDTO> findGaWinCountList() {
	    Query query = getSession().createQuery(" select new com.gf.ssc.cqssc.model.dto.WinCoDTO(co,u) from GaWinCount co,User u where 1=1 and co.userId = u.userId order by co.totalMoney desc")
	    		.setMaxResults(10);
	    List<WinCoDTO> list = query.list();
		return list;
	}
	
	public GfCqSscGaOmit getFcGaOmitBySessionNo(String sessionNo){
		String hql = " from GfCqSscGaOmit ho where ho.sessionNo=? ";
		List<GfCqSscGaOmit> list;
		list = getSession().createQuery(hql).setString(0, sessionNo).list();
		if(list!=null&&list.size()>0){//
			return list.get(0);
		}else{
			return null;
		}
		
	}

	@Override
	public List<GfCqSscGaOmit> findGfCqSscGaOmitList(String hqls, List<Object> para,
			int num) {
		String  hql=" from GfCqSscGaOmit ho where 1=1  ";
		Query query =makeQuerySQL(hql+hqls,para);
		query.setMaxResults(num);
		List<GfCqSscGaOmit>list=query.list();
		return list;
	}
	
	@Override
	public PaginationSupport findGameBetAndWinList(String hqls,
			List<Object> para, int startIndex, int pageSize) {
		Query query = makeQuerySQL( " select new com.apps.model.dto.AgentDTO(se.sessionNo,se.startTime,se.endTime,sum(ho.money),sum(ho.winCash),sum(ho.money)-sum(ho.winCash)) from GaBetSponsor ho,GfCqSscGaSession se where 1=1 "
				+ " and ho.gameType ="+ Constants.GAME_TYPE_GF_CQSSC
				+ " and se.sessionId = ho.sessionId "+hqls+" group by ho.sessionId order by ho.sessionId desc ", para);
		query.setFirstResult(startIndex);
		query.setMaxResults(pageSize);
		List queList = query.list();
		Query count = makeQuerySQL("select count(ho.sessionId) from GaBetSponsor ho,GfCqSscGaSession se where 1=1" 
				+ " and ho.gameType ="+ Constants.GAME_TYPE_GF_CQSSC
				+" and  se.sessionId = ho.sessionId "+hqls+" group by ho.sessionId", para);
		Integer totalCount = (Integer) count.list().size();
		return new PaginationSupport(queList, totalCount.intValue());
	}



	public PaginationSupport findGfCqSscGaSessionList(String hql,
			List<Object> para, int startIndex, int pageSize) {
		Query query = makeQuerySQL( " from GfCqSscGaSession ho where 1=1 "+hql,para);
		query.setFirstResult(startIndex);
		query.setMaxResults(pageSize);
		List queList = query.list();
		Query count = makeQuerySQL("select count(ho.sessionId) from GfCqSscGaSession ho where 1=1 "+hql, para);
		Integer totalCount = (Integer) count.uniqueResult();
		return new PaginationSupport(queList, totalCount.intValue());
	}

}