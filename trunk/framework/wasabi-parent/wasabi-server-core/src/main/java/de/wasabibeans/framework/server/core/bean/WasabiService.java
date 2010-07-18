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

package de.wasabibeans.framework.server.core.bean;

import javax.jcr.Session;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public abstract class WasabiService {

	protected WasabiLogger logger;

	protected JcrConnector jcr;

	public WasabiService() {
		this.logger = WasabiLogger.getLogger(this.getClass());
		this.jcr = JcrConnector.getJCRConnector();
	}

	protected Session getJCRSession() throws UnexpectedInternalProblemException {
		Session s = null;
		// String username = ctx.getCallerPrincipal().getName();
		// s = jcr.getJCRSession(username);
		//
		// if (s != null) {
		// // The session might have been closed because it was bound to a transaction.
		// // Due to a yet unknown reason 'isLive()' does not return false in that case but throws an
		// // IllegalArgumentException.
		// try {
		// if (!s.isLive()) {
		// s = null;
		// }
		// } catch (IllegalStateException ise) {
		// s = null;
		// }
		// }
		//
		// if (s == null) {
		// s = jcr.getJCRSession();
		// jcr.storeJCRSession(username, s);
		// }

		// System.out.println(s.toString());
		s = jcr.getJCRSession();
		return s;
	}

	protected void cleanJCRSession(Session s) {
		s.logout();
	}
}
