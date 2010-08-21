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

import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class GroupServiceImpl {

	public static void addMember(Node groupNode, Node userNode) throws UnexpectedInternalProblemException {
		try {
			Node userRef = groupNode.getNode(WasabiNodeProperty.MEMBERS).addNode(userNode.getIdentifier(),
					WasabiNodeType.OBJECT_REF);
			userRef.setProperty(WasabiNodeProperty.REFERENCED_OBJECT, userNode);
			Node groupRef = userNode.getNode(WasabiNodeProperty.MEMBERSHIPS).addNode(groupNode.getIdentifier(),
					WasabiNodeType.OBJECT_REF);
			groupRef.setProperty(WasabiNodeProperty.REFERENCED_OBJECT, groupNode);
		} catch (ItemExistsException iee) {
			// do nothing, user is already a member of the group
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

	}

	public static Node create(String name, Node parentGroupNode, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			Node newGroup;
			if (parentGroupNode != null) {
				newGroup = parentGroupNode.getNode(WasabiNodeProperty.SUBGROUPS).addNode(name, WasabiNodeType.GROUP);
			} else {
				Node rootOfGroupsNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME);
				newGroup = rootOfGroupsNode.addNode(name, WasabiNodeType.GROUP);
			}
			setDisplayName(newGroup, name, null);

			// special case when creating the group wasabi or when creating the group admins
			if (name.equals(WasabiConstants.WASABI_GROUP_NAME) || name.equals(WasabiConstants.ADMINS_GROUP_NAME)) {
				ObjectServiceImpl.created(newGroup, s, null, true);
			} else {
				ObjectServiceImpl.created(newGroup, s, callerPrincipal, true);
			}

			return newGroup;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getAllGroups(Session s) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.GROUP, "s1");

			// build the query
			Query query = qomf.createQuery(selector, null, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getDisplayName(Node groupNode) throws UnexpectedInternalProblemException {
		try {
			return groupNode.getProperty(WasabiNodeProperty.DISPLAY_NAME).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getGroupByName(String groupName, Session s) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.GROUP, "s1");
			Constraint constraint = qomf.comparison(qomf.nodeName("s1"),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(groupName)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			NodeIterator ni = query.execute().getNodes();
			if (ni.hasNext()) {
				return ni.nextNode();
			} else {
				return null;
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getWasabiGroup(Session s) throws UnexpectedInternalProblemException {
		try {
			return s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME).getNode(
					WasabiConstants.WASABI_GROUP_NAME);
		} catch (PathNotFoundException pnfe) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_WASABI_GROUP, pnfe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getGroupsByDisplayName(String displayName, Session s)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.GROUP, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", WasabiNodeProperty.DISPLAY_NAME),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(displayName)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getMemberByName(Node groupNode, String userName) throws UnexpectedInternalProblemException {
		try {
			NodeIterator ni = groupNode.getNode(WasabiNodeProperty.MEMBERS).getNodes();
			while (ni.hasNext()) {
				Node aMemberRef = ni.nextNode();
				Node aMember = null;
				try {
					aMember = aMemberRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					aMemberRef.remove();
				}
				if (aMember != null && aMember.getName().equals(userName)) {
					return aMember;
				}
			}
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns a {@code NodeIterator} containing nodes of type wasabi:objectref that point to the actual
	 * wasabi:user-nodes. So the returned {@code NodeIterator} does NOT contain the actual wasabi:user-nodes (this is
	 * due to efficiency reasons).
	 * 
	 * @param groupNode
	 *            the node representing a wasabi-group
	 * @return {@code NodeIterator} containing nodes of type wasabi:objctref
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getMembers(Node groupNode) throws UnexpectedInternalProblemException {
		try {
			return groupNode.getNode(WasabiNodeProperty.MEMBERS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns a {@code Vector} that contains a {@code NodeIterator} for the direct members of the group represented by
	 * the given node and a {@code NodeIterator} for each subgroup of the group represented by the given node.
	 * 
	 * @param groupNode
	 *            the node representing a wasabi-group
	 * @return {@code Vector} containing {@code NodeIterator}s
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<NodeIterator> getAllMembers(Node groupNode) throws UnexpectedInternalProblemException {
		Vector<NodeIterator> allMembers = new Vector<NodeIterator>();
		retrieveAllMembers(allMembers, groupNode);
		return allMembers;
	}

	private static Vector<NodeIterator> retrieveAllMembers(Vector<NodeIterator> allMembers, Node groupNode)
			throws UnexpectedInternalProblemException {
		allMembers.add(getMembers(groupNode));
		NodeIterator ni = getSubGroups(groupNode);
		while (ni.hasNext()) {
			allMembers.addAll(retrieveAllMembers(allMembers, ni.nextNode()));
		}
		return allMembers;
	}

	public static Node getParentGroup(Node groupNode) throws UnexpectedInternalProblemException {
		try {
			Node parent = groupNode.getParent().getParent();
			if (parent.getName().equals("")) { // only the root node has an empty name (see JSR 283 3.1.3)
				return null;
			}
			return parent;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getSubGroupByName(Node groupNode, String name) throws UnexpectedInternalProblemException {
		try {
			return groupNode.getNode(WasabiNodeProperty.SUBGROUPS).getNode(name);
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getSubGroups(Node groupNode) throws UnexpectedInternalProblemException {
		try {
			return groupNode.getNode(WasabiNodeProperty.SUBGROUPS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getTopLevelGroups(Session s) throws UnexpectedInternalProblemException {
		try {
			Node rootOfGroupsNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME);
			return rootOfGroupsNode.getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean isDirectMember(Node groupNode, Node userNode) throws UnexpectedInternalProblemException {
		try {
			return groupNode.getNode(WasabiNodeProperty.MEMBERS).hasNode(userNode.getIdentifier());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean isMember(Node groupNode, Node userNode) throws UnexpectedInternalProblemException {
		if (isDirectMember(groupNode, userNode)) {
			return true;
		} else {
			NodeIterator ni = getSubGroups(groupNode);
			while (ni.hasNext()) {
				Node aSubgroup = ni.nextNode();
				if (isMember(aSubgroup, userNode)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void move(Node groupNode, Node newParentGroupNode, String callerPrincipal)
			throws UnexpectedInternalProblemException {
		try {
			if (newParentGroupNode != null) {
				groupNode.getSession().move(groupNode.getPath(),
						newParentGroupNode.getPath() + "/" + WasabiNodeProperty.SUBGROUPS + "/" + groupNode.getName());
			} else {
				Session s = groupNode.getSession();
				Node rootOfGroupsNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME);
				s.move(groupNode.getPath(), rootOfGroupsNode.getPath() + "/" + groupNode.getName());
			}
			ObjectServiceImpl.modified(groupNode, groupNode.getSession(), callerPrincipal, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

	}

	public static void remove(Node groupNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(groupNode);
	}

	public static void removeMember(Node groupNode, Node userNode) throws UnexpectedInternalProblemException {
		try {
			Node userref = groupNode.getNode(WasabiNodeProperty.MEMBERS).getNode(userNode.getIdentifier());
			userref.remove();
			Node groupref = userNode.getNode(WasabiNodeProperty.MEMBERSHIPS).getNode(groupNode.getIdentifier());
			groupref.remove();
		} catch (PathNotFoundException pnfe) {
			// do nothing, user to be removed is not a member
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node groupNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			if (getGroupByName(name, groupNode.getSession()) != null) {
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "group", name), name);
			}

			groupNode.getSession().move(groupNode.getPath(), groupNode.getParent().getPath() + "/" + name);
			ObjectServiceImpl.modified(groupNode, groupNode.getSession(), callerPrincipal, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setDisplayName(Node groupNode, String displayName, String callerPrincipal)
			throws UnexpectedInternalProblemException {
		try {
			groupNode.setProperty(WasabiNodeProperty.DISPLAY_NAME, displayName);
			ObjectServiceImpl.modified(groupNode, groupNode.getSession(), callerPrincipal, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
