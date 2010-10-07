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
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.authorization.WasabiContainerACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class ContainerServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			Node containerNode = environmentNode.addNode(WasabiNodeProperty.CONTAINERS + "/" + name,
					WasabiNodeType.CONTAINER);
			ObjectServiceImpl.created(containerNode, s, false, callerPrincipal, true);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiContainerACL.ACLEntryForCreate(containerNode, s, false);
				WasabiContainerACL.ACLEntryTemplateForCreate(containerNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return containerNode;
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

	public static void move(Node containerNode, Node newEnvironmentNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		try {
			containerNode.getSession().move(containerNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.CONTAINERS + "/" + containerNode.getName());
			ObjectServiceImpl.modified(containerNode, s, false, callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiContainerACL.ACLEntryForMove(containerNode, s, false);
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

	public static void remove(Node containerNode, Session s, boolean doJcrSave, boolean throwEvents, JmsConnector jms,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			ObjectServiceImpl.removeRecursive(containerNode, s, true, jms, callerPrincipal);

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

	public static void rename(Node containerNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		ObjectServiceImpl.rename(containerNode, name, s, doJcrSave, callerPrincipal);
	}
}
