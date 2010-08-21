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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;

/**
 * Class, that implements the internal access on WasabiUser objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "UserService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class UserService extends ObjectService implements UserServiceLocal, UserServiceRemote {

	@Override
	public WasabiUserDTO create(String name, String password) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		if (password == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"password"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node userNode = UserServiceImpl.create(name, password, s, getCurrentUser());
			s.save();
			return TransferManager.convertNode2DTO(userNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiUserDTO> getAllUsers() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
			NodeIterator ni = UserServiceImpl.getAllUsers(s);
			while (ni.hasNext()) {
				users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return users;
		} finally {
			s.logout();
		}
	}

	@Override
	public String getCurrentUser() {
		return ctx.getCallerPrincipal().getName();
	}

	@Override
	public WasabiValueDTO getDisplayName(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			long version = ObjectServiceImpl.getVersion(userNode);
			return TransferManager.convertValue2DTO(UserServiceImpl.getDisplayName(userNode), version);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiValueDTO getHomeRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Long version = ObjectServiceImpl.getVersion(userNode);
			return TransferManager.convertValue2DTO(UserServiceImpl.getHomeRoom(userNode), version);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Vector<WasabiGroupDTO> memberships = new Vector<WasabiGroupDTO>();
			NodeIterator ni = UserServiceImpl.getMemberships(userNode);
			while (ni.hasNext()) {
				Node groupRef = ni.nextNode();
				Node group = null;
				try {
					group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					groupRef.remove();
				}
				if (group != null) {
					memberships.add((WasabiGroupDTO) TransferManager.convertNode2DTO(group));
				}
			}
			return memberships;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public String getPassword(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			return UserServiceImpl.getPassword(userNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiValueDTO getStartRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Long version = ObjectServiceImpl.getVersion(userNode);
			return TransferManager.convertValue2DTO(UserServiceImpl.getStartRoom(userNode), version);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiValueDTO getStatus(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Long version = ObjectServiceImpl.getVersion(userNode);
			return TransferManager.convertValue2DTO(UserServiceImpl.getStatus(userNode), version);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiUserDTO getUserByName(String userName) throws UnexpectedInternalProblemException {
		if (userName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			return TransferManager.convertNode2DTO(UserServiceImpl.getUserByName(userName, s));
		} finally {
			s.logout();
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
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"display-name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
			NodeIterator ni = UserServiceImpl.getUsersByDisplayName(displayName, s);
			while (ni.hasNext()) {
				users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return users;
		} finally {
			s.logout();
		}
	}

	@Override
	public void setDisplayName(WasabiUserDTO user, String displayName, Long version)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"displayname"));
		}

		Node userNode = null;
		Session s = jcr.getJCRSession();
		try {
			userNode = TransferManager.convertDTO2Node(user, s);
			writeAccessCheck(userNode, user, version, s);
			UserServiceImpl.setDisplayName(userNode, displayName, getCurrentUser());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			writeAccessRelease(userNode, user, s);
			s.logout();
		}
	}

	@Override
	public void setPassword(WasabiUserDTO user, String password) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		if (password == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"displayname"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			UserServiceImpl.setPassword(userNode, password);
		} finally {
			s.logout();
		}
	}

	@Override
	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room, Long version)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException {
		Node userNode = null;
		Session s = jcr.getJCRSession();
		try {
			userNode = TransferManager.convertDTO2Node(user, s);
			Node roomNode = TransferManager.convertDTO2Node(room, s);
			writeAccessCheck(userNode, user, version, s);
			UserServiceImpl.setStartRoom(userNode, roomNode, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			writeAccessRelease(userNode, user, s);
			s.logout();
		}
	}

	@Override
	public void setStatus(WasabiUserDTO user, boolean active, Long version) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		Node userNode = null;
		Session s = jcr.getJCRSession();
		try {
			userNode = TransferManager.convertDTO2Node(user, s);
			writeAccessCheck(userNode, user, version, s);
			UserServiceImpl.setStatus(userNode, active, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			writeAccessRelease(userNode, user, s);
			s.logout();
		}
	}

	@Override
	public void remove(WasabiUserDTO user) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			UserServiceImpl.remove(userNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public void rename(WasabiUserDTO user, String name, Long version) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node userNode = null;
		Session s = jcr.getJCRSession();
		try {
			userNode = TransferManager.convertDTO2Node(user, s);
			writeAccessCheck(userNode, user, version, s);
			UserServiceImpl.rename(userNode, name, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			writeAccessRelease(userNode, user, s);
			s.logout();
		}
	}
}