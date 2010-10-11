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
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.jcr.Node;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.aop.JCRSessionInterceptor;
import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.LockingServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.remote.LockingServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@SecurityDomain("wasabi")
@Stateless(name = "LockingService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors( { JCRSessionInterceptor.class })
public class LockingService implements LockingServiceLocal, LockingServiceRemote, WasabiAOP {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;

	protected JndiConnector jndi;
	@EJB
	private LockingHelperLocal locker;

	public JcrConnector getJcrConnector() {
		return jcr;
	}

	public JndiConnector getJndiConnector() {
		return jndi;
	}

	public <T extends WasabiObjectDTO> T lock(T object, boolean isDeep) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException, ConcurrentModificationException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LockingService.lock()", "WRITE",
								"object"));
		/* Authorization - End */

		String lockToken = Locker.acquireLock(objectNode, object, isDeep, s, locker);
		return TransferManager.enrichWithLockToken(object, lockToken, isDeep);
	}

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	public <T extends WasabiObjectDTO> T unlock(T object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException, ConcurrentModificationException {
		if (object == null) {
			return null;
		}
		if (object.getLockToken() == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.LOCKING_TOKEN_NULL);
		}

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LockingService.unlock()", "WRITE",
						"object"));
		/* Authorization - End */

		Locker.releaseLock(objectNode, s);
		return TransferManager.removeLockToken(object);
	}
}
