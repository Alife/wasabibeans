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

package de.wasabibeans.framework.server.core.remote;

import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;

/**
 * Interface, that defines the remote access on WasabiRoomDTO objects.
 */
@Remote
public interface RoomServiceRemote extends ObjectServiceRemote {

	public WasabiRoomDTO create(String name, WasabiRoomDTO environment);

	public WasabiRoomDTO getEnvironment(WasabiRoomDTO room);

	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name);

	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room);

	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO environment, int depth);

	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate);

	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth);

	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator);

	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator, WasabiRoomDTO environment);

	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate);

	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth);

	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier);

	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier, WasabiRoomDTO environment);

	public WasabiRoomDTO getRootHome();

	public WasabiRoomDTO getRootRoom();

	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment);

	public void remove(WasabiRoomDTO room);

	public void rename(WasabiRoomDTO room, String name);

}
