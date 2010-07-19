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

import java.util.Vector;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;

/**
 * Class, that implements the internal access on WasabiUser objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "UserService")
public class UserService extends ObjectService implements UserServiceLocal, UserServiceRemote {

	@Resource
	private SessionContext sessionContext;

	@Override
	public WasabiUserDTO create(String name, String password) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		Session s = getJCRSession();
		try {
			Node userNode = UserServiceImpl.create(name, password, s, getCurrentUser());
			s.save();
			return TransferManager.convertNode2DTO(userNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public Vector<WasabiUserDTO> getAllUsers() throws UnexpectedInternalProblemException {
		Session s = getJCRSession();
		try {
			Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
			NodeIterator ni = UserServiceImpl.getAllUsers(s);
			while (ni.hasNext()) {
				users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return users;
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public String getCurrentUser() {
		return sessionContext.getCallerPrincipal().getName();
	}

	@Override
	public String getDisplayName(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			return UserServiceImpl.getDisplayName(userNode);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public WasabiRoomDTO getHomeRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			return TransferManager.convertNode2DTO(UserServiceImpl.getHomeRoom(userNode));
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPassword(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			return UserServiceImpl.getPassword(userNode);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public WasabiRoomDTO getStartRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			return TransferManager.convertNode2DTO(UserServiceImpl.getStartRoom(userNode));
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public boolean getStatus(WasabiUserDTO user) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			return UserServiceImpl.getStatus(userNode);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public WasabiUserDTO getUserByName(String userName) throws UnexpectedInternalProblemException {
		Session s = getJCRSession();
		try {
			return TransferManager.convertNode2DTO(UserServiceImpl.getUserByName(userName, s));
		} finally {
			cleanJCRSession(s);
		}
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
	public Vector<WasabiUserDTO> getUsersByDisplayName(String displayName) throws UnexpectedInternalProblemException {
		Session s = getJCRSession();
		try {
			Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
			NodeIterator ni = UserServiceImpl.getUsersByDisplayName(displayName, s);
			while (ni.hasNext()) {
				users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return users;
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public void move(WasabiUserDTO user, WasabiRoomDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDisplayName(WasabiUserDTO user, String displayName) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			UserServiceImpl.setDisplayName(userNode, displayName);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public void setPassword(WasabiUserDTO user, String password) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			UserServiceImpl.setPassword(userNode, password);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		try {
			UserServiceImpl.setStartRoom(userNode, roomNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public void setStatus(WasabiUserDTO user, boolean active) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			UserServiceImpl.setStatus(userNode, active);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}

	}

	@Override
	public void remove(WasabiUserDTO user) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			UserServiceImpl.remove(userNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public void rename(WasabiUserDTO user, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			UserServiceImpl.rename(userNode, name);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			cleanJCRSession(s);
		}
	}

	@Override
	public WasabiRoomDTO getEnvironment(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		// TODO Auto-generated method stub
		return null;
	}
}