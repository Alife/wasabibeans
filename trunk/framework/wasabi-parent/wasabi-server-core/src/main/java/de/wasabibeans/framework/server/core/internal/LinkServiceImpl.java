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

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class LinkServiceImpl {

	public static Node create(String name, Node destinationNode, Node environmentNode, Session s, String callerPrincipal)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException {
		try {
			Node linkNode = environmentNode.addNode(WasabiNodeProperty.LINKS + "/" + name, WasabiNodeType.LINK);
			setDestination(linkNode, destinationNode, null);
			ObjectServiceImpl.created(linkNode, s, callerPrincipal, true);

			return linkNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "link", name), name, iee);
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
			throw new TargetDoesNotExistException(WasabiExceptionMessages.INTERNAL_REFERENCE_INVALID, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node linkNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(linkNode);
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

	public static void move(Node linkNode, Node newEnvironmentNode, String callerPrincipal)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException {
		try {
			linkNode.getSession().move(linkNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.LINKS + "/" + linkNode.getName());
			ObjectServiceImpl.modified(linkNode, linkNode.getSession(), callerPrincipal, false);
		} catch (ItemExistsException iee) {
			try {
				String name = linkNode.getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "link", name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node linkNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(linkNode);
	}

	public static void rename(Node linkNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(linkNode, name, callerPrincipal);
	}

	public static void setDestination(Node linkNode, Node objectNode, String callerPrincipal)
			throws UnexpectedInternalProblemException {
		try {
			linkNode.setProperty(WasabiNodeProperty.DESTINATION, objectNode);
			ObjectServiceImpl.modified(linkNode, linkNode.getSession(), callerPrincipal, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
