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

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiUser;

/**
 * Class, that implements the internal access on WasabiUser objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "UserService")
public class UserService extends ObjectService implements UserServiceLocal, UserServiceRemote {

	@Resource
	private SessionContext sessionContext;

	@Override
	public WasabiUserDTO create(String name, String password) throws SQLException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String insertUserQuery = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

		try {
			run.update(insertUserQuery, name, passwordCrypt);
		} catch (SQLException e) {
			throw new SQLException(e.toString());
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiUserDTO> getAllUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentUser() {
		return sessionContext.getCallerPrincipal().getName();
	}

	@Override
	public String getDisplayName(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getEnvironment(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getHomeRoom(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getPassword(WasabiUserDTO user) throws SQLException, UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = getName(user);
		String getPasswordQuery = "SELECT password FROM wasabi_user WHERE username=?";
		try {
			ResultSetHandler<List<WasabiUser>> h = new BeanListHandler(WasabiUser.class);

			List<WasabiUser> result = run.query(getPasswordQuery, h, wasabiUser);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (SQLException e) {
			throw new SQLException(e.toString());
		}
	}

	@Override
	public WasabiRoomDTO getStartRoom(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getStatus(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WasabiUserDTO getUserByName(String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiUserDTO getUserByName(WasabiRoomDTO room, String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiUserDTO> getUsers(WasabiRoomDTO room) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiUserDTO> getUsersByDisplayName(String displayName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(WasabiUserDTO user, WasabiRoomDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiUserDTO user) throws SQLException, UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = getName(user);
		String removeUserQuery = "DELETE FROM wasabi_user WHERE username=?";

		try {
			run.update(removeUserQuery, wasabiUser);
		} catch (SQLException e) {
			throw new SQLException(e.toString());
		}
	}

	@Override
	public void rename(WasabiUserDTO user, String name) throws SQLException, UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = getName(user);
		String renameUserQuery = "UPDATE wasabi_user SET username=? WHERE username=?";

		try {
			run.update(renameUserQuery, name, wasabiUser);
		} catch (SQLException e) {
			throw new SQLException(e.toString());
		}
	}

	@Override
	public void setDisplayName(WasabiUserDTO user, String displayName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPassword(WasabiUserDTO user, String password) throws SQLException,
			UnexpectedInternalProblemException, ObjectDoesNotExistException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = getName(user);
		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String setPasswordQuery = "UPDATE wasabi_user SET password=? WHERE username=?";

		try {
			run.update(setPasswordQuery, passwordCrypt, wasabiUser);
		} catch (SQLException e) {
			throw new SQLException(WasabiExceptionMessages.INTERNAL_NO_USER + ": " + e.toString());
		}
	}

	@Override
	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(WasabiUserDTO user, boolean active) {
		// TODO Auto-generated method stub

	}

}