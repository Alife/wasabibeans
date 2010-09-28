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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.authorization.WasabiRoomACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiPipelineDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;

/**
 * Class, that implements the internal access on WasabiRoom objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "RoomService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RoomService extends ObjectService implements RoomServiceLocal, RoomServiceRemote {

	@Override
	public WasabiRoomDTO create(String name, WasabiRoomDTO environment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, NoPermissionException,
			ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
						WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.create()",
							"INSERT or WRITE", "environment"));
			/* Authorization - End */

			Locker.recognizeLockTokens(s, environment);
			Node roomNode = RoomServiceImpl.create(name, environmentNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(roomNode, environmentNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(roomNode, environment);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node environmentNode = RoomServiceImpl.getEnvironment(roomNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, new int[] { WasabiPermission.VIEW,
					WasabiPermission.READ }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "RoomService.getEnvironment()",
						"VIEW or READ"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(roomNode);
		return TransferManager.convertValue2DTO(environmentNode, optLockId);
	}

	@Override
	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSessionTx();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomByNameNode = RoomServiceImpl.getRoomByName(roomNode, name);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(roomByNameNode, callerPrincipal, new int[] { WasabiPermission.VIEW,
					WasabiPermission.READ }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "RoomService.getRoomByName()",
						"VIEW or READ"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(roomByNameNode, room);
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(roomNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);
			for (String string : authorizedRooms) {
				Node aNode = RoomServiceImpl.getRoomById(string, s);
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(aNode, room));
			}
		}
		/* Authorization - End */
		else {
			NodeIterator ni = RoomServiceImpl.getRooms(roomNode);
			while (ni.hasNext())
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(ni.nextNode(), room));
		}

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO environment, int depth) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = RoomServiceImpl.getRoomsFiltered(environmentNode, depth, callerPrincipal,
					s);
			for (String string : authorizedRooms) {
				Node aNode = RoomServiceImpl.getRoomById(string, s);
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(aNode, environment));
			}
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRooms(environmentNode, depth))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);

			for (Node room : RoomServiceImpl.getRoomsByCreationDate(environmentNode, startDate, endDate)) {
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
			}
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByCreationDate(environmentNode, startDate, endDate))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = RoomServiceImpl.getRoomsFiltered(environmentNode, depth, callerPrincipal,
					s);
			for (Node room : RoomServiceImpl.getRoomsByCreationDate(environmentNode, startDate, endDate, depth)) {
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
			}
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByCreationDate(environmentNode, startDate, endDate, depth))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = RoomServiceImpl.getRoomsByCreator(creatorNode); ni.hasNext();) {
				Node roomNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.VIEW, s))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(roomNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = RoomServiceImpl.getRoomsByCreator(creatorNode); ni.hasNext();)
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator, WasabiRoomDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);
			for (Node room : RoomServiceImpl.getRoomsByCreator(creatorNode, environmentNode))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByCreator(creatorNode, environmentNode))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);
			for (Node room : RoomServiceImpl.getRoomsByModificationDate(environmentNode, startDate, endDate))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByModificationDate(environmentNode, startDate, endDate))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);
			for (Node room : RoomServiceImpl.getRoomsByModificationDate(environmentNode, startDate, endDate, depth))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByModificationDate(environmentNode, startDate, endDate, depth))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (NodeIterator ni = RoomServiceImpl.getRoomsByModifier(modifierNode); ni.hasNext();) {
				Node roomNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.VIEW, s))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(roomNode));
			}
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = RoomServiceImpl.getRoomsByModifier(modifierNode); ni.hasNext();)
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return rooms;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier, WasabiRoomDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ROOM, s);
			for (Node room : RoomServiceImpl.getRoomsByModifier(modifierNode, environmentNode))
				if (authorizedRooms.contains(ObjectServiceImpl.getUUID(room)))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByModifier(modifierNode, environmentNode))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public WasabiRoomDTO getRootHome() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootHome(s));
	}

	@Override
	public WasabiRoomDTO getRootRoom() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootRoom(s));
	}

	@Override
	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException, NoPermissionException {
		Node roomNode = null;
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			roomNode = TransferManager.convertDTO2Node(room, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE) {
				if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.move()", "WRITE", "room"));
				if (!WasabiAuthorizer.authorize(newEnvironmentNode, callerPrincipal, new int[] {
						WasabiPermission.INSERT, WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.move()",
							"INSERT or WRITE", "newEnvironment"));
			}
			/* Authorization - End */

			Locker.recognizeLockTokens(s, room, newEnvironment);
			Locker.acquireLock(roomNode, room, false, s, locker);
			Locker.checkOptLockId(roomNode, room, optLockId);
			RoomServiceImpl.move(roomNode, newEnvironmentNode, callerPrincipal, s);
			s.save();
			EventCreator.createMovedEvent(roomNode, newEnvironmentNode, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(roomNode, room, s, locker);
		}
	}

	@Override
	public void remove(WasabiRoomDTO room) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException, ConcurrentModificationException {
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node roomNode = TransferManager.convertDTO2Node(room, s);
			Locker.recognizeLockTokens(s, room);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE) {
				if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.remove()", "WRITE",
							"room"));
				else
					WasabiRoomACL.remove(roomNode, callerPrincipal, s);
			}
			/* Authorization - End */

			else {
				// TODO special case for events due to recursive deletion of subtree
				EventCreator.createRemovedEvent(roomNode, jms, callerPrincipal);
				RoomServiceImpl.remove(roomNode);
			}
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void rename(WasabiRoomDTO room, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node roomNode = null;
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			roomNode = TransferManager.convertDTO2Node(room, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.rename()", "WRITE",
							"room"));
			/* Authorization - End */

			Locker.recognizeLockTokens(s, room);
			Locker.acquireLock(roomNode, room, false, s, locker);
			Locker.checkOptLockId(roomNode, room, optLockId);
			RoomServiceImpl.rename(roomNode, name, callerPrincipal);
			EventCreator.createPropertyChangedEvent(roomNode, WasabiProperty.NAME, name, jms, callerPrincipal);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(roomNode, room, s, locker);
		}
	}

	@Override
	public void setPipeline(WasabiRoomDTO room, WasabiPipelineDTO pipeline) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		try {
			Node roomNode = TransferManager.convertDTO2Node(room, s);
			Node pipelineNode = TransferManager.convertDTO2Node(pipeline, s);
			Locker.recognizeLockTokens(s, room, pipeline);
			RoomServiceImpl.setPipeline(roomNode, pipelineNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiPipelineDTO getPipeline(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		return TransferManager.convertNode2DTO(RoomServiceImpl.getPipeline(roomNode));
	}
}
