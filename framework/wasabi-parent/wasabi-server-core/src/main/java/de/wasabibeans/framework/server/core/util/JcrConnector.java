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

public class JcrConnector {

	private static WasabiLogger logger = WasabiLogger.getLogger(JcrConnector.class);
	
	private Repository jcrRepository;
	private JndiConnector jndi;

	public static JcrConnector getJCRConnector() {
		return new JcrConnector();
	}

	public static JcrConnector getJCRConnector(JndiConnector jndi) {
		return new JcrConnector(jndi);
	}

	public JcrConnector() {
		this.jndi = JndiConnector.getJNDIConnector();
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
			Session s = getJCRRepository().login(
					new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
//			logger.info("Session used: " + ((JCASessionHandle) s).getXAResource().toString());
			return s;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
