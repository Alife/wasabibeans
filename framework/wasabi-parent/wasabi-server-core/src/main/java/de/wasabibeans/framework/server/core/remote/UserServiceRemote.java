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

package de.wasabibeans.framework.server.core.remote;

import java.sql.SQLException;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiUserDTO objects.
 */
@Remote
public interface UserServiceRemote extends ObjectServiceRemote {

	public WasabiUserDTO create(String name, String password);

	public Vector<WasabiUserDTO> getAllUsers();

	public String getCurrentUser();

	public String getDisplayName(WasabiUserDTO user);

	public WasabiRoomDTO getEnvironment(WasabiUserDTO user);

	public WasabiRoomDTO getHomeRoom(WasabiUserDTO user);

	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user);

	public String getPassword(WasabiUserDTO user) throws SQLException, UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public WasabiRoomDTO getStartRoom(WasabiUserDTO user);

	public boolean getStatus(WasabiUserDTO user);

	public WasabiUserDTO getUserByName(String userName);

	public WasabiUserDTO getUserByName(WasabiRoomDTO room, String userName);

	public Vector<WasabiUserDTO> getUsers(WasabiRoomDTO room);

	public Vector<WasabiUserDTO> getUsersByDisplayName(String displayName);

	public void move(WasabiUserDTO user, WasabiRoomDTO newEnvironment);

	public void remove(WasabiUserDTO user);

	public void rename(WasabiUserDTO user, String name);

	public void setDisplayName(WasabiUserDTO user, String displayName);

	public void setPassword(WasabiUserDTO user, String password) throws SQLException, UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room);

	public void setStatus(WasabiUserDTO user, boolean active);
}
