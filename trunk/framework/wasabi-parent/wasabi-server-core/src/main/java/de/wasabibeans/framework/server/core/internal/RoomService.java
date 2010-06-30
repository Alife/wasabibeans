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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
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
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		if (environment == null && !name.equals(WasabiConstants.ROOT_ROOM_NAME)) {
			logger.error(WasabiExceptionMessages.INTERNAL_ENVIRONMENT_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_ENVIRONMENT_NULL);
		}
		Session s = getJCRSession();
		Node environmentNode = convertDTO2Node(environment, s);
		try {
			Node roomNode = environmentNode.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.WASABI_ROOM);
			s.save();

			return convertNode2DTO(roomNode);
		} catch (ItemExistsException iee) {
			String msg = "Room " + name + " already exists.";
			logger.warn(msg, iee);
			throw new ObjectAlreadyExistsException(msg, name);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public WasabiRoomDTO getEnvironment(WasabiRoomDTO room) {
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		try {
			return convertNode2DTO(roomNode.getParent().getParent());
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public WasabiRoomDTO getRoomByName(WasabiRoomDTO room, String name) {
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		try {
			return convertNode2DTO(roomNode.getNode(WasabiNodeProperty.ROOMS + "/" + name));
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public Vector<WasabiRoomDTO> getRooms(WasabiRoomDTO room) {
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		try {
			Vector<WasabiRoomDTO> rooms = new Vector<WasabiRoomDTO>();
			NodeIterator iter = roomNode.getNode(WasabiNodeProperty.ROOMS).getNodes();
			while (iter.hasNext()) {
				rooms.add((WasabiRoomDTO) convertNode2DTO(iter.nextNode()));
			}
			return rooms;
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
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
		Session s = getJCRSession();
		try {
			Node roomNode = s.getRootNode().getNode(
					WasabiConstants.ROOT_ROOM_NAME + "/" + WasabiNodeProperty.ROOMS + "/"
							+ WasabiConstants.HOME_ROOM_NAME);
			return convertNode2DTO(roomNode);
		} catch (PathNotFoundException pnfe) {
			logger.warn(WasabiExceptionMessages.INTERNAL_NO_HOME_ROOM);
			throw new RuntimeException(WasabiExceptionMessages.INTERNAL_NO_HOME_ROOM);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public WasabiRoomDTO getRootRoom() {
		Session s = getJCRSession();
		try {
			Node roomNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			return convertNode2DTO(roomNode);
		} catch (PathNotFoundException pnfe) {
			logger.warn(WasabiExceptionMessages.INTERNAL_NO_ROOT_ROOM);
			throw new RuntimeException(WasabiExceptionMessages.INTERNAL_NO_ROOT_ROOM);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public void move(WasabiRoomDTO room, WasabiRoomDTO newEnvironment) {
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		Node newEnvironmentNode = convertDTO2Node(newEnvironment, s);
		try {
			s.move(roomNode.getPath(), newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.ROOMS + "/"
					+ roomNode.getName());
			s.save();
		} catch (ItemExistsException iee) {
			try {
				String msg = "Room " + roomNode.getName() + " already exists.";
				logger.warn(msg, iee);
				throw new ObjectAlreadyExistsException(msg, roomNode.getName());
			} catch (RepositoryException re) {
				logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
				throw new RuntimeException(re);
			}
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public void remove(WasabiRoomDTO room) {
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		try {
			roomNode.remove();
			s.save();
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@Override
	public void rename(WasabiRoomDTO room, String name) {
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		Session s = getJCRSession();
		Node roomNode = convertDTO2Node(room, s);
		try {
			s.move(roomNode.getPath(), roomNode.getParent().getPath() + "/" + name);
			s.save();
		} catch (ItemExistsException iee) {
			String msg = "Room " + name + " already exists.";
			logger.warn(msg, iee);
			throw new ObjectAlreadyExistsException(msg, name);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

}
