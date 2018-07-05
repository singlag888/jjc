package com.game.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.framework.util.DateTimeUtil;
import com.game.service.IGaK3Service;

/**
 * 初始化场次
 */
public class InitK3Session extends QuartzJobBean {

	private static IGaK3Service gaK3Service;
	protected final Log log = LogFactory.getLog(getClass());
	
	public static IGaK3Service getGaK3Service() {
		return gaK3Service;
	}

	@SuppressWarnings("static-access")
	public void setGaK3Service(IGaK3Service gaK3Service) {
		this.gaK3Service = gaK3Service;
	}
	
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		try {
			log.info("_______[k3 init session]["+DateTimeUtil.getDateTime()+"]___________________");
			
			gaK3Service.updateInitSession();
			
			log.info("_______[k3 init session end]["+DateTimeUtil.getDateTime()+"]___________________");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}