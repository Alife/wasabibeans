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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.authorization.WasabiLinkACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class LinkServiceImpl {

	public static Node create(String name, Node destinationNode, Node environmentNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws ObjectAlreadyExistsException, UnexpectedInternalProblemException,
			ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			Node linkNode = environmentNode.addNode(WasabiNodeProperty.LINKS + "/" + name, WasabiNodeType.LINK);
			setDestination(linkNode, destinationNode, s, false, null);
			ObjectServiceImpl.created(linkNode, s, false, callerPrincipal, true);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiLinkACL.ACLEntryForCreate(linkNode);
				WasabiLinkACL.ACLEntryTemplateForCreate(linkNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return linkNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name), iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getDestination(Node linkNode) throws TargetDoesNotExistException,
			UnexpectedInternalProblemException {
		try {
			return linkNode.getProperty(WasabiNodeProperty.DESTINATION).getNode();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node linkNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(linkNode);
	}

	public static Node getLinkById(String id, Session s) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		try {
			return s.getNodeByIdentifier(id);
		} catch (ItemNotFoundException infe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages
					.get(WasabiExceptionMessages.OBJECT_DNE_ID, id), infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getLinkByName(Node locationNode, String name) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.LINKS + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getLinks(Node locationNode) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.LINKS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-links. 2) Their
	 * represented wasabi-links are direct links of the wasabi-environment represented by the given {@code
	 * environmentNode}. 3) Their creation-date is not before the given {@code startDate} and not after the given
	 * {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getLinksByCreationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.LINKS, startDate, endDate);
	}

	public static Vector<Node> getLinksByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.LINKS, startDate, endDate,
				depth);
	}

	public static NodeIterator getLinksByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, WasabiNodeType.LINK);
	}

	public static Vector<Node> getLinksByCreator(Node creatorNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, environmentNode, WasabiNodeProperty.LINKS);
	}

	public static Vector<Node> getLinksByModificationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.LINKS, startDate,
				endDate);
	}

	public static Vector<Node> getLinksByModificationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.LINKS, startDate,
				endDate, depth);
	}

	public static NodeIterator getLinksByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, WasabiNodeType.LINK);
	}

	public static Vector<Node> getLinksByModifier(Node modifierNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, environmentNode, WasabiNodeProperty.LINKS);
	}

	public static NodeIterator getLinksOrderedByCreationDate(Node locationNode, SortType order)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesOrderedByCreationDate(locationNode, WasabiNodeProperty.LINKS, order);
	}

	public static void move(Node linkNode, Node newEnvironmentNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException {
		try {
			linkNode.getSession().move(linkNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.LINKS + "/" + linkNode.getName());
			ObjectServiceImpl.modified(linkNode, s, false, callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiLinkACL.ACLEntryForMove(linkNode);
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_ENV, iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node linkNode, Session s, boolean doJcrSave, boolean throwEvents, JmsConnector jms,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			ObjectServiceImpl.removeRecursive(linkNode, s, true, jms, callerPrincipal);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node linkNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		ObjectServiceImpl.rename(linkNode, name, s, doJcrSave, callerPrincipal);
	}

	public static void setDestination(Node linkNode, Node objectNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			linkNode.setProperty(WasabiNodeProperty.DESTINATION, objectNode);
			ObjectServiceImpl.modified(linkNode, s, false, callerPrincipal, false);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
