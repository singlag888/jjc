package com.gf.ssc.jxssc.service.impl;

import java.util.Random;

import com.framework.util.StringUtil;

public class JxSscOpenResult {
	public final static String[] SOURCE = {
		"0","1","2","3","4","5","6","7","8","9"
	};
	
	/**
	 * 产生一个随机的结果
	 * @return
	 */
	public String getRandomResult(){
		Random random=new Random();
		String number1=SOURCE[random.nextInt(10)];
		String number2=SOURCE[random.nextInt(10)];
		String number3=SOURCE[random.nextInt(10)];
		String number4=SOURCE[random.nextInt(10)];
		String number5=SOURCE[random.nextInt(10)];
		return number1+","+number2+","+number3+","+number4+","+number5;
	}
	
	
	
	
	
	public static void main(String[] args){
		JxSscOpenResult bj3=new JxSscOpenResult();
		for(int i=0;i<50;i++){
			System.out.println(bj3.getRandomResult());
		}
	}
}
