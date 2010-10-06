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

import java.util.Date;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.aop.JCRSessionInterceptor;
import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authorization.Certificate;
import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.local.ObjectServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.ObjectServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

/**
 * Class, that implements the internal access on WasabiObject objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "ObjectService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors( { JCRSessionInterceptor.class })
public class ObjectService implements ObjectServiceLocal, ObjectServiceRemote, WasabiAOP {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;
	protected JmsConnector jms;
	protected JndiConnector jndi;

	@Override
	public boolean exists(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			Node objectNode = null;
			try {
				objectNode = TransferManager.convertDTO2Node(object, s);
			} catch (WasabiException np) {
				throw new NoPermissionException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_EXISTS));
			}

			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = ObjectServiceImpl.getUUID(userNode);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
						if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
								WasabiPermission.VIEW))
							if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
								throw new NoPermissionException(WasabiExceptionMessages
										.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_EXISTS));
							else
								Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
										WasabiPermission.VIEW, true);
					} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
						throw new NoPermissionException(WasabiExceptionMessages
								.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_EXISTS));
			/* Authorization - End */

			s.getNodeByIdentifier(object.getId());
			return true;
		} catch (ItemNotFoundException infe) {
			return false;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiValueDTO getCreatedBy(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.getCreatedBy()", "VIEW", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.VIEW, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getCreatedBy()",
							"VIEW", "object"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(objectNode);
		return TransferManager.convertValue2DTO(ObjectServiceImpl.getCreatedBy(objectNode), optLockId);
	}

	@Override
	public WasabiValueDTO getCreatedOn(WasabiObjectDTO object) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.getCreatedOn()", "VIEW", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.VIEW, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getCreatedOn()",
							"VIEW", "object"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(objectNode);
		return TransferManager.convertValue2DTO(ObjectServiceImpl.getCreatedOn(objectNode), optLockId);
	}

	public JcrConnector getJcrConnector() {
		return jcr;
	}

	public JndiConnector getJndiConnector() {
		return jndi;
	}

	@Override
	public WasabiValueDTO getModifiedBy(WasabiObjectDTO object) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.getModifiedBy()", "VIEW", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.VIEW, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getModifiedBy()",
							"VIEW", "object"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(objectNode);
		return TransferManager.convertValue2DTO(ObjectServiceImpl.getModifiedBy(objectNode), optLockId);
	}

	@Override
	public WasabiValueDTO getModifiedOn(WasabiObjectDTO object) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.getModifiedOn()", "VIEW", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.VIEW, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getModifiedOn()",
							"VIEW", "object"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(objectNode);
		return TransferManager.convertValue2DTO(ObjectServiceImpl.getModifiedOn(objectNode), optLockId);
	}

	public WasabiValueDTO getName(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		long start1 = java.lang.System.nanoTime();
		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.getModifiedOn()", "VIEW or READ", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.VIEW, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getModifiedOn()",
							"VIEW or READ", "object"));
		}
		/* Authorization - End */
		long end1 = java.lang.System.nanoTime();
		long time = (end1 - start1);
		Certificate.sum = Certificate.sum + time;
		System.out.println("getName pass1: " + time);
		System.out.println("sum: " + Certificate.sum);
		System.out.println("-------");

		Long optLockId = ObjectServiceImpl.getOptLockId(objectNode);
		return TransferManager.convertValue2DTO(ObjectServiceImpl.getName(objectNode), optLockId);
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName)
			throws UnexpectedInternalProblemException {
		if (attributeName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"attributeName"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiObjectDTO> result = new Vector<WasabiObjectDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node node : ObjectServiceImpl.getObjectsByAttributeName(attributeName, s))
				if (WasabiAuthorizer.authorize(node, callerPrincipal, WasabiPermission.VIEW, s))
					result.add(TransferManager.convertNode2DTO(node));
		}
		/* Authorization - End */
		else
			for (Node node : ObjectServiceImpl.getObjectsByAttributeName(attributeName, s))
				result.add(TransferManager.convertNode2DTO(node));

		return result;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiObjectDTO> result = new Vector<WasabiObjectDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByCreator(creatorNode); ni.hasNext();) {
				Node objectNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					result.add(TransferManager.convertNode2DTO(objectNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByCreator(creatorNode); ni.hasNext();)
				result.add(TransferManager.convertNode2DTO(ni.nextNode()));

		return result;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiObjectDTO> result = new Vector<WasabiObjectDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByModifier(modifierNode); ni.hasNext();) {
				Node objectNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					result.add(TransferManager.convertNode2DTO(objectNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByModifier(modifierNode); ni.hasNext();)
				result.add(TransferManager.convertNode2DTO(ni.nextNode()));

		return result;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByName(String name) throws UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiObjectDTO> result = new Vector<WasabiObjectDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByName(name, s); ni.hasNext();) {
				Node objectNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
					result.add(TransferManager.convertNode2DTO(objectNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ObjectServiceImpl.getObjectsByName(name, s); ni.hasNext();)
				result.add(TransferManager.convertNode2DTO(ni.nextNode()));

		return result;
	}

	public String getUUID(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.READ))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getUUID()",
									"READ", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.READ, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.getUUID()", "READ",
							"object"));
		/* Authorization - End */

		return ObjectServiceImpl.getUUID(objectNode);
	}

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
		this.jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	@Override
	public void setCreatedBy(WasabiObjectDTO object, WasabiUserDTO user, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		Node userCallerNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userCallerNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.GRANT))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.setCreatedBy()", "GRANT", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.GRANT, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.setCreatedBy()",
							"GRANT", "object"));
		/* Authorization - End */

		Locker.checkOptLockId(objectNode, object, optLockId);
		ObjectServiceImpl.setCreatedBy(objectNode, userNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}

	@Override
	public void setCreatedOn(WasabiObjectDTO object, Date creationTime, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		Node userCallerNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userCallerNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.GRANT))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.setCreatedOn()", "GRANT", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.GRANT, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.setCreatedOn()",
							"GRANT", "object"));
		/* Authorization - End */

		Locker.checkOptLockId(objectNode, object, optLockId);
		ObjectServiceImpl.setCreatedOn(objectNode, creationTime, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}

	@Override
	public void setModifiedBy(WasabiObjectDTO object, WasabiUserDTO user, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userCallerNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userCallerNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.GRANT))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.setModifiedBy()", "GRANT", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.GRANT, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.setModifiedBy()",
							"GRANT", "object"));
		/* Authorization - End */

		Node userNode = TransferManager.convertDTO2Node(user, s);
		Locker.checkOptLockId(objectNode, object, optLockId);
		ObjectServiceImpl.setModifiedBy(objectNode, userNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}

	@Override
	public void setModifiedOn(WasabiObjectDTO object, Date modificationTime, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userCallerNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userCallerNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
							WasabiPermission.GRANT))
						if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
									"ObjectService.setModifiedOn()", "GRANT", "object"));
						else
							Certificate.setObjectServiceMap(userUUID, ObjectServiceImpl.getUUID(objectNode),
									WasabiPermission.GRANT, true);
				} else if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ObjectService.setModifiedOn()",
							"GRANT", "object"));
		/* Authorization - End */

		Locker.checkOptLockId(objectNode, object, optLockId);
		ObjectServiceImpl.setModifiedOn(objectNode, modificationTime, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}
}
