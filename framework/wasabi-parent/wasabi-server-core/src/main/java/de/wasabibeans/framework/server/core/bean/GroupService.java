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
import de.wasabibeans.framework.server.core.authorization.WasabiGroupACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.GroupServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.GroupServiceRemote;

/**
 * Class, that implements the internal access on WasabiGroup objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "GroupService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class GroupService extends ObjectService implements GroupServiceLocal, GroupServiceRemote {

	@Override
	public void addMember(WasabiGroupDTO group, WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String userName = ObjectServiceImpl.getName(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				// case: semi-free groups
				if (!callerPrincipal.equals(userName)) {
					if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
							WasabiPermission.WRITE, WasabiPermission.GRANT }, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.addMember()",
								"INSERT, WRITE or GRANT", "group"));
					if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
							WasabiPermission.WRITE }, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.addMember()",
								"INSERT or WRITE", "user"));
				}
				// case: free groups and private groups
				else if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.EXECUTE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.addMember()", "EXECUTE",
							"group"));
		}
		/* Authorization - End */

		GroupServiceImpl.addMember(groupNode, userNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		EventCreator.createMembershipEvent(groupNode, userNode, true, jms, callerPrincipal);
	}

	@Override
	public WasabiGroupDTO create(String name, WasabiGroupDTO parentGroup) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		if (GroupServiceImpl.getGroupByName(name, s) != null) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name));
		}

		Node parentGroupNode = null;
		if (parentGroup != null) {
			parentGroupNode = TransferManager.convertDTO2Node(parentGroup, s);
		}

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (parentGroup == null) {
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					throw new NoPermissionException(WasabiExceptionMessages
							.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_ADMIN));
			} else if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(parentGroupNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
						WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.create()",
							"INSERT or WRITE", "parentGroup"));
		}
		/* Authorization - End */

		Node groupNode = GroupServiceImpl.create(name, parentGroupNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createCreatedEvent(groupNode, parentGroupNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(groupNode, parentGroup);
	}

	@Override
	public Vector<WasabiGroupDTO> getAllGroups() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Vector<WasabiGroupDTO> allGroups = new Vector<WasabiGroupDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		NodeIterator ni = GroupServiceImpl.getAllGroups(s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				while (ni.hasNext()) {
					Node groupNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
						allGroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(groupNode));
				}
			} else
				while (ni.hasNext())
					allGroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		/* Authorization - End */
		else
			while (ni.hasNext())
				allGroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return allGroups;
	}

	@Override
	public Vector<WasabiUserDTO> getAllMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE) {
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.READ, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getAllMembers()",
								"READ", "group"));
			}
			/* Authorization - End */

			Vector<WasabiUserDTO> allMembers = new Vector<WasabiUserDTO>();
			for (NodeIterator ni : GroupServiceImpl.getAllMembers(groupNode)) {
				while (ni.hasNext()) {
					Node userRef = ni.nextNode();
					Node user = null;
					try {
						user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
					} catch (ItemNotFoundException infe) {
						try {
							userRef.remove();
							s.save();
						} catch (RepositoryException re) {
							/*
							 * do nothing -> remove failed -> reference already removed by another thread concurrently
							 * or currently locked
							 */
						}
					}
					if (user != null) {
						WasabiUserDTO userDTO = (WasabiUserDTO) TransferManager.convertNode2DTO(user);
						if (!allMembers.contains(userDTO)) {
							/* Authorization - Begin */
							if (WasabiConstants.ACL_CHECK_ENABLE) {
								if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
									if (WasabiAuthorizer.authorize(user, callerPrincipal, WasabiPermission.VIEW, s))
										allMembers.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
								} else
									allMembers.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
							}
							/* Authorization - End */
							else
								allMembers.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
						}
					}
				}
			}

			return allMembers;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiValueDTO getDisplayName(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getDisplayName()",
							"VIEW", "group"));
		}
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(groupNode);
		return TransferManager.convertValue2DTO(GroupServiceImpl.getDisplayName(groupNode), optLockId);
	}

	@Override
	public WasabiGroupDTO getGroupByName(String groupName) throws UnexpectedInternalProblemException,
			NoPermissionException {
		if (groupName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node groupNode = GroupServiceImpl.getGroupByName(groupName, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
							"GroupService.getGroupByName()", "VIEW", "group"));
		}
		/* Authorization - End */

		return TransferManager.convertNode2DTO(groupNode);
	}

	@Override
	public Vector<WasabiGroupDTO> getGroupsByDisplayName(String displayName) throws UnexpectedInternalProblemException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"display-name"));
		}

		Session s = jcr.getJCRSession();
		Vector<WasabiGroupDTO> groups = new Vector<WasabiGroupDTO>();
		NodeIterator ni = GroupServiceImpl.getGroupsByDisplayName(displayName, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				while (ni.hasNext()) {
					Node groupNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
						groups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(groupNode));
				}
			} else
				while (ni.hasNext())
					groups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		/* Authorization - End */
		else
			while (ni.hasNext())
				groups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return groups;
	}

	@Override
	public WasabiUserDTO getMemberByName(WasabiGroupDTO group, String userName) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		if (userName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node userNode = GroupServiceImpl.getMemberByName(groupNode, userName);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.READ, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getMemberByName()",
							"READ", "group"));
				if (WasabiConstants.ACL_CHECK_ENABLE)
					if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
								"GroupService.getMemberByName()", "VIEW"));
			}
		/* Authorization - End */

		return TransferManager.convertNode2DTO(userNode);
	}

	@Override
	public Vector<WasabiUserDTO> getMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Vector<WasabiUserDTO> members = new Vector<WasabiUserDTO>();
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.READ, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getMembers()",
								"READ", "group"));
			/* Authorization - End */

			NodeIterator ni = GroupServiceImpl.getMembers(groupNode);
			while (ni.hasNext()) {
				Node userRef = ni.nextNode();
				Node user = null;
				try {
					user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					try {
						userRef.remove();
						s.save();
					} catch (RepositoryException re) {
						/*
						 * do nothing -> remove failed -> reference already removed by another thread concurrently or
						 * currently locked
						 */
					}
				}
				if (user != null) {

					/* Authorization - Begin */
					if (WasabiConstants.ACL_CHECK_ENABLE) {
						if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
							if (WasabiAuthorizer.authorize(user, callerPrincipal, WasabiPermission.VIEW, s))
								members.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
						} else
							members.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
					}
					/* Authorization - End */
					else
						members.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
				}
			}
			return members;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiValueDTO getParentGroup(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getParentGroup()",
							"VIEW", "group"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(groupNode);
		Node parentGroupNode = GroupServiceImpl.getParentGroup(groupNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(parentGroupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
							"GroupService.getParentGroup()", "VIEW"));
		/* Authorization - End */

		return TransferManager.convertValue2DTO(parentGroupNode, optLockId);
	}

	@Override
	public WasabiGroupDTO getSubGroupByName(WasabiGroupDTO group, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getSubGroupByName()",
							"VIEW", "group"));
		/* Authorization - End */

		Node subGroupNode = GroupServiceImpl.getSubGroupByName(groupNode, name);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(subGroupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
							"GroupService.getSubGroupByName()", "VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(subGroupNode, group);
	}

	@Override
	public Vector<WasabiGroupDTO> getSubGroups(WasabiGroupDTO group) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Vector<WasabiGroupDTO> subgroups = new Vector<WasabiGroupDTO>();
		NodeIterator ni = GroupServiceImpl.getSubGroups(groupNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s)) {
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.getSubGroups()", "VIEW",
							"group"));
				}

				while (ni.hasNext()) {
					Node subgroupNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(subgroupNode, callerPrincipal, WasabiPermission.VIEW, s))
						subgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(subgroupNode, group));
				}
			} else
				while (ni.hasNext())
					subgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode(), group));
		}

		/* Authorization - End */
		else
			while (ni.hasNext())
				subgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode(), group));

		return subgroups;
	}

	@Override
	public Vector<WasabiGroupDTO> getTopLevelGroups() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Vector<WasabiGroupDTO> topgroups = new Vector<WasabiGroupDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		NodeIterator ni = GroupServiceImpl.getTopLevelGroups(s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				while (ni.hasNext()) {
					Node topLvlGroups = ni.nextNode();
					if (WasabiAuthorizer.authorize(topLvlGroups, callerPrincipal, WasabiPermission.VIEW, s))
						topgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(topLvlGroups));
				}
			} else
				while (ni.hasNext())
					topgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		/* Authorization - End */
		else
			while (ni.hasNext())
				topgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return topgroups;
	}

	@Override
	public boolean isDirectMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.isDirectMember()",
							"VIEW", "group"));

				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.isDirectMember()",
							"VIEW", "user"));
			}
		/* Authorization - End */

		return GroupServiceImpl.isDirectMember(groupNode, userNode);
	}

	@Override
	public boolean isMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.isMember()", "VIEW",
							"group"));
				if (!WasabiAuthorizer.authorize(userNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.isMember()", "VIEW",
							"user"));
			}
		/* Authorization - End */

		return GroupServiceImpl.isMember(groupNode, userNode);
	}

	@Override
	public void move(WasabiGroupDTO group, WasabiGroupDTO newParentGroup, Long optLockId)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException,
			NoPermissionException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node newParentGroupNode = null;
		if (newParentGroup != null) {
			newParentGroupNode = TransferManager.convertDTO2Node(newParentGroup, s);
		}
		Node groupNode = TransferManager.convertDTO2Node(group, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (newParentGroupNode == null)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					throw new NoPermissionException(WasabiExceptionMessages
							.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_ADMIN));
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(newParentGroupNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.move()", "WRITE",
							"newParentGroup"));
			}
		}
		/* Authorization - End */

		Locker.checkOptLockId(groupNode, group, optLockId);
		GroupServiceImpl.move(groupNode, newParentGroupNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createMovedEvent(groupNode, newParentGroupNode, jms, callerPrincipal);
	}

	@Override
	public void remove(WasabiGroupDTO group, Long optLockId) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		Locker.checkOptLockId(groupNode, group, optLockId);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.remove()", "WRITE",
							"group"));
				else
					WasabiGroupACL
							.remove(groupNode, callerPrincipal, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms);
			} else
				GroupServiceImpl.remove(groupNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms, callerPrincipal);
		}
		/* Authorization - End */
		else {
			GroupServiceImpl.remove(groupNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms, callerPrincipal);
		}

	}

	@Override
	public void removeMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node groupNode = TransferManager.convertDTO2Node(group, s);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		String userName = ObjectServiceImpl.getName(userNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!callerPrincipal.equals(userName))
					if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, new int[] { WasabiPermission.WRITE,
							WasabiPermission.GRANT }, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.removeMember()",
								"WRITE or GRANT", "group"));
		/* Authorization - End */

		GroupServiceImpl.removeMember(groupNode, userNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		EventCreator.createMembershipEvent(groupNode, userNode, false, jms, callerPrincipal);

	}

	@Override
	public void rename(WasabiGroupDTO group, String name, Long optLockId) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node groupNode = TransferManager.convertDTO2Node(group, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.rename()", "WRITE",
							"group"));
		/* Authorization - End */

		Locker.checkOptLockId(groupNode, group, optLockId);
		GroupServiceImpl.rename(groupNode, name, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(groupNode, WasabiProperty.NAME, name, jms, callerPrincipal);
	}

	@Override
	public void setDisplayName(WasabiGroupDTO group, String displayName, Long optLockId)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException,
			NoPermissionException {
		if (displayName == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"displayname"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node groupNode = TransferManager.convertDTO2Node(group, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(groupNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "GroupService.setDisplayName()",
							"WRITE", "group"));
		/* Authorization - End */

		Locker.checkOptLockId(groupNode, group, optLockId);
		GroupServiceImpl
				.setDisplayName(groupNode, displayName, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(groupNode, WasabiProperty.NAME, displayName, jms, callerPrincipal);
	}
}
