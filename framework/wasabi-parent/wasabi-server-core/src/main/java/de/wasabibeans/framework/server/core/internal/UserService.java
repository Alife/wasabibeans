/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.internal;

import java.sql.SQLException;

import java.util.Vector;

import javax.ejb.Stateless;

import org.apache.commons.dbutils.QueryRunner;
import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;

/**
 * Class, that implements the internal access on WasabiUser objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "UserService")
public class UserService extends ObjectService implements UserServiceLocal, UserServiceRemote {

	@Override
	public WasabiUserDTO create(String name, String password) {
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
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public String getPassword(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
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
	public void remove(WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiUserDTO user, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDisplayName(WasabiUserDTO user, String displayName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPassword(WasabiUserDTO user, String password) throws SQLException {
		
		QueryRunner run = new QueryRunner(SqlConnector.connect());
		
		String wasabiUser = getName(user);
		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String setPasswordQuery = "UPDATE wasabi_user SET password=? WHERE user=?";
		
		try {
			run.update(setPasswordQuery, passwordCrypt, wasabiUser);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new SQLException(WasabiExceptionMessages.INTERNAL_NO_USER + ": "+ e.toString());
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