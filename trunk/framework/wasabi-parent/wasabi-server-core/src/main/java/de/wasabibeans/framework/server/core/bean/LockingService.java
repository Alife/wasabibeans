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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.LockingServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.remote.LockingServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless(name = "LockingService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class LockingService implements LockingServiceLocal, LockingServiceRemote {

	@EJB
	private LockingHelperLocal locker;

	protected JndiConnector jndi;
	protected JcrConnector jcr;

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	public <T extends WasabiObjectDTO> T lock(T object, boolean isDeep) throws UnexpectedInternalProblemException,
			ConcurrentModificationException, ObjectDoesNotExistException, LockingException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		Locker.recognizeLockTokens(s, object);
		String lockToken = Locker.acquireLock(objectNode, object, isDeep, s, locker);
		return TransferManager.enrichWithLockToken(object, lockToken, isDeep);
	}

	public <T extends WasabiObjectDTO> T unlock(T object) throws UnexpectedInternalProblemException {
		if (object == null) {
			return null;
		}
		if (object.getLockToken() == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_LOCKING_TOKEN_NULL);
		}

		Locker.releaseLock(object.getId(), object.getLockToken(), locker);
		return TransferManager.removeLockToken(object);
	}
}