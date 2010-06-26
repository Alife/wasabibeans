package de.wasabibeans.framework.server.core.util;

import org.jboss.logging.Logger;

public class WasabiLogger {
	
	private Logger logger;
		
	public static WasabiLogger getLogger(Class<?> clazz) {
		return new WasabiLogger(Logger.getLogger(clazz));
	}
	
	public WasabiLogger(Logger logger) {
		this.logger = logger;
	}
	
	public void debug(String msg) {
		logger.debug(msg);
	}
	
	public void info(String msg) {
		logger.info(msg);
	}
	
	public void error(String msg) {
		logger.error(msg);
	}
	
	public void warn(String msg) {
		logger.warn(msg);
	}
	
	public void debug(String msg, Throwable t) {
		logger.debug(msg);
	}
	
	public void info(String msg, Throwable t) {
		logger.info(msg);
	}
	
	public void error(String msg, Throwable t) {
		logger.error(msg);
		t.printStackTrace();
	}
	
	public void warn(String msg, Throwable t) {
		logger.warn(msg);
	}
}
