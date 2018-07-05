package com.gf.three.model.dto;

import com.game.model.GaBetSponsor;
import com.ram.model.User;

/**
 * 北京五分彩DTO
 * @author hpz
 */
public class GfThreeDTO {

	private GaBetSponsor sp = new GaBetSponsor();
	private User u = new User();
	
	public GfThreeDTO(){
		
	}
	public GfThreeDTO(GaBetSponsor sp,User u){
		this.sp = sp;
		this.u = u;
	}
	
	public GaBetSponsor getSp() {
		return sp;
	}

	public void setSp(GaBetSponsor sp) {
		this.sp = sp;
	}

	public User getU() {
		return u;
	}

	public void setU(User u) {
		this.u = u;
	}
	
}
