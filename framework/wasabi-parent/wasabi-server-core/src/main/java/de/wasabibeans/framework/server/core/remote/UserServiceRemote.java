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

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiUserDTO objects.
 */
@Remote
public interface UserServiceRemote extends ObjectServiceRemote {

	public WasabiUserDTO create(String name, String password) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ConcurrentModificationException;

	public Vector<WasabiUserDTO> getAllUsers() throws UnexpectedInternalProblemException;

	public String getCurrentUser() throws UnexpectedInternalProblemException;

	public WasabiValueDTO getDisplayName(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getHomeRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException;

	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public String getPassword(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getStartRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException;

	public WasabiValueDTO getStatus(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiUserDTO getUserByName(String userName) throws UnexpectedInternalProblemException;

	public WasabiUserDTO getUserByName(WasabiRoomDTO room, String userName) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiUserDTO> getUsers(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiUserDTO> getUsersByDisplayName(String displayName) throws UnexpectedInternalProblemException;

	public void remove(WasabiUserDTO user) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException;

	public void enter(WasabiUserDTO user, WasabiRoomDTO room) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException;

	public void leave(WasabiUserDTO user, WasabiRoomDTO room) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException;

	public Vector<WasabiRoomDTO> getWhereabouts(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void rename(WasabiUserDTO user, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, ConcurrentModificationException;

	public void setDisplayName(WasabiUserDTO user, String displayName, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException;

	public void setPassword(WasabiUserDTO user, String password) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException;

	public void setStatus(WasabiUserDTO user, boolean active, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException;
}
