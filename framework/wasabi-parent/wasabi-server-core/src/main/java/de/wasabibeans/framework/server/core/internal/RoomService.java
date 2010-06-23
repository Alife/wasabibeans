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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;

/**
 * Class, that implements the internal access on WasabiRoom objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "RoomService")
public class RoomService extends ObjectService implements RoomServiceLocal, RoomServiceRemote {

	@Override
	public WasabiRoomDTO create(String name, WasabiRoomDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getEnvironment(WasabiRoomDTO room) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO environment, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByCreator(WasabiUserDTO creator, WasabiRoomDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModificationDate(WasabiRoomDTO environment, Date startDate, Date endDate,
			int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiRoomDTO> getRoomsByModifier(WasabiUserDTO modifier, WasabiRoomDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getRootHome() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiRoomDTO getRootRoom() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiRoomDTO room) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiRoomDTO room, String name) {
		// TODO Auto-generated method stub

	}

}
