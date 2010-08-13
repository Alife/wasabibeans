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
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.local.GroupServiceLocal;
import de.wasabibeans.framework.server.core.remote.GroupServiceRemote;

/**
 * Class, that implements the internal access on WasabiGroup objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "GroupService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class GroupService extends ObjectService implements GroupServiceLocal, GroupServiceRemote {

	@Override
	public void addMember(WasabiGroupDTO group, WasabiUserDTO user) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Node userNode = TransferManager.convertDTO2Node(user, s);
			GroupServiceImpl.addMember(groupNode, userNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiGroupDTO create(String name, WasabiGroupDTO parentGroup) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession();
		try {
			Node parentGroupNode = null;
			if (parentGroup != null) {
				parentGroupNode = TransferManager.convertDTO2Node(parentGroup, s);
			}
			Node newGroup = GroupServiceImpl.create(name, parentGroupNode, s);
			s.save();
			return TransferManager.convertNode2DTO(newGroup);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getAllGroups() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Vector<WasabiGroupDTO> allGroups = new Vector<WasabiGroupDTO>();
			NodeIterator ni = GroupServiceImpl.getAllGroups(s);
			while (ni.hasNext()) {
				allGroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return allGroups;
		} finally {
			s.logout();
		}
	}

	@Override
	public String getDisplayName(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			return GroupServiceImpl.getDisplayName(groupNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiGroupDTO getGroupByName(String groupName) throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = GroupServiceImpl.getGroupByName(groupName, s);
			return TransferManager.convertNode2DTO(groupNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getGroupsByDisplayName(String displayName) throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Vector<WasabiGroupDTO> groups = new Vector<WasabiGroupDTO>();
			NodeIterator ni = GroupServiceImpl.getGroupsByDisplayName(displayName, s);
			while (ni.hasNext()) {
				groups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return groups;
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiUserDTO getMemberByName(WasabiGroupDTO group, String userName) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Node userNode = GroupServiceImpl.getMemberByName(groupNode, userName);
			return TransferManager.convertNode2DTO(userNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiUserDTO> getMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Vector<WasabiUserDTO> members = new Vector<WasabiUserDTO>();
			NodeIterator ni = GroupServiceImpl.getMembers(groupNode);
			while (ni.hasNext()) {
				Node userRef = ni.nextNode();
				Node user = null;
				try {
					user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					userRef.remove();
				}
				if (user != null) {
					members.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
				}
			}
			return members;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiUserDTO> getAllMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Vector<WasabiUserDTO> allMembers = new Vector<WasabiUserDTO>();
			for (NodeIterator ni : GroupServiceImpl.getAllMembers(groupNode)) {
				while (ni.hasNext()) {
					Node userRef = ni.nextNode();
					Node user = null;
					try {
						user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
					} catch (ItemNotFoundException infe) {
						userRef.remove();
					}
					if (user != null) {
						WasabiUserDTO userDTO = (WasabiUserDTO) TransferManager.convertNode2DTO(user);
						if (!allMembers.contains(userDTO)) {
							allMembers.add((WasabiUserDTO) TransferManager.convertNode2DTO(user));
						}
					}
				}
			}
			return allMembers;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiGroupDTO getParentGroup(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			return TransferManager.convertNode2DTO(GroupServiceImpl.getParentGroup(groupNode));
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiGroupDTO getSubGroupByName(WasabiGroupDTO group, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			return TransferManager.convertNode2DTO(GroupServiceImpl.getSubGroupByName(groupNode, name));
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getSubGroups(WasabiGroupDTO group) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Vector<WasabiGroupDTO> subgroups = new Vector<WasabiGroupDTO>();
			NodeIterator ni = GroupServiceImpl.getSubGroups(groupNode);
			while (ni.hasNext()) {
				subgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return subgroups;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiGroupDTO> getTopLevelGroups() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Vector<WasabiGroupDTO> topgroups = new Vector<WasabiGroupDTO>();
			NodeIterator ni = GroupServiceImpl.getTopLevelGroups(s);
			while (ni.hasNext()) {
				topgroups.add((WasabiGroupDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return topgroups;
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean isDirectMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Node userNode = TransferManager.convertDTO2Node(user, s);
			return GroupServiceImpl.isDirectMember(groupNode, userNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean isMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Node userNode = TransferManager.convertDTO2Node(user, s);
			return GroupServiceImpl.isMember(groupNode, userNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public void move(WasabiGroupDTO group, WasabiGroupDTO newParentGroup) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node newParentGroupNode = null;
			if (newParentGroup != null) {
				newParentGroupNode = TransferManager.convertDTO2Node(newParentGroup, s);
			}
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			GroupServiceImpl.move(groupNode, newParentGroupNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}

	}

	@Override
	public void remove(WasabiGroupDTO group) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			GroupServiceImpl.remove(groupNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}

	}

	@Override
	public void removeMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			Node userNode = TransferManager.convertDTO2Node(user, s);
			GroupServiceImpl.removeMember(groupNode, userNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}

	}

	@Override
	public void rename(WasabiGroupDTO group, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			GroupServiceImpl.rename(groupNode, name);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}

	}

	@Override
	public void setDisplayName(WasabiGroupDTO group, String displayName) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node groupNode = TransferManager.convertDTO2Node(group, s);
			GroupServiceImpl.setDisplayName(groupNode, displayName);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}
}
