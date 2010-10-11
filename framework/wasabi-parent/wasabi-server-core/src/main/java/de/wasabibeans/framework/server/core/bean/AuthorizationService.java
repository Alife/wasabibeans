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
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.AuthorizationServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.local.AuthorizationServiceLocal;
import de.wasabibeans.framework.server.core.remote.AuthorizationServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@SecurityDomain("wasabi")
@Stateless(name = "AuthorizationService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors( { JCRSessionInterceptor.class })
public class AuthorizationService implements AuthorizationServiceLocal, AuthorizationServiceRemote, WasabiAOP {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;
	protected JndiConnector jndi;

	@Override
	public JcrConnector getJcrConnector() {
		return jcr;
	}

	@Override
	public JndiConnector getJndiConnector() {
		return jndi;
	}

	@Override
	public boolean hasPermission(WasabiObjectDTO object, int permission) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userCallerNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userCallerUUID = ObjectServiceImpl.getUUID(userCallerNode);
		String objectUUID = ObjectServiceImpl.getUUID(objectNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getCreatedBy()",
							"VIEW", "object"));
		/* Authorization - End */

		return AuthorizationServiceImpl.hasPermission(objectUUID, userCallerUUID, permission, objectNode,
				userCallerNode, s);
	}

	@Override
	public boolean hasPermission(WasabiObjectDTO object, WasabiUserDTO user, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException {

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		String objectUUID = ObjectServiceImpl.getUUID(objectNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getCreatedBy()",
							"VIEW", "object"));
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getCreatedBy()",
							"VIEW", "user"));
			}
		/* Authorization - End */

		return AuthorizationServiceImpl.hasPermission(objectUUID, userUUID, permission, objectNode, userNode, s);
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

	/**
	 * Stupid service method, should be deleted some day...
	 */
	@Override
	public boolean returnTrue() {
		return true;
	}

	@Override
	public boolean existsCertificate(WasabiObjectDTO wasabiObject, WasabiUserDTO wasabiUser, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException {
		// TODO Auto-generated method stub
		return false;
	}
}
