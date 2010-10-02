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

package de.wasabibeans.framework.server.core.locking;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class LockingHelper implements LockingHelperLocal {

	private JndiConnector jndi;
	private JcrConnector jcr;

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	@Override
	public String acquireLock(String nodeId, boolean isDeep) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			LockManager lockManager = s.getWorkspace().getLockManager();
			String lockToken = lockManager.lock(s.getNodeByIdentifier(nodeId).getPath(), isDeep, false, 10, null)
					.getLockToken();
			lockManager.removeLockToken(lockToken);
			return lockToken;
		} finally {
			jcr.cleanup(false);
		}
	}

	@Override
	public void releaseLock(String nodeId, String lockToken) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException {
		try {
			Session s = jcr.getJCRSession();
			LockManager lockManager = s.getWorkspace().getLockManager();
			lockManager.addLockToken(lockToken);
			lockManager.unlock(s.getNodeByIdentifier(nodeId).getPath());
		} catch (ItemNotFoundException infe) {
			/* This happens if a node shall be unlocked that does not exist any more. Just do nothing. */
		} finally {
			jcr.cleanup(false);
		}
	}
}
