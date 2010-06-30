/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 *  Further information are online available at: http://www.wasabibeans.de
 */

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
