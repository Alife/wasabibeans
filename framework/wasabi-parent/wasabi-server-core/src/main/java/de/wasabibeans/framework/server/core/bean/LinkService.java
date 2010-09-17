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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.LinkServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.LinkServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.LinkServiceRemote;

/**
 * Class, that implements the internal access on WasabiLink objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "LinkService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LinkService extends ObjectService implements LinkServiceLocal, LinkServiceRemote {

	@Override
	public WasabiLinkDTO create(String name, WasabiObjectDTO destination, WasabiLocationDTO environment)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node destinationNode = TransferManager.convertDTO2Node(destination, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Locker.recognizeLockTokens(s, environment);
			Node linkNode = LinkServiceImpl.create(name, destinationNode, environmentNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(linkNode, environmentNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(linkNode, environment);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiValueDTO getDestination(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node linkNode = TransferManager.convertDTO2Node(link, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(linkNode);
			return TransferManager.convertValue2DTO(LinkServiceImpl.getDestination(linkNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node linkNode = TransferManager.convertDTO2Node(link, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(linkNode);
			return TransferManager.convertValue2DTO(LinkServiceImpl.getEnvironment(linkNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiLinkDTO getLinkByName(WasabiLocationDTO location, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			return TransferManager.convertNode2DTO(LinkServiceImpl.getLinkByName(locationNode, name), location);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinks(WasabiLocationDTO location) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (NodeIterator ni = LinkServiceImpl.getLinks(locationNode); ni.hasNext();) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate, depth)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (NodeIterator ni = LinkServiceImpl.getLinksByCreator(creatorNode); ni.hasNext();) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByCreator(creatorNode, environmentNode)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate, depth)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (NodeIterator ni = LinkServiceImpl.getLinksByModifier(modifierNode); ni.hasNext();) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (Node link : LinkServiceImpl.getLinksByModifier(modifierNode, environmentNode)) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
			for (NodeIterator ni = LinkServiceImpl.getLinksOrderedByCreationDate(locationNode, order); ni.hasNext();) {
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
			}
			return links;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void move(WasabiLinkDTO link, WasabiLocationDTO newEnvironment, Long optLockId)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		Node linkNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			linkNode = TransferManager.convertDTO2Node(link, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
			Locker.recognizeLockTokens(s, link, newEnvironment);
			Locker.acquireLock(linkNode, link, false, s, locker);
			Locker.checkOptLockId(linkNode, link, optLockId);
			LinkServiceImpl.move(linkNode, newEnvironmentNode, callerPrincipal);
			EventCreator.createMovedEvent(linkNode, newEnvironmentNode, jms, callerPrincipal);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(linkNode, link, s, locker);
			jcr.logout();
		}
	}

	@Override
	public void remove(WasabiLinkDTO link) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node linkNode = TransferManager.convertDTO2Node(link, s);
			EventCreator.createRemovedEvent(linkNode, jms, callerPrincipal);
			Locker.recognizeLockTokens(s, link);
			LinkServiceImpl.remove(linkNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void rename(WasabiLinkDTO link, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ObjectDoesNotExistException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node linkNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			linkNode = TransferManager.convertDTO2Node(link, s);
			Locker.recognizeLockTokens(s, link);
			Locker.acquireLock(linkNode, link, false, s, locker);
			Locker.checkOptLockId(linkNode, link, optLockId);
			LinkServiceImpl.rename(linkNode, name, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(linkNode, WasabiProperty.NAME, name, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(linkNode, link, s, locker);
			jcr.logout();
		}

	}

	@Override
	public void setDestination(WasabiLinkDTO link, WasabiObjectDTO object, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException {
		Node linkNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			linkNode = TransferManager.convertDTO2Node(link, s);
			Node objectNode = null;
			if (object != null) {
				objectNode = TransferManager.convertDTO2Node(object, s);
			}
			Locker.recognizeLockTokens(s, link);
			Locker.acquireLock(linkNode, link, false, s, locker);
			Locker.checkOptLockId(linkNode, link, optLockId);
			LinkServiceImpl.setDestination(linkNode, objectNode, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(linkNode, WasabiProperty.DESTINATION, objectNode, jms,
					callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(linkNode, link, s, locker);
			jcr.logout();
		}
	}
}
