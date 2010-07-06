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

import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class JCRConnector {

	private Repository jcrRepository;
	private JNDIConnector jndi;

	public static JCRConnector getJCRConnector() {
		return new JCRConnector();
	}

	public JCRConnector() {
		this.jndi = JNDIConnector.getJNDIConnector();
	}

	public Repository getJCRRepository() throws UnexpectedInternalProblemException {
		if (this.jcrRepository == null) {
			this.jcrRepository = (Repository) jndi.localLookup(WasabiConstants.JNDI_JCR_DATASOURCE);
		}
		return this.jcrRepository;
	}

	@SuppressWarnings("unchecked")
	public void storeJCRSession(String username, Session session) throws UnexpectedInternalProblemException {
		ConcurrentHashMap<String, Session> user2session = (ConcurrentHashMap<String, Session>) jndi
				.lookup(WasabiConstants.JNDI_JCR_USER2SESSION);
		user2session.put(username, session);
	}

	@SuppressWarnings("unchecked")
	public Session getJCRSession(String username) throws UnexpectedInternalProblemException {
		ConcurrentHashMap<String, Session> user2session = (ConcurrentHashMap<String, Session>) jndi
				.lookup(WasabiConstants.JNDI_JCR_USER2SESSION);
		return user2session.get(username);
	}

	public Session getJCRSession() throws UnexpectedInternalProblemException {
		try {
			return getJCRRepository().login(new SimpleCredentials("user", "pwd".toCharArray()));
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
