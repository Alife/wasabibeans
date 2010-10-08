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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.authorization.WasabiUserSQL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class UserServiceImpl {

	public static Node create(String name, String password, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			// JCR
			Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			Node userNode = rootOfUsersNode.addNode(name, WasabiNodeType.USER);
			setDisplayName(userNode, name, s, false, null);
			Node homeRoomNode = RoomServiceImpl.create(name, RoomServiceImpl.getRootHome(s), s, false, callerPrincipal);
			userNode.setProperty(WasabiNodeProperty.HOME_ROOM, homeRoomNode);
			setStartRoom(userNode, homeRoomNode, s, false, null);
			enter(userNode, homeRoomNode, s, false);
			GroupServiceImpl.addMember(GroupServiceImpl.getWasabiGroup(s), userNode, s, false);

			// special case when creating the root user
			if (name.equals(WasabiConstants.ROOT_USER_NAME)) {
				ObjectServiceImpl.created(userNode, s, false, null, true);
			} else {
				ObjectServiceImpl.created(userNode, s, false, callerPrincipal, true);
			}

			/* ACL Environment - Begin */
			WasabiUserSQL.SqlQueryForCreate(name, password);
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiUserACL.ACLEntryForCreate(userNode, homeRoomNode, callerPrincipal, s);
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return userNode;
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

	public static void enter(Node userNode, Node roomNode, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			Node userRef = roomNode.getNode(WasabiNodeProperty.PRESENT_USERS).addNode(userNode.getIdentifier(),
					WasabiNodeType.OBJECT_REF);
			userRef.setProperty(WasabiNodeProperty.REFERENCED_OBJECT, userNode);
			Node roomRef = userNode.getNode(WasabiNodeProperty.WHEREABOUTS).addNode(roomNode.getIdentifier(),
					WasabiNodeType.OBJECT_REF);
			roomRef.setProperty(WasabiNodeProperty.REFERENCED_OBJECT, roomNode);

			if (doJcrSave) {
				s.save();
			}
		} catch (ItemExistsException iee) {
			// do nothing, user is already present in room
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

	public static NodeIterator getAllUsers(Session s) throws UnexpectedInternalProblemException {
		try {
			Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			return rootOfUsersNode.getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getDisplayName(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.DISPLAY_NAME).getString();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getHomeRoom(Node userNode) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException {
		try {
			return userNode.getProperty(WasabiNodeProperty.HOME_ROOM).getNode();
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns a {@code NodeIterator} containing nodes of type wasabi:objectref that point to the actual
	 * wasabi:group-nodes. So the returned {@code NodeIterator} does NOT contain the actual wasabi:group-nodes (this is
	 * due to efficiency reasons).
	 * 
	 * @param userNode
	 *            the node representing a wasabi-user
	 * @return {@code NodeIterator} containing nodes of type wasabi:objectref
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getMemberships(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getNode(WasabiNodeProperty.MEMBERSHIPS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getPassword(Node userNode) throws UnexpectedInternalProblemException {
		return WasabiUserSQL.SqlQueryForGetPassword(userNode);
	}

	public static Node getStartRoom(Node userNode) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException {
		try {
			return userNode.getProperty(WasabiNodeProperty.START_ROOM).getNode();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean getStatus(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.ACTIVE).getBoolean();
		} catch (PathNotFoundException pnfe) {
			return false;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getUserById(String id, Session s) throws UnexpectedInternalProblemException,
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

	public static Node getUserByName(Node roomNode, String userName) throws UnexpectedInternalProblemException {
		try {
			Node userNode = getUserByName(userName, roomNode.getSession());
			if (userNode == null) { // user does not exist any more
				return null;
			}
			if (roomNode.getNode(WasabiNodeProperty.PRESENT_USERS).hasNode(userNode.getIdentifier())) {
				// user is present in room
				return userNode;
			}
			// user exists but is not present in room
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getUserByName(String userName, Session s) throws UnexpectedInternalProblemException {
		try {
			Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			return rootOfUsersNode.getNode(userName);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns a {@code NodeIterator} containing nodes of type wasabi:objectref that point to the actual
	 * wasabi:user-nodes. So the returned {@code NodeIterator} does NOT contain the actual wasabi:user-nodes (this is
	 * due to efficiency reasons).
	 * 
	 * @param roomNode
	 *            the node representing a wasabi-room
	 * @return {@code NodeIterator} containing nodes of type wasabi:objectref
	 * @throws UnexpectedInternalProblemException
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getUsers(Node roomNode) throws UnexpectedInternalProblemException {
		try {
			return roomNode.getNode(WasabiNodeProperty.PRESENT_USERS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getUsersByDisplayName(String displayName, Session s)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodeByPropertyStringValue(WasabiNodeType.USER, WasabiNodeProperty.DISPLAY_NAME,
				displayName, s);
	}

	/**
	 * Returns a {@code NodeIterator} containing nodes of type wasabi:objectref that point to the actual
	 * wasabi:room-nodes. So the returned {@code NodeIterator} does NOT contain the actual wasabi:room-nodes (this is
	 * due to efficiency reasons).
	 * 
	 * @param userNode
	 *            the node representing a wasabi-user
	 * @return {@code NodeIterator} containing nodes of type wasabi:objectref
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getWhereabouts(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getNode(WasabiNodeProperty.WHEREABOUTS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void leave(Node userNode, Node roomNode, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Node userRef = roomNode.getNode(WasabiNodeProperty.PRESENT_USERS).getNode(userNode.getIdentifier());
			userRef.remove();
			Node roomRef = userNode.getNode(WasabiNodeProperty.WHEREABOUTS).getNode(roomNode.getIdentifier());
			roomRef.remove();

			if (doJcrSave) {
				s.save();
			}
		} catch (PathNotFoundException pnfe) {
			// do nothing, user not present
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node userNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			/* ACL Environment - Begin */
			WasabiUserSQL.SqlQueryForRemove(userNode);
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiUserSQL.removeRights(userNode);
			/* ACL Environment - End */

			// JCR
			try {
				Node homeRoom = getHomeRoom(userNode);
				RoomServiceImpl.remove(homeRoom, false, s, false, false, null, callerPrincipal);
			} catch (TargetDoesNotExistException tdnee) {
				// nothing to remove if home room does not exist
			}
			ObjectServiceImpl.remove(userNode, s, false);

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

	public static void rename(Node userNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, TargetDoesNotExistException,
			ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			String wasabiUser = ObjectServiceImpl.getName(userNode);

			// JCR
			ObjectServiceImpl.rename(userNode, name, s, false, callerPrincipal);
			RoomServiceImpl.rename(UserServiceImpl.getHomeRoom(userNode), name, false, s, false, callerPrincipal);

			// Database
			WasabiUserSQL.SqlQueryForRename(wasabiUser, name);

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

	public static void setDisplayName(Node userNode, String displayName, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			userNode.setProperty(WasabiNodeProperty.DISPLAY_NAME, displayName);
			ObjectServiceImpl.modified(userNode, s, false, callerPrincipal, false);

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

	public static void setPassword(Node userNode, String password) throws UnexpectedInternalProblemException {
		WasabiUserSQL.SqlQueryForSetPassword(userNode, password);
	}

	public static void setStartRoom(Node userNode, Node roomNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			userNode.setProperty(WasabiNodeProperty.START_ROOM, roomNode);
			ObjectServiceImpl.modified(userNode, s, false, callerPrincipal, false);

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

	public static void setStatus(Node userNode, boolean active, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			userNode.setProperty(WasabiNodeProperty.ACTIVE, active);
			ObjectServiceImpl.modified(userNode, s, false, callerPrincipal, false);

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
}
