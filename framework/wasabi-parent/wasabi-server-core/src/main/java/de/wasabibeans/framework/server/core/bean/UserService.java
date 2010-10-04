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

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;

/**
 * Class, that implements the internal access on WasabiUser objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "UserService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UserService extends ObjectService implements UserServiceLocal, UserServiceRemote {

	@Override
	public WasabiUserDTO create(String name, String password) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ConcurrentModificationException, NoPermissionException {
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
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node wasabiGroup = GroupServiceImpl.getWasabiGroup(s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE) {
				if (!WasabiAuthorizer.authorize(wasabiGroup, callerPrincipal, new int[] { WasabiPermission.INSERT,
						WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_GROUP, "UserService.create()",
							"INSERT or WRITE", WasabiConstants.WASABI_GROUP_NAME));
			}
			/* Authorization - End */

			Node userNode = UserServiceImpl.create(name, password, s, getCurrentUser());
			s.save();
			return TransferManager.convertNode2DTO(userNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void enter(WasabiUserDTO user, WasabiRoomDTO room) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Node roomNode = TransferManager.convertDTO2Node(room, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(roomNode, callerPrincipal, WasabiPermission.EXECUTE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.enter()", "EXECUTE",
							"room"));
			/* Authorization - End */

			UserServiceImpl.enter(userNode, roomNode);
			s.save();
			EventCreator.createUserMovementEvent(userNode, roomNode, true, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public Vector<WasabiUserDTO> getAllUsers() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		try {
			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE) {
				Vector<String> authorizedUsers = WasabiAuthorizer.authorizePermission(s.getRootNode(), callerPrincipal,
						WasabiPermission.VIEW, WasabiType.USER, s);
				for (String id : authorizedUsers) {
					Node user = UserServiceImpl.getUserById(id, s);
					users.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
				}
			}
			/* Authorization - End */
			else {
				NodeIterator ni = UserServiceImpl.getAllUsers(s);
				while (ni.hasNext()) {
					users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));
				}
			}

			return users;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public String getCurrentUser() {
		return ctx.getCallerPrincipal().getName();
	}

	@Override
	public WasabiValueDTO getDisplayName(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.getDisplayName()", "VIEW",
						"user"));
		/* Authorization - End */

		long optLockId = ObjectServiceImpl.getOptLockId(userNode);
		return TransferManager.convertValue2DTO(UserServiceImpl.getDisplayName(userNode), optLockId);
	}

	@Override
	public WasabiValueDTO getHomeRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		Long optLockId = ObjectServiceImpl.getOptLockId(userNode);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node roomNode = UserServiceImpl.getHomeRoom(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "UserService.getDisplayName()",
						"VIEW"));
		/* Authorization - End */

		return TransferManager.convertValue2DTO(roomNode, optLockId);
	}

	@Override
	public Vector<WasabiGroupDTO> getMemberships(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.getMemberships()",
							"VIEW", "user"));
			/* Authorization - End */

			Vector<WasabiGroupDTO> memberships = new Vector<WasabiGroupDTO>();
			NodeIterator ni = UserServiceImpl.getMemberships(userNode);
			while (ni.hasNext()) {
				Node groupRef = ni.nextNode();
				Node group = null;
				try {
					group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					try {
						groupRef.remove();
					} catch (RepositoryException re) {
						// do nothing -> remove failed -> reference already removed by another thread concurrently
					}
				}
				if (group != null) {
					memberships.add((WasabiGroupDTO) TransferManager.convertNode2DTO(group));
				}
			}

			return memberships;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public String getPassword(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.getPassword()", "WRITE",
						"user"));
		/* Authorization - End */

		return UserServiceImpl.getPassword(userNode);
	}

	@Override
	public WasabiValueDTO getStartRoom(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, TargetDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.getStartRoom()", "READ",
						"user"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(userNode);
		return TransferManager.convertValue2DTO(UserServiceImpl.getStartRoom(userNode), optLockId);
	}

	@Override
	public WasabiValueDTO getStatus(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.getStatus()", "READ", "user"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(userNode);
		return TransferManager.convertValue2DTO(UserServiceImpl.getStatus(userNode), optLockId);
	}

	@Override
	public WasabiUserDTO getUserByName(String userName) throws UnexpectedInternalProblemException,
			NoPermissionException {
		if (userName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node userNode = UserServiceImpl.getUserByName(userName, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "UserService.getStatus()", "VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(userNode);
	}

	@Override
	public WasabiUserDTO getUserByName(WasabiRoomDTO room, String userName) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (userName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node roomNode = TransferManager.convertDTO2Node(room, s);
		Node userNode = UserServiceImpl.getUserByName(roomNode, userName);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "UserService.getUserByName()",
						"VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(userNode);
	}

	@Override
	public Vector<WasabiUserDTO> getUsers(WasabiRoomDTO room) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node roomNode = TransferManager.convertDTO2Node(room, s);
			Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			NodeIterator ni = UserServiceImpl.getUsers(roomNode);
			while (ni.hasNext()) {
				Node userRef = ni.nextNode();
				Node user = null;
				try {
					user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					try {
						userRef.remove();
					} catch (RepositoryException re) {
						// do nothing -> remove failed -> reference already removed by another thread concurrently
					}
				}
				if (user != null) {

					/* Authorization - Begin */
					if (WasabiConstants.ACL_CHECK_ENABLE) {
						if (WasabiAuthorizer.authorize(user, callerPrincipal, WasabiPermission.VIEW, s))
							users.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
					}
					/* Authorization - End */
					else
						users.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
				}
			}
			return users;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public Vector<WasabiUserDTO> getUsersByDisplayName(String displayName) throws UnexpectedInternalProblemException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"display-name"));
		}

		Session s = jcr.getJCRSession();
		Vector<WasabiUserDTO> users = new Vector<WasabiUserDTO>();
		NodeIterator ni = UserServiceImpl.getUsersByDisplayName(displayName, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			while (ni.hasNext()) {
				Node userNode = ni.nextNode();
				if (WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
					users.add((WasabiUserDTO) TransferManager.convertNode2DTO(userNode));
			}
		}
		/* Authorization - End */
		else
			while (ni.hasNext())
				users.add((WasabiUserDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return users;
	}

	public Vector<WasabiRoomDTO> getWhereabouts(WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Vector<WasabiRoomDTO> whereabouts = new Vector<WasabiRoomDTO>();
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			NodeIterator ni = UserServiceImpl.getWhereabouts(userNode);

			while (ni.hasNext()) {
				Node roomRef = ni.nextNode();
				Node room = null;
				try {
					room = roomRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					try {
						roomRef.remove();
					} catch (RepositoryException re) {
						// do nothing -> remove failed -> reference already removed by another thread concurrently
					}
				}
				if (room != null) {
					/* Authorization - Begin */
					if (WasabiConstants.ACL_CHECK_ENABLE) {
						if (WasabiAuthorizer.authorize(room, callerPrincipal, WasabiPermission.VIEW, s))
							whereabouts.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room));
					}
					/* Authorization - End */
					else
						whereabouts.add((WasabiRoomDTO) TransferManager.convertNode2DTO(room));
				}
			}

			return whereabouts;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void leave(WasabiUserDTO user, WasabiRoomDTO room) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);
			Node roomNode = TransferManager.convertDTO2Node(room, s);
			UserServiceImpl.leave(userNode, roomNode);
			s.save();
			EventCreator.createUserMovementEvent(userNode, roomNode, false, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void remove(WasabiUserDTO user, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.remove()", "WRITE",
							"user"));
			/* Authorization - End */

			Locker.checkOptLockId(userNode, user, optLockId);
			EventCreator.createRemovedEvent(userNode, jms, callerPrincipal);
			UserServiceImpl.remove(userNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void rename(WasabiUserDTO user, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.rename()", "WRITE",
							"user"));
			/* Authorization - End */

			Locker.checkOptLockId(userNode, user, optLockId);
			UserServiceImpl.rename(userNode, name, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(userNode, WasabiProperty.NAME, name, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void setDisplayName(WasabiUserDTO user, String displayName, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"displayname"));
		}

		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.setDisplayName()",
							"WRITE", "user"));
			/* Authorization - End */

			Locker.checkOptLockId(userNode, user, optLockId);
			UserServiceImpl.setDisplayName(userNode, displayName, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(userNode, WasabiProperty.DISPLAY_NAME, displayName, jms,
					callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void setPassword(WasabiUserDTO user, String password) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (password == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"password"));
		}

		Session s = jcr.getJCRSession();
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.setPassword()", "WRITE",
						"user"));
		/* Authorization - End */

		UserServiceImpl.setPassword(userNode, password);
	}

	@Override
	public void setStartRoom(WasabiUserDTO user, WasabiRoomDTO room, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.setStartRoom()", "WRITE",
							"user"));
			/* Authorization - End */

			Node roomNode = TransferManager.convertDTO2Node(room, s);
			Locker.checkOptLockId(userNode, user, optLockId);
			UserServiceImpl.setStartRoom(userNode, roomNode, callerPrincipal);
			EventCreator
					.createPropertyChangedEvent(userNode, WasabiProperty.START_ROOM, roomNode, jms, callerPrincipal);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void setStatus(WasabiUserDTO user, boolean active, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node userNode = TransferManager.convertDTO2Node(user, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "UserService.setStatus()", "WRITE",
							"user"));
			/* Authorization - End */

			Locker.checkOptLockId(userNode, user, optLockId);
			UserServiceImpl.setStatus(userNode, active, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(userNode, WasabiProperty.STATUS, active, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}