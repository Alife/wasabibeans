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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiRoomDTO objects.
 */
@Remote
public interface RoomServiceRemote extends ObjectServiceRemote {

	public WasabiRoomDTO create(String name, WasabiRoomDTO environment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, NoPermissionException;

	public WasabiRoomDTO getEnvironment(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO environment, int depth)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator, WasabiRoomDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier, WasabiRoomDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public WasabiRoomDTO getRootHome() throws UnexpectedInternalProblemException;

	public WasabiRoomDTO getRootRoom() throws UnexpectedInternalProblemException;

	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException;

	public void remove(WasabiRoomDTO room) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void rename(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException;

}
