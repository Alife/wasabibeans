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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.authorization.WasabiContainerACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ContainerServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.ContainerServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.ContainerServiceRemote;

/**
 * Class, that implements the internal access on WasabiContainer objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "ContainerService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ContainerService extends ObjectService implements ContainerServiceLocal, ContainerServiceRemote {

	@Override
	public WasabiContainerDTO create(String name, WasabiLocationDTO environment) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ContainerService.create()",
						"INSERT or WRITE", "environment"));
		/* Authorization - End */

		Node containerNode = ContainerServiceImpl.create(name, environmentNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createCreatedEvent(containerNode, environmentNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(containerNode, environment);
	}

	@Override
	public WasabiContainerDTO getContainerByName(WasabiLocationDTO location, String name)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Node containerNode = ContainerServiceImpl.getContainerByName(locationNode, name);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
						"ContainerService.getContainerByName()", "VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(containerNode, location);
	}

	@Override
	public Vector<WasabiContainerDTO> getContainers(WasabiLocationDTO location) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(locationNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);
			for (String string : authorizedRooms) {
				Node aNode = RoomServiceImpl.getRoomById(string, s);
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(aNode, location));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ContainerServiceImpl.getContainers(locationNode); ni.hasNext();)
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);

			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate)) {
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(container)))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));
			}
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);

			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate)) {
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(container)))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));
			}
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate,
					depth))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = ContainerServiceImpl.getContainersByCreator(creatorNode); ni.hasNext();) {
				Node containerNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.VIEW, s))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ContainerServiceImpl.getContainersByCreator(creatorNode); ni.hasNext();)
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node containerNode : ContainerServiceImpl.getContainersByCreator(creatorNode, environmentNode))
				if (WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.VIEW, s))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode));
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByCreator(creatorNode, environmentNode))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);
			for (Node containerNode : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(containerNode)))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode, environment));
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);
			for (Node containerNode : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate, depth))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(containerNode)))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode, environment));
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate, depth))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = ContainerServiceImpl.getContainersByModifier(modifierNode); ni.hasNext();) {
				Node containerNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.VIEW, s))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = ContainerServiceImpl.getContainersByModifier(modifierNode); ni.hasNext();)
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node containerNode : ContainerServiceImpl.getContainersByModifier(modifierNode, environmentNode))
				if (WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.VIEW, s))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode));
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByModifier(modifierNode, environmentNode))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByNamePattern(WasabiLocationDTO environment, String pattern)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.CONTAINER, s);
			for (Node containerNode : ContainerServiceImpl.getContainersByNamePattern(environmentNode, pattern))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(containerNode)))
					containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(containerNode, environment));
		}
		/* Authorization - End */
		else
			for (Node container : ContainerServiceImpl.getContainersByNamePattern(environmentNode, pattern))
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container, environment));

		return containers;
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiContainerDTO container) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node containerNode = TransferManager.convertDTO2Node(container, s);
		Node environmentNode = ContainerServiceImpl.getEnvironment(containerNode);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
						"ContainerService.getEnvironment()", "VIEW"));
		/* Authorization - End */
		Long optLockId = ObjectServiceImpl.getOptLockId(containerNode);
		return TransferManager.convertValue2DTO(environmentNode, optLockId);
	}

	@Override
	public void move(WasabiContainerDTO container, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node containerNode = TransferManager.convertDTO2Node(container, s);
		Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(newEnvironmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ContainerService.move()",
						"INSERT or WRITE", "newEnvironment"));
			if (!WasabiAuthorizer.authorizeChildreen(containerNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ContainerService.move()", "WRITE",
						"container and sub objects"));
		}
		/* Authorization - End */

		Locker.checkOptLockId(containerNode, container, optLockId);
		ContainerServiceImpl.move(containerNode, newEnvironmentNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createMovedEvent(containerNode, newEnvironmentNode, jms, callerPrincipal);
	}

	@Override
	public void remove(WasabiContainerDTO container, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node containerNode = TransferManager.convertDTO2Node(container, s);
		Locker.checkOptLockId(containerNode, container, optLockId);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ContainerService.remove()", "WRITE",
						"container"));
			else
				WasabiContainerACL.remove(containerNode, callerPrincipal, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		}
		/* Authorization - End */
		else {
			// TODO special case for events due to recursive deletion of subtree
			EventCreator.createRemovedEvent(containerNode, jms, callerPrincipal);
			ContainerServiceImpl.remove(containerNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		}

	}

	@Override
	public void rename(WasabiContainerDTO container, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ObjectDoesNotExistException,
			ConcurrentModificationException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node containerNode = TransferManager.convertDTO2Node(container, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(containerNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ContainerService.rename()", "WRITE",
						"container"));
		/* Authorization - End */

		Locker.checkOptLockId(containerNode, container, optLockId);
		ContainerServiceImpl.rename(containerNode, name, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(containerNode, WasabiProperty.NAME, name, jms, callerPrincipal);
	}
}