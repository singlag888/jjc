package com.game.model;
import java.util.Date;
import java.math.BigDecimal;

   /**
    * GaK3BetDetail 实体类
    * author:	ch
    * createDate:	2016-03-11 13:33:14
    */ 


public class GaK3BetDetail{
	private Integer betDetailId;
	private Integer userId;
	private Integer betId;
	private Integer betOptionId;
	private Integer sessionId;
	private Date betTime;
	private String betType;
	private String betContent;
	private Integer betPoint;
	private Integer betNum;
	private String winResult;
	private BigDecimal winCash;
	private String betFlag;
	private BigDecimal betRate;
	public void setBetDetailId(Integer betDetailId){
	this.betDetailId=betDetailId;
	}
	public Integer getBetDetailId(){
		return betDetailId;
	}
	public void setUserId(Integer userId){
	this.userId=userId;
	}
	public Integer getUserId(){
		return userId;
	}
	public void setBetId(Integer betId){
	this.betId=betId;
	}
	public Integer getBetId(){
		return betId;
	}
	public void setBetOptionId(Integer betOptionId){
	this.betOptionId=betOptionId;
	}
	public Integer getBetOptionId(){
		return betOptionId;
	}
	public void setSessionId(Integer sessionId){
	this.sessionId=sessionId;
	}
	public Integer getSessionId(){
		return sessionId;
	}
	public void setBetTime(Date betTime){
	this.betTime=betTime;
	}
	public Date getBetTime(){
		return betTime;
	}
	public void setBetType(String betType){
	this.betType=betType;
	}
	public String getBetType(){
		return betType;
	}
	public void setBetContent(String betContent){
	this.betContent=betContent;
	}
	public String getBetContent(){
		return betContent;
	}
	public void setBetPoint(Integer betPoint){
	this.betPoint=betPoint;
	}
	public Integer getBetPoint(){
		return betPoint;
	}
	public void setBetNum(Integer betNum){
	this.betNum=betNum;
	}
	public Integer getBetNum(){
		return betNum;
	}
	public void setWinResult(String winResult){
	this.winResult=winResult;
	}
	public String getWinResult(){
		return winResult;
	}
	public void setWinCash(BigDecimal winCash){
	this.winCash=winCash;
	}
	public BigDecimal getWinCash(){
		return winCash;
	}
	public void setBetFlag(String betFlag){
	this.betFlag=betFlag;
	}
	public String getBetFlag(){
		return betFlag;
	}
	public void setBetRate(BigDecimal betRate){
	this.betRate=betRate;
	}
	public BigDecimal getBetRate(){
		return betRate;
	}
}
