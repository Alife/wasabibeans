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

import java.io.Serializable;
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
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.AttributeValueException;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.AttributeServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.AttributeServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.AttributeServiceRemote;

/**
 * Class, that implements the internal access on WasabiAttribute objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "AttributeService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AttributeService extends ObjectService implements AttributeServiceLocal, AttributeServiceRemote {

	@Override
	public WasabiAttributeDTO create(String name, Serializable value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			AttributeValueException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Locker.recognizeDeepLockToken(affiliation, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node affiliationNode = TransferManager.convertDTO2Node(affiliation, s);
			Node attributeNode = AttributeServiceImpl.create(name, value, affiliationNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(attributeNode, affiliationNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(attributeNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiAttributeDTO create(String name, WasabiObjectDTO value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, AttributeValueException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Locker.recognizeDeepLockToken(affiliation, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node affiliationNode = TransferManager.convertDTO2Node(affiliation, s);
			Node valueNode = TransferManager.convertDTO2Node(value, s);
			Node attributeNode = AttributeServiceImpl.create(name, valueNode, affiliationNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(attributeNode, affiliationNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(attributeNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiValueDTO getAffiliation(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
			return TransferManager.convertValue2DTO(AttributeServiceImpl.getAffiliation(attributeNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiAttributeDTO getAttributeByName(WasabiObjectDTO object, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node objectNode = TransferManager.convertDTO2Node(object, s);
			return TransferManager.convertNode2DTO(AttributeServiceImpl.getAttributeByName(objectNode, name));
		} finally {
			jcr.logout();
		}
	}

	@Override
	public String getAttributeType(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
			return AttributeServiceImpl.getAttributeType(attributeNode);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public Vector<WasabiAttributeDTO> getAttributes(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node objectNode = TransferManager.convertDTO2Node(object, s);
			Vector<WasabiAttributeDTO> attributes = new Vector<WasabiAttributeDTO>();
			NodeIterator ni = AttributeServiceImpl.getAttributes(objectNode);
			while (ni.hasNext()) {
				attributes.add((WasabiAttributeDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return attributes;
		} finally {
			jcr.logout();
		}
	}

	@Override
	public <T extends Serializable> WasabiValueDTO getValue(Class<T> type, WasabiAttributeDTO attribute)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, AttributeValueException {
		if (type == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"type"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
			return TransferManager.convertValue2DTO(AttributeServiceImpl.getValue(type, attributeNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public WasabiValueDTO getWasabiValue(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, AttributeValueException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
			return TransferManager.convertValue2DTO(AttributeServiceImpl.getWasabiValue(attributeNode), optLockId);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void move(WasabiAttributeDTO attribute, WasabiObjectDTO newAffiliation, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		Node attributeNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Node newAffiliationNode = TransferManager.convertDTO2Node(newAffiliation, s);
			Locker.acquireLock(attributeNode, attribute, false, s, locker);
			Locker.checkOptLockId(attributeNode, attribute, optLockId);
			AttributeServiceImpl.move(attributeNode, newAffiliationNode, callerPrincipal);
			s.save();
			EventCreator.createMovedEvent(attributeNode, newAffiliationNode, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(attributeNode, attribute, s, locker);
			jcr.logout();
		}
	}

	@Override
	public void remove(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
			EventCreator.createRemovedEvent(attributeNode, jms, callerPrincipal);
			AttributeServiceImpl.remove(attributeNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	@Override
	public void rename(WasabiAttributeDTO attribute, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node attributeNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Locker.acquireLock(attributeNode, attribute, false, s, locker);
			Locker.checkOptLockId(attributeNode, attribute, optLockId);
			AttributeServiceImpl.rename(attributeNode, name, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.NAME, name, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(attributeNode, attribute, s, locker);
			jcr.logout();
		}
	}

	@Override
	public void setValue(WasabiAttributeDTO attribute, Serializable value, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, AttributeValueException,
			ObjectDoesNotExistException {
		Node attributeNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Locker.acquireLock(attributeNode, attribute, false, s, locker);
			Locker.checkOptLockId(attributeNode, attribute, optLockId);
			AttributeServiceImpl.setValue(attributeNode, value, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.VALUE, value, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(attributeNode, attribute, s, locker);
			jcr.logout();
		}
	}

	@Override
	public void setWasabiValue(WasabiAttributeDTO attribute, WasabiObjectDTO value, Long optLockId)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException {
		Node attributeNode = null;
		Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			attributeNode = TransferManager.convertDTO2Node(attribute, s);
			Node valueNode = null;
			if (value != null) {
				valueNode = TransferManager.convertDTO2Node(value, s);
			}
			Locker.acquireLock(attributeNode, attribute, false, s, locker);
			Locker.checkOptLockId(attributeNode, attribute, optLockId);
			AttributeServiceImpl.setWasabiValue(attributeNode, valueNode, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.VALUE, valueNode, jms,
					callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(attributeNode, attribute, s, locker);
			jcr.logout();
		}
	}
}
