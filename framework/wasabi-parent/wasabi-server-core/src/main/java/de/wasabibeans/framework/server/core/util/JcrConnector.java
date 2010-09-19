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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.jca.JCASessionHandle;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.locking.Locker;

public class JcrConnector {

	private static WasabiLogger logger = WasabiLogger.getLogger(JcrConnector.class);

	private JndiConnector jndi;

	private Repository jcrRepository;
	private Session session;

	public static JcrConnector getJCRConnector(JndiConnector jndi) {
		return new JcrConnector(jndi);
	}

	public JcrConnector(JndiConnector jndi) {
		this.jndi = jndi;
	}

	public Repository getJCRRepository() throws UnexpectedInternalProblemException {
		// if (this.jcrRepository == null) {
		this.jcrRepository = (Repository) jndi.lookupLocal(WasabiConstants.JNDI_JCR_DATASOURCE);
		// }
		return this.jcrRepository;
	}

	public Session getJCRSession() throws UnexpectedInternalProblemException {
		try {
			if (session == null) {
				session = getJCRRepository().login(
						new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
			}
			//logger.info("Session used: " + ((JCASessionHandle) session).getXAResource().toString());
			return session;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public void destroy() {
		try {
			if (session != null) {
				JCASessionHandle handle = (JCASessionHandle) session;
				// sending an error event forces the JCA adapter to get rid of the corrupt JCR session
				// (calling handle.getManagedConnection().destroy() does not work, as destroy() reduces the size of the
				// JCA pool, which would eventually lead to no JCR session being available at all)
				handle.getManagedConnection().sendrrorEvent(handle,
						new Exception("This is just a workaround to close a corrupt JCR session - IGNORE."));
				session = null;
			}
		} catch (Exception e) {
			logger
					.error(
							"Fatal internal error: A JCR session is corrupt and could not be dealt with accordingly. Restart the JCR repository to avoid possible consequential errors.",
							e);
		}
	}

	public void logout() {
		try {
			if (session != null) {
				Locker.cleanUpLockTokens(session);
				session.logout();
				session = null;
			}
		} catch (Exception e) {
			logger
					.error(
							"Fatal internal error: A JCR session could not be returned to the connection pool properly. Restart the JCR repository to avoid possible consequential errors.",
							e);
		}
	}
}
