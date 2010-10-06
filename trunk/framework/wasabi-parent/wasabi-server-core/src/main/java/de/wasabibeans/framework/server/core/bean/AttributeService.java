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
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAttributeACL;
import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.AttributeValueException;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
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
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AttributeService extends ObjectService implements AttributeServiceLocal, AttributeServiceRemote {

	@Override
	public WasabiAttributeDTO create(String name, Serializable value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			AttributeValueException, ConcurrentModificationException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node affiliationNode = TransferManager.convertDTO2Node(affiliation, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(affiliationNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.create()",
						"INSERT or WRITE", "affiliation"));
		/* Authorization - End */

		Node attributeNode = AttributeServiceImpl.create(name, value, affiliationNode, s,
				WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createCreatedEvent(attributeNode, affiliationNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(attributeNode, affiliation);
	}

	@Override
	public WasabiAttributeDTO create(String name, WasabiObjectDTO value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, AttributeValueException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node affiliationNode = TransferManager.convertDTO2Node(affiliation, s);
		Node valueNode = TransferManager.convertDTO2Node(value, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(affiliationNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.create()",
						"INSERT or WRITE", "affiliation"));
		/* Authorization - End */

		Node attributeNode = AttributeServiceImpl.create(name, valueNode, affiliationNode, s,
				WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createCreatedEvent(attributeNode, affiliationNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(attributeNode, affiliation);
	}

	@Override
	public WasabiValueDTO getAffiliation(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
		Node affiliationNode = AttributeServiceImpl.getAffiliation(attributeNode);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(affiliationNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
						"AttributeService.getAffiliation()", "READ"));
		/* Authorization - End */

		return TransferManager.convertValue2DTO(affiliationNode, optLockId);
	}

	@Override
	public WasabiAttributeDTO getAttributeByName(WasabiObjectDTO object, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = AttributeServiceImpl.getAttributeByName(objectNode, name);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN,
						"AttributeService.getAttributeByName()", "VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(attributeNode, object);
	}

	@Override
	public Vector<WasabiAttributeDTO> getAttributes(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		Vector<WasabiAttributeDTO> attributes = new Vector<WasabiAttributeDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			Vector<String> authorizedDocuments = WasabiAuthorizer.authorizePermission(objectNode, callerPrincipal,
					WasabiPermission.VIEW, WasabiType.ATTRIBUTE, s);
			for (String id : authorizedDocuments) {
				Node doc = AttributeServiceImpl.getAttributeById(id, s);
				attributes.add((WasabiAttributeDTO) TransferManager.convertNode2DTO(doc, object));
			}
		}
		/* Authorization - End */
		else {
			NodeIterator ni = AttributeServiceImpl.getAttributes(objectNode);
			while (ni.hasNext())
				attributes.add((WasabiAttributeDTO) TransferManager.convertNode2DTO(ni.nextNode(), object));
		}

		return attributes;
	}

	@Override
	public String getAttributeType(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.getAttributeType()",
						"READ", "attribute"));
		/* Authorization - End */

		return AttributeServiceImpl.getAttributeType(attributeNode);
	}

	@Override
	public <T extends Serializable> WasabiValueDTO getValue(Class<T> type, WasabiAttributeDTO attribute)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, AttributeValueException,
			NoPermissionException {
		if (type == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"type"));
		}

		Session s = jcr.getJCRSession();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.getValue()", "READ",
						"attribute"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
		return TransferManager.convertValue2DTO(AttributeServiceImpl.getValue(type, attributeNode), optLockId);
	}

	@Override
	public WasabiValueDTO getWasabiValue(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, AttributeValueException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.getWasabiValue()",
						"READ", "attribute"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(attributeNode);
		return TransferManager.convertValue2DTO(AttributeServiceImpl.getWasabiValue(attributeNode), optLockId);
	}

	@Override
	public void move(WasabiAttributeDTO attribute, WasabiObjectDTO newAffiliation, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		Node newAffiliationNode = TransferManager.convertDTO2Node(newAffiliation, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(newAffiliationNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.move()",
						"INSERT or WRITE", "newEnvironment"));
			if (!WasabiAuthorizer.authorizeChildreen(attributeNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.move()", "WRITE",
						"room and sub objects"));
		}
		/* Authorization - End */

		Locker.checkOptLockId(attributeNode, attribute, optLockId);
		AttributeServiceImpl.move(attributeNode, newAffiliationNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createMovedEvent(attributeNode, newAffiliationNode, jms, callerPrincipal);
	}

	@Override
	public void remove(WasabiAttributeDTO attribute, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		Locker.checkOptLockId(attributeNode, attribute, optLockId);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.remove()", "WRITE",
						"document"));
			else
				WasabiAttributeACL.remove(attributeNode, callerPrincipal, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		}
		/* Authorization - End */
		else {
			// TODO special case for events due to recursive deletion of subtree
			EventCreator.createRemovedEvent(attributeNode, jms, callerPrincipal);
			AttributeServiceImpl.remove(attributeNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
		}

	}

	@Override
	public void rename(WasabiAttributeDTO attribute, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.rename()", "WRITE",
						"attribute"));
		/* Authorization - End */

		Locker.checkOptLockId(attributeNode, attribute, optLockId);
		AttributeServiceImpl.rename(attributeNode, name, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.NAME, name, jms, callerPrincipal);
	}

	@Override
	public void setValue(WasabiAttributeDTO attribute, Serializable value, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, AttributeValueException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.setValue()", "WRITE",
						"attribute"));
		/* Authorization - End */

		Locker.checkOptLockId(attributeNode, attribute, optLockId);
		AttributeServiceImpl.setValue(attributeNode, value, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.VALUE, value, jms, callerPrincipal);
	}

	@Override
	public void setWasabiValue(WasabiAttributeDTO attribute, WasabiObjectDTO value, Long optLockId)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node attributeNode = TransferManager.convertDTO2Node(attribute, s);
		Node valueNode = null;
		if (value != null) {
			valueNode = TransferManager.convertDTO2Node(value, s);
		}

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(attributeNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "AttributeService.setWasabiValue()",
						"WRITE", "attribute"));
		/* Authorization - End */

		Locker.checkOptLockId(attributeNode, attribute, optLockId);
		AttributeServiceImpl.setWasabiValue(attributeNode, valueNode, s, WasabiConstants.JCR_SAVE_PER_METHOD,
				callerPrincipal);
		EventCreator.createPropertyChangedEvent(attributeNode, WasabiProperty.VALUE, valueNode, jms, callerPrincipal);
	}
}
