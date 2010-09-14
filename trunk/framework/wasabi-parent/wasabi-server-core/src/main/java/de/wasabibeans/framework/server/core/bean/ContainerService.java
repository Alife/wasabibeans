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
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ContainerServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.ContainerServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.ContainerServiceRemote;

/**
 * Class, that implements the internal access on WasabiContainer objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "ContainerService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ContainerService extends ObjectService implements ContainerServiceLocal, ContainerServiceRemote {

	@Override
	public WasabiContainerDTO create(String name, WasabiLocationDTO environment) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Node containerNode = ContainerServiceImpl.create(name, environmentNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(containerNode, environmentNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(containerNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiContainerDTO getContainerByName(WasabiLocationDTO location, String name)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			return TransferManager.convertNode2DTO(ContainerServiceImpl.getContainerByName(locationNode, name));
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainers(WasabiLocationDTO location) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (NodeIterator ni = ContainerServiceImpl.getContainers(locationNode); ni.hasNext();) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByCreationDate(environmentNode, startDate, endDate,
					depth)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (NodeIterator ni = ContainerServiceImpl.getContainersByCreator(creatorNode); ni.hasNext();) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByCreator(creatorNode, environmentNode)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByModificationDate(environmentNode, startDate,
					endDate, depth)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (NodeIterator ni = ContainerServiceImpl.getContainersByModifier(modifierNode); ni.hasNext();) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByModifier(modifierNode, environmentNode)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByNamePattern(WasabiLocationDTO environment, String pattern)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiContainerDTO> containers = new Vector<WasabiContainerDTO>();
			for (Node container : ContainerServiceImpl.getContainersByNamePattern(environmentNode, pattern)) {
				containers.add((WasabiContainerDTO) TransferManager.convertNode2DTO(container));
			}
			return containers;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiContainerDTO container) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node containerNode = TransferManager.convertDTO2Node(container, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(containerNode);
			return TransferManager.convertValue2DTO(ContainerServiceImpl.getEnvironment(containerNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void move(WasabiContainerDTO container, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		Node containerNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			containerNode = TransferManager.convertDTO2Node(container, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
			Locker.acquireLock(containerNode, container, optLockId, s, locker);
			ContainerServiceImpl.move(containerNode, newEnvironmentNode, callerPrincipal);
			s.save();
			EventCreator.createMovedEvent(containerNode, newEnvironmentNode, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(containerNode, s, locker);
			jcr.logout();
		}
	}

	@Override
	public void remove(WasabiContainerDTO container) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node containerNode = TransferManager.convertDTO2Node(container, s);
			EventCreator.createRemovedEvent(containerNode, jms, callerPrincipal);
			ContainerServiceImpl.remove(containerNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void rename(WasabiContainerDTO container, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node containerNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			containerNode = TransferManager.convertDTO2Node(container, s);
			Locker.acquireLock(containerNode, container, optLockId, s, locker);
			ContainerServiceImpl.rename(containerNode, name, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(containerNode, WasabiProperty.NAME, name, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(containerNode, s, locker);
			jcr.logout();
		}
	}
}