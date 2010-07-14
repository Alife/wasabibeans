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

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiUser;

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
			Node callerPrincipalNode = UserServiceImpl.getUserByName(callerPrincipal,s);
			// Database
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
			String insertUserQuery = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

			run.update(insertUserQuery, name, passwordCrypt);

			// Rights for user and environment
			int[] rights = { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.INSERT,
					WasabiPermission.WRITE, WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.GRANT };
			boolean[] allow = { true, true, true, true, true, true, true };

			ACLServiceImpl.create(userNode, userNode, rights, allow, 0, 0);
			ACLServiceImpl.create(homeRoomNode, userNode, rights, allow, 0, 0);
			
			//TODO: deactivate inheritance; check if callerPrincipal is in admin group
			if (callerPrincipalNode != userNode && !callerPrincipal.equals("root"))
				ACLServiceImpl.create(userNode, callerPrincipalNode, rights, allow, 0, 0);

			return userNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "user", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		
		// TODO environment
		// TODO wasabi group
	}

	public static Vector<Node> getAllUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	public static String getDisplayName(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.DISPLAY_NAME).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static WasabiRoomDTO getEnvironment(Node user) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Node getHomeRoom(Node userNode) throws UnexpectedInternalProblemException {
		try {
			return userNode.getProperty(WasabiNodeProperty.START_ROOM).getNode();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Vector<WasabiGroupDTO> getMemberships(Node user) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String getPassword(Node userNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String getPasswordQuery = "SELECT password FROM wasabi_user WHERE username=?";
		try {
			ResultSetHandler<List<WasabiUser>> h = new BeanListHandler(WasabiUser.class);

			List<WasabiUser> result = run.query(getPasswordQuery, h, wasabiUser);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
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

	public static Vector<Node> getUsersByDisplayName(String displayName) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void move(Node user, WasabiRoomDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	public static void remove(Node userNode) throws UnexpectedInternalProblemException {
		// JCR
		ObjectServiceImpl.remove(getHomeRoom(userNode));
		ObjectServiceImpl.remove(userNode);

		// Database
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String removeUserQuery = "DELETE FROM wasabi_user WHERE username=?";

		try {
			run.update(removeUserQuery, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

		// TODO Group memberships?
		// TODO Environment of user?
	}

	public static void rename(Node userNode, String name) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		// JCR
		ObjectServiceImpl.rename(userNode, name);

		// Database
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String renameUserQuery = "UPDATE wasabi_user SET username=? WHERE username=?";

		try {
			run.update(renameUserQuery, name, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
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
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String setPasswordQuery = "UPDATE wasabi_user SET password=? WHERE username=?";

		try {
			run.update(setPasswordQuery, passwordCrypt, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_USER, e);
		}
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
