package com.xy.ssc.tjssc.model.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.game.model.GaBetDetail;
import com.ram.model.User;

public class TjSscDTO {
	private String sessionNo;
	private BigDecimal totalPoint;
	private BigDecimal winCash;
	private BigDecimal payoff;
	private Date startTime;
	private Date endTime;
	private User user;
	private GaBetDetail gaBetDetail;
	public TjSscDTO(){
		
	}
	public TjSscDTO(GaBetDetail gaBetDetail, User user){
		this.setGaBetDetail(gaBetDetail);
		this.setUser(user);
	}
	
	public TjSscDTO(BigDecimal totalPoint,
			BigDecimal winCash,BigDecimal payoff){
		this.totalPoint = totalPoint;
		this.winCash = winCash;
		this.payoff = payoff;
	}
	
	public TjSscDTO(String sessionNo,BigDecimal totalPoint,
			BigDecimal winCash,BigDecimal payoff){
		this.sessionNo = sessionNo;
		this.totalPoint = totalPoint;
		this.winCash = winCash;
		this.payoff = payoff;
	}
	public TjSscDTO(String sessionNo,BigDecimal totalPoint,
			BigDecimal winCash,Date startTime,Date endTime,BigDecimal payoff){
		this.sessionNo = sessionNo;
		this.totalPoint = totalPoint;
		this.winCash = winCash;
		this.startTime = startTime;
		this.endTime = endTime;
		this.payoff = payoff;
	}

	public String getSessionNo() {
		return sessionNo;
	}

	public void setSessionNo(String sessionNo) {
		this.sessionNo = sessionNo;
	}

	public BigDecimal getTotalPoint() {
		return totalPoint;
	}

	public void setTotalPoint(BigDecimal totalPoint) {
		this.totalPoint = totalPoint;
	}

	public BigDecimal getWinCash() {
		return winCash;
	}

	public void setWinCash(BigDecimal winCash) {
		this.winCash = winCash;
	}

	public BigDecimal getPayoff() {
		return payoff;
	}

	public void setPayoff(BigDecimal payoff) {
		this.payoff = payoff;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	/**
	 * @return the gaBetDetail
	 */
	public GaBetDetail getGaBetDetail() {
		return gaBetDetail;
	}
	/**
	 * @param gaBetDetail the gaBetDetail to set
	 */
	public void setGaBetDetail(GaBetDetail gaBetDetail) {
		this.gaBetDetail = gaBetDetail;
	}
	
}
