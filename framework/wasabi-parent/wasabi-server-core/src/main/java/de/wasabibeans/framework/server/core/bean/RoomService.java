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

import de.wasabibeans.framework.server.core.authorization.Certificate;
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
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
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
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
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
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.create()", "INSERT or WRITE",
						"environment"));
		/* Authorization - End */

		Node roomNode = RoomServiceImpl.create(name, environmentNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createCreatedEvent(roomNode, environmentNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(roomNode, environment);
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node environmentNode = RoomServiceImpl.getEnvironment(roomNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "RoomService.getEnvironment()",
						"VIEW"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(roomNode);
		return TransferManager.convertValue2DTO(environmentNode, optLockId);
	}

	@Override
	public WasabiPipelineDTO getPipeline(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node roomNode = TransferManager.convertDTO2Node(room, s);

		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.getPipeline()", "GRANT",
						"room"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(RoomServiceImpl.getPipeline(roomNode));
	}

	@Override
	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomByNameNode = RoomServiceImpl.getRoomByName(roomNode, name);
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		String userUUID = ObjectServiceImpl.getUUID(userNode);

		long start1 = java.lang.System.nanoTime();
		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
					if (!Certificate.getCertificate(userUUID, ObjectServiceImpl.getUUID(roomByNameNode),
							WasabiPermission.VIEW))
						if (!WasabiAuthorizer.authorize(roomByNameNode, callerPrincipal, WasabiPermission.VIEW, s))
							throw new NoPermissionException(WasabiExceptionMessages.get(
									WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
									"RoomService.getRoomByName()", "VIEW"));
						else
							Certificate.setCertificate(userUUID, ObjectServiceImpl.getUUID(roomByNameNode),
									WasabiPermission.VIEW);
				} else if (!WasabiAuthorizer.authorize(roomByNameNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "RoomService.getRoomByName()",
							"VIEW"));
		/* Authorization - End */
		long end1 = java.lang.System.nanoTime();
		long time = (end1 - start1);
		Certificate.sum = Certificate.sum + time;
		System.out.println("getRoomByName pass1: " + time);

		return TransferManager.convertNode2DTO(roomByNameNode, room);
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node roomNode : RoomServiceImpl.getRoomsByCreator(creatorNode, environmentNode))
				if (WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.VIEW, s))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(roomNode));
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
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
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node roomNode : RoomServiceImpl.getRoomsByModifier(modifierNode, environmentNode))
				if (WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.VIEW, s))
					rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(roomNode));
		}
		/* Authorization - End */
		else
			for (Node room : RoomServiceImpl.getRoomsByModifier(modifierNode, environmentNode))
				rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room, environment));

		return rooms;
	}

	@Override
	public WasabiRoomDTO getRootHome() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootHome(s));
	}

	@Override
	public WasabiRoomDTO getRootRoom() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootRoom(s));
	}

	@Override
	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(newEnvironmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.move()", "INSERT or WRITE",
						"newEnvironment"));
			if (!WasabiAuthorizer.authorizeChildreen(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.move()", "WRITE",
						"room and sub objects"));
		}
		/* Authorization - End */

		Locker.checkOptLockId(roomNode, room, optLockId);
		RoomServiceImpl.move(roomNode, newEnvironmentNode, true, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createMovedEvent(roomNode, newEnvironmentNode, jms, callerPrincipal);
	}

	@Override
	public void remove(WasabiRoomDTO room, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException, ConcurrentModificationException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Locker.checkOptLockId(roomNode, room, optLockId);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.remove()", "WRITE", "room"));
			else
				WasabiRoomACL.remove(roomNode, callerPrincipal, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms);
		}
		/* Authorization - End */
		else {
			RoomServiceImpl.remove(roomNode, true, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms, callerPrincipal);
		}

	}

	@Override
	public void rename(WasabiRoomDTO room, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomNode = TransferManager.convertDTO2Node(room, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.rename()", "WRITE", "room"));
		/* Authorization - End */

		Locker.checkOptLockId(roomNode, room, optLockId);
		RoomServiceImpl.rename(roomNode, name, true, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(roomNode, WasabiProperty.NAME, name, jms, callerPrincipal);
	}

	@Override
	public void setPipeline(WasabiRoomDTO room, WasabiPipelineDTO pipeline, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException,
			ConcurrentModificationException {
		Session s = jcr.getJCRSession();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Node pipelineNode = TransferManager.convertDTO2Node(pipeline, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "RoomService.setPipeline()", "GRANT",
						"room"));
		/* Authorization - End */

		RoomServiceImpl.setPipeline(roomNode, pipelineNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
	}
}
