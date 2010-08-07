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

import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;

import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.authorization.WasabiUserSQL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class UserServiceImpl {

	public static Node create(String name, String password, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		if (password == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"password"));
		}

		try {
			// JCR
			Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			Node userNode = rootOfUsersNode.addNode(name, WasabiNodeType.WASABI_USER);
			setDisplayName(userNode, name);
			Node homeRoomNode = RoomServiceImpl.create(name, RoomServiceImpl.getRootHome(s));
			userNode.setProperty(WasabiNodeProperty.HOME_ROOM, homeRoomNode);
			setStartRoom(userNode, homeRoomNode);
			Node callerPrincipalNode = UserServiceImpl.getUserByName(callerPrincipal, s);

			// Database
			WasabiUserSQL.SqlQueryForCreate(name, password);
			// Rights
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiUserACL.ACLEntryForCreate(userNode, homeRoomNode, callerPrincipalNode, callerPrincipal, s);

			return userNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "user", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

		// TODO environment
		// TODO wasabi group
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
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getHomeRoom(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.START_ROOM).getNode();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getMemberships(Node user) {
		// TODO Auto-generated method stub
		return null;
	}

	public static String getPassword(Node userNode) throws UnexpectedInternalProblemException {
		return WasabiUserSQL.SqlQueryForGetPassword(userNode);
	}

	public static Node getStartRoom(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.START_ROOM).getNode();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean getStatus(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.ACTIVE).getBoolean();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getUserByName(String userName, Session s) throws UnexpectedInternalProblemException {
		if (userName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		try {
			Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			return rootOfUsersNode.getNode(userName);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getUserByName(WasabiRoomDTO room, String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getUsers(WasabiRoomDTO room) {
		// TODO Auto-generated method stub
		return null;
	}

	public static NodeIterator getUsersByDisplayName(String displayName, Session s)
			throws UnexpectedInternalProblemException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"display-name"));
		}
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.WASABI_USER, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", WasabiNodeProperty.DISPLAY_NAME),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(displayName)));
			
			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);
			
			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void move(Node user, WasabiRoomDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	public static void remove(Node userNode) throws UnexpectedInternalProblemException {
		// Database
		WasabiUserSQL.SqlQueryForRemove(userNode);
		
		// JCR
		ObjectServiceImpl.remove(getHomeRoom(userNode));
		ObjectServiceImpl.remove(userNode);

		// TODO Group memberships?
		// TODO Environment of user?
	}

	public static void rename(Node userNode, String name) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		// Database
		WasabiUserSQL.SqlQueryForRename(userNode, name);
		
		// JCR
		ObjectServiceImpl.rename(userNode, name);
	}

	public static void setDisplayName(Node userNode, String displayName) throws UnexpectedInternalProblemException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"displayname"));
		}
		try {
			userNode.setProperty(WasabiNodeProperty.DISPLAY_NAME, displayName);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setPassword(Node userNode, String password) throws UnexpectedInternalProblemException {
		if (password == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"displayname"));
		}
		WasabiUserSQL.SqlQueryForSetPassword(userNode, password);
	}

	public static void setStartRoom(Node userNode, Node roomNode) throws UnexpectedInternalProblemException {
		try {
			userNode.setProperty(WasabiNodeProperty.START_ROOM, roomNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setStatus(Node userNode, boolean active) throws UnexpectedInternalProblemException {
		try {
			userNode.setProperty(WasabiNodeProperty.ACTIVE, active);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
