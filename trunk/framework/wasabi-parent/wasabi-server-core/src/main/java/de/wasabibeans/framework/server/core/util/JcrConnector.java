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

import java.lang.reflect.Method;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
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

	/**
	 * Returns a JCA handle that is connected to a JCR session.
	 * 
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public Session getJCRSession() throws UnexpectedInternalProblemException {
		try {
			if (session == null) {
				session = getJCRRepository().login(
						new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
			}
			// logger.info("Session used: " + ((JCASessionHandle) session).getXAResource().toString());
			return session;
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
		}
	}

	/**
	 * Destroys the JCR session to which the current JCA handle {@code session} of this instance is connected.
	 */
	public void destroy() {
		try {
			if (session != null) {
				JCASessionHandle handle = (JCASessionHandle) session;
				/*
				 * sending an error event forces the JCA adapter to get rid of the corrupt JCR session (calling
				 * handle.getManagedConnection().destroy() does not work, as destroy() reduces the size of the JCA pool,
				 * which would eventually lead to no JCR session being available at all)
				 */
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

	/**
	 * Prepares the JCR session to which the current JCA handle {@code session} of this instance is connected for being
	 * returned to the JCA connection pool.
	 * 
	 * @param explicitReturn
	 *            {@code true} if the JCR session shall also be returned to the JCA connection pool by this method call
	 */
	public void cleanup(boolean explicitReturn) {
		try {
			if (session != null) {
				/*
				 * workaround 1: there seems to be a problem when using JCA handles within transactions (which happens
				 * within the wasabi core almost all the time). normally the cache of a JCR session (to which a JCA
				 * handle is connected) is updated when changes of other JCR sessions are persisted. within container
				 * managed transactions this update procedure seems to fail from to time and consequently even simple
				 * test cases fail. i have not yet been able to pinpoint this bug in a way that would allow me create a
				 * very concise test-case that could be handed to the jackrabbit-developers. the bug seems very similar
				 * to this one https://issues.apache.org/jira/browse/JCR-1953, which is already marked as resolved,
				 * though.
				 */
				clearCache(session);
				/*
				 * workaround 2: the automatic logout procedure of the JCA handles does not remove existing lock-tokens
				 * from JCR sessions. so a client could retrieve a JCR session that still holds lock-tokens from the JCA
				 * connection pool.
				 */
				Locker.cleanUpLockTokens(session);

				if (explicitReturn) {
					session.logout();
				}

				session = null;
			}
		} catch (Exception e) {
			logger
					.error(
							"Fatal internal error: A JCR session could not be returned to the connection pool properly. Restart the JCR repository to avoid possible consequential errors.",
							e);
		}
	}
	
	/**
	 * Clears the item cache of the JCR session to which the given JCA handle {@code s} is connected.
	 * 
	 * @param s
	 * @throws Exception
	 */
	private void clearCache(Session s) throws Exception {
		JCASessionHandle jcaHandle = (JCASessionHandle) s;
		SessionImpl jackrabbitSession = (SessionImpl) jcaHandle.getXAResource();
		ItemManager cacheOfSession = jackrabbitSession.getItemManager();
		Method clearCacheMethod = cacheOfSession.getClass().getDeclaredMethod("dispose");
		clearCacheMethod.setAccessible(true);
		clearCacheMethod.invoke(cacheOfSession);
	}

}
