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

package de.wasabibeans.framework.server.core.internal;

import java.util.Date;
import java.util.Vector;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.authorization.WasabiRoomACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class RoomServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			Node roomNode = environmentNode.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.ROOM);
			// special case when creating the room of the root user or when creating the wasabi home room
			if (name.equals(WasabiConstants.ROOT_USER_NAME) || name.equals(WasabiConstants.HOME_ROOM_NAME)) {
				ObjectServiceImpl.created(roomNode, s, false, null, true);
			} else {
				ObjectServiceImpl.created(roomNode, s, false, callerPrincipal, true);
			}

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiRoomACL.ACLEntryForCreate(roomNode, s, false);
				WasabiRoomACL.ACLEntryTemplateForCreate(roomNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return roomNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name), iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node roomNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(roomNode);
	}

	public static Node getRoomByName(Node roomNode, String name) throws UnexpectedInternalProblemException {
		try {
			return roomNode.getNode(WasabiNodeProperty.ROOMS + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getRooms(Node roomNode) throws UnexpectedInternalProblemException {
		try {
			return roomNode.getNode(WasabiNodeProperty.ROOMS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Vector<Node> getRooms(Node environmentNode, int depth) throws UnexpectedInternalProblemException {
		Vector<Node> allSubRooms = new Vector<Node>();
		for (NodeIterator ni = getRooms(environmentNode); ni.hasNext();) {
			Node subroom = ni.nextNode();
			allSubRooms.add(subroom);
			if (depth > 0) {
				allSubRooms.addAll(getRooms(subroom, depth - 1));
			} else if (depth < 0) {
				allSubRooms.addAll(getRooms(subroom, depth));
			}
		}
		return allSubRooms;
	}

	public static Vector<String> getRoomsFiltered(Node environmentNode, int depth, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException {
		Vector<String> authorizedRooms = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
				WasabiPermission.VIEW, WasabiType.ROOM, s);

		NodeIterator ni = RoomServiceImpl.getRooms(environmentNode);

		while (ni.hasNext()) {
			Node subroom = ni.nextNode();
			if (depth > 0)
				authorizedRooms.addAll(getRoomsFiltered(subroom, depth - 1, callerPrincipal, s));
			else if (depth < 0)
				authorizedRooms.addAll(getRoomsFiltered(subroom, depth, callerPrincipal, s));
		}

		return authorizedRooms;
	}

	public static Node getRoomById(String id, Session s) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		try {
			return s.getNodeByIdentifier(id);
		} catch (ItemNotFoundException infe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages
					.get(WasabiExceptionMessages.OBJECT_DNE_ID, id), infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Vector<Node> getRoomsByCreationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.ROOMS, startDate, endDate);
	}

	public static Vector<Node> getRoomsByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.ROOMS, startDate, endDate,
				depth);
	}

	public static NodeIterator getRoomsByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, WasabiNodeType.ROOM);
	}

	public static Vector<Node> getRoomsByCreator(Node creatorNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, environmentNode, WasabiNodeProperty.ROOMS);
	}

	public static Vector<Node> getRoomsByModificationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.ROOMS, startDate,
				endDate);
	}

	public static Vector<Node> getRoomsByModificationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.ROOMS, startDate,
				endDate, depth);
	}

	public static NodeIterator getRoomsByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, WasabiNodeType.ROOM);
	}

	public static Vector<Node> getRoomsByModifier(Node modifierNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, environmentNode, WasabiNodeProperty.ROOMS);
	}

	public static Node getRootHome(Session s) throws UnexpectedInternalProblemException {
		try {
			return s.getRootNode().getNode(
					WasabiConstants.ROOT_ROOM_NAME + "/" + WasabiNodeProperty.ROOMS + "/"
							+ WasabiConstants.HOME_ROOM_NAME);
		} catch (PathNotFoundException pnfe) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_HOME_ROOM, pnfe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getRootRoom(Session s) throws UnexpectedInternalProblemException {
		try {
			return s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
		} catch (PathNotFoundException pnfe) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_ROOT_ROOM, pnfe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean isHomeRoom(Node roomNode, Session s) throws UnexpectedInternalProblemException {
		try {
			return RoomServiceImpl.getEnvironment(roomNode).getIdentifier().equals(
					RoomServiceImpl.getRootHome(s).getIdentifier());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void move(Node roomNode, Node newEnvironmentNode, boolean checkIfHomeRoom, Session s,
			boolean doJcrSave, String callerPrincipal) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ObjectDoesNotExistException, ConcurrentModificationException {
		try {
			if (checkIfHomeRoom
					&& RoomServiceImpl.getEnvironment(roomNode).getIdentifier().equals(
							RoomServiceImpl.getRootHome(s).getIdentifier())) {
				throw new IllegalArgumentException("A user's home-room cannot be removed.");
			}

			roomNode.getSession().move(roomNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.ROOMS + "/" + roomNode.getName());
			ObjectServiceImpl.modified(roomNode, s, false, callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiRoomACL.ACLEntryForMove(roomNode, s, false);
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_ENV, iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node roomNode, boolean checkIfHomeRoom, Session s, boolean doJcrSave,
			boolean throwEvents, JmsConnector jms, String callerPrincipal) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		try {
			if (checkIfHomeRoom && isHomeRoom(roomNode, s)) {
				throw new IllegalArgumentException("A user's home-room cannot be removed.");
			}
			ObjectServiceImpl.removeRecursive(roomNode, s, throwEvents, jms, callerPrincipal);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node roomNode, String name, boolean checkIfHomeRoom, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			if (checkIfHomeRoom
					&& RoomServiceImpl.getEnvironment(roomNode).getIdentifier().equals(
							RoomServiceImpl.getRootHome(s).getIdentifier())) {
				throw new IllegalArgumentException("A user's home-room cannot be renamed.");
			}
			ObjectServiceImpl.rename(roomNode, name, s, false, callerPrincipal);

			if (doJcrSave) {
				s.save();
			}
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name), iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// ------------------------------------- Wasabi Pipes -----------------------------------------------------

	public static void setPipeline(Node roomNode, Node pipelineNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			roomNode.setProperty(WasabiNodeProperty.PIPELINE, pipelineNode);
			ObjectServiceImpl.modified(roomNode, s, false, callerPrincipal, false);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getPipeline(Node roomNode) throws TargetDoesNotExistException,
			UnexpectedInternalProblemException {
		try {
			return roomNode.getProperty(WasabiNodeProperty.PIPELINE).getNode();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
