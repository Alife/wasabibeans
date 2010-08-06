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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiRoomtACL;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;

/**
 * Class, that implements the internal access on WasabiRoom objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "RoomService")
public class RoomService extends ObjectService implements RoomServiceLocal, RoomServiceRemote {

	@Override
	public WasabiRoomDTO create(String name, WasabiRoomDTO environment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession(ctx);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		try {
			Node roomNode = RoomServiceImpl.create(name, environmentNode);
			s.save();
			WasabiRoomtACL.ACLEntryForCreate(roomNode, s);
			return TransferManager.convertNode2DTO(roomNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiRoomDTO getEnvironment(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		return TransferManager.convertNode2DTO(RoomServiceImpl.getEnvironment(roomNode));
	}

	@Override
	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRoomByName(roomNode, name));
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();
		NodeIterator ni = RoomServiceImpl.getRooms(roomNode);
		while (ni.hasNext()) {
			rooms.add((WasabiRoomDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		return rooms;
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
	public WasabiRoomDTO getRootHome() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession(ctx);
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootHome(s));
	}

	@Override
	public WasabiRoomDTO getRootRoom() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession(ctx);
		return TransferManager.convertNode2DTO(RoomServiceImpl.getRootRoom(s));
	}

	@Override
	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
		try {
			RoomServiceImpl.move(roomNode, newEnvironmentNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void remove(WasabiRoomDTO room) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		try {
			RoomServiceImpl.remove(roomNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void rename(WasabiRoomDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession(ctx);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		try {
			RoomServiceImpl.rename(roomNode, name);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
