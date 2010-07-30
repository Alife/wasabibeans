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

package de.wasabibeans.framework.server.init;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class Init extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private WasabiLogger logger = WasabiLogger.getLogger(this.getClass());

	public Init() throws NamingException {
		new Thread() {

			public void run() {
				System.out.println("Wasabi initialization started...");
				int sleepTime = 2000;
				while (true) {
					try {
						Thread.sleep(sleepTime);
						WasabiManager.initDatabase();
						WasabiManager.initRepository(WasabiConstants.JCR_NODETYPES_RESOURCE_PATH, true);
						logger.info("Wasabi initialization completed.");
						break;
					} catch (Exception e) {
						logger.error("Wasabi initialization failed:", e);
						break;
					}
				}
			}

		}.start();
	}
}
