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
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class ContainerServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException {
		try {
			Node containerNode = environmentNode.addNode(WasabiNodeProperty.CONTAINERS + "/" + name,
					WasabiNodeType.CONTAINER);
			ObjectServiceImpl.created(containerNode, s, callerPrincipal, true);
			return containerNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "container", name), name, iee);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_CREATION_FAILURE, "container"), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getContainerByName(Node locationNode, String name) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.CONTAINERS + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getContainers(Node locationNode) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.CONTAINERS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their creation-date is not before the given {@code startDate} and not after the given
	 * {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByCreationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.CONTAINERS, startDate,
				endDate);
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct or indirect (up to the given {@code depth}) sub-containers of the
	 * wasabi-environment represented by the given {@code environmentNode}. 3) Their creation-date is not before the
	 * given {@code startDate} and not after the given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct sub-containers; {@code depth < 0} -> all direct and indirect
	 *            sub-containers
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.CONTAINERS, startDate,
				endDate, depth);
	}

	public static NodeIterator getContainersByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, WasabiNodeType.CONTAINER);
	}

	public static Vector<Node> getContainersByCreator(Node creatorNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, environmentNode, WasabiNodeProperty.CONTAINERS);
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their modification-date is not before the given {@code startDate} and not after the
	 * given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByModificationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.CONTAINERS, startDate,
				endDate);
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct or indirect (up to the given {@code depth}) sub-containers of the
	 * wasabi-environment represented by the given {@code environmentNode}. 3) Their modification-date is not before the
	 * given {@code startDate} and not after the given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct sub-containers; {@code depth < 0} -> all direct and indirect
	 *            sub-containers
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByModificationDate(Node environmentNode, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.CONTAINERS, startDate,
				endDate, depth);
	}

	public static NodeIterator getContainersByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, WasabiNodeType.CONTAINER);
	}

	public static Vector<Node> getContainersByModifier(Node modifierNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, environmentNode, WasabiNodeProperty.CONTAINERS);
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their names match the given regular expression {@code regex}.
	 * 
	 * @param environmentNode
	 * @param regex
	 * @return
	 * @throws UnexpectedInternalProblemException
	 * @see http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#sum
	 */
	public static Vector<Node> getContainersByNamePattern(Node environmentNode, String regex)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByNamePattern(environmentNode, regex, WasabiNodeProperty.CONTAINERS);
	}

	public static Node getEnvironment(Node containerNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(containerNode);
	}

	public static void move(Node containerNode, Node newEnvironmentNode, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			containerNode.getSession().move(containerNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.CONTAINERS + "/" + containerNode.getName());
			ObjectServiceImpl.modified(containerNode, containerNode.getSession(), callerPrincipal, false);
		} catch (ItemExistsException iee) {
			try {
				String name = containerNode.getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "container", name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

	}

	public static void remove(Node containerNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(containerNode);
	}

	public static void rename(Node containerNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(containerNode, name, callerPrincipal);
	}
}
