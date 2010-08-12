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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.authorization.WasabiRoomACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class RoomServiceImpl {

	public static Node create(String name, Node environmentNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		try {
			Node roomNode = environmentNode.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.ROOM);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiRoomACL.ACLEntryForCreate(roomNode, s);
				WasabiRoomACL.ACLEntryTemplateForCreate(roomNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			return roomNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "room", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node roomNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(roomNode);
	}

	public static Node getRoomByName(Node roomNode, String name) throws UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
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

	public static Vector<Node> getRooms(Node environment, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByCreationDate(Node environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByCreationDate(Node environment, Date startDate, Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByCreator(WasabiUserDTO creator, Node environment) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByModificationDate(Node environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByModificationDate(Node environment, Date startDate, Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getRoomsByModifier(WasabiUserDTO modifier, Node environment) {
		// TODO Auto-generated method stub
		return null;
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

	public static void move(Node roomNode, Node newEnvironmentNode) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		try {
			roomNode.getSession().move(roomNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.ROOMS + "/" + roomNode.getName());
		} catch (ItemExistsException iee) {
			try {
				String name = roomNode.getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "room", name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node roomNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(roomNode);
	}

	public static void rename(Node roomNode, String name) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(roomNode, name);
	}
}
