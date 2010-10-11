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
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.authorization.WasabiLinkACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
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
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
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
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class LinkService extends ObjectService implements LinkServiceLocal, LinkServiceRemote {

	@Override
	public WasabiLinkDTO create(String name, WasabiObjectDTO destination, WasabiLocationDTO environment)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node destinationNode = TransferManager.convertDTO2Node(destination, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
						WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.create()",
							"INSERT or WRITE", "environment"));
		/* Authorization - End */

		Node linkNode = LinkServiceImpl.create(name, destinationNode, environmentNode, s,
				WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createCreatedEvent(linkNode, environmentNode, jms, callerPrincipal);
		return TransferManager.convertNode2DTO(linkNode, environment);
	}

	@Override
	public WasabiValueDTO getDestination(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node linkNode = TransferManager.convertDTO2Node(link, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.READ, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.getDestination()",
							"READ", "document"));
		/* Authorization - End */

		Long optLockId = ObjectServiceImpl.getOptLockId(linkNode);
		return TransferManager.convertValue2DTO(LinkServiceImpl.getDestination(linkNode), optLockId);
	}

	@Override
	public WasabiValueDTO getEnvironment(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node linkNode = TransferManager.convertDTO2Node(link, s);
		Long optLockId = ObjectServiceImpl.getOptLockId(linkNode);
		Node environmentNode = LinkServiceImpl.getEnvironment(linkNode);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "LinkService.getEnvironment()",
							"VIEW"));
		/* Authorization - End */

		return TransferManager.convertValue2DTO(environmentNode, optLockId);
	}

	@Override
	public WasabiLinkDTO getLinkByName(WasabiLocationDTO location, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node linkNode = LinkServiceImpl.getLinkByName(locationNode, name);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
				if (!WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.VIEW, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "LinkService.getLinkByName()",
							"VIEW"));
		/* Authorization - End */

		return TransferManager.convertNode2DTO(linkNode, location);
	}

	@Override
	public Vector<WasabiLinkDTO> getLinks(WasabiLocationDTO location) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(locationNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (String id : authorizedLinks) {
					Node link = LinkServiceImpl.getLinkById(id, s);
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, location));
				}
			} else
				for (NodeIterator ni = LinkServiceImpl.getLinks(locationNode); ni.hasNext();)
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = LinkServiceImpl.getLinks(locationNode); ni.hasNext();)
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate, depth))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate, depth))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByCreationDate(environmentNode, startDate, endDate, depth))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				for (NodeIterator ni = LinkServiceImpl.getLinksByCreator(creatorNode); ni.hasNext();) {
					Node linkNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.VIEW, s))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(linkNode));
				}
			} else
				for (NodeIterator ni = LinkServiceImpl.getLinksByCreator(creatorNode); ni.hasNext();)
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = LinkServiceImpl.getLinksByCreator(creatorNode); ni.hasNext();)
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByCreator(creatorNode, environmentNode))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByCreator(creatorNode, environmentNode))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByCreator(creatorNode, environmentNode))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate, depth))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByModificationDate(environmentNode, startDate, endDate, depth))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				for (NodeIterator ni = LinkServiceImpl.getLinksByModifier(modifierNode); ni.hasNext();) {
					Node linkNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.VIEW, s))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(linkNode));
				}
			}
			for (NodeIterator ni = LinkServiceImpl.getLinksByModifier(modifierNode); ni.hasNext();)
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		/* Authorization - End */
		for (NodeIterator ni = LinkServiceImpl.getLinksByModifier(modifierNode); ni.hasNext();)
			links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode()));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Vector<String> authorizedLinks = WasabiAuthorizer.authorizePermission(environmentNode, callerPrincipal,
						WasabiPermission.VIEW, WasabiType.LINK, s);
				for (Node link : LinkServiceImpl.getLinksByModifier(modifierNode, environmentNode))
					if (authorizedLinks.contains(link))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
			} else
				for (Node link : LinkServiceImpl.getLinksByModifier(modifierNode, environmentNode))
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));
		}
		/* Authorization - End */
		else
			for (Node link : LinkServiceImpl.getLinksByModifier(modifierNode, environmentNode))
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(link, environment));

		return links;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Vector<WasabiLinkDTO> links = new Vector<WasabiLinkDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				for (NodeIterator ni = LinkServiceImpl.getLinksOrderedByCreationDate(locationNode, order); ni.hasNext();) {
					Node linkNode = ni.nextNode();
					if (WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.VIEW, s))
						links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(linkNode, location));
				}
			} else
				for (NodeIterator ni = LinkServiceImpl.getLinksOrderedByCreationDate(locationNode, order); ni.hasNext();)
					links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
		}
		/* Authorization - End */
		else
			for (NodeIterator ni = LinkServiceImpl.getLinksOrderedByCreationDate(locationNode, order); ni.hasNext();)
				links.add((WasabiLinkDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));

		return links;
	}

	@Override
	public void move(WasabiLinkDTO link, WasabiLocationDTO newEnvironment, Long optLockId)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ConcurrentModificationException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node linkNode = TransferManager.convertDTO2Node(link, s);
		Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.authorize(newEnvironmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
					WasabiPermission.WRITE }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.move()", "INSERT or WRITE",
						"newEnvironment"));
			if (!WasabiAuthorizer.authorizeChildreen(linkNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.move()", "WRITE",
						"link and sub objects"));
		}
		/* Authorization - End */

		Locker.checkOptLockId(linkNode, link, optLockId);
		LinkServiceImpl.move(linkNode, newEnvironmentNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createMovedEvent(linkNode, newEnvironmentNode, jms, callerPrincipal);
	}

	@Override
	public void remove(WasabiLinkDTO link, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node linkNode = TransferManager.convertDTO2Node(link, s);
		Locker.checkOptLockId(linkNode, link, optLockId);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorize(linkNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.remove()", "WRITE",
							"document"));
				else {
					EventCreator.createRemovedEvent(linkNode, jms, callerPrincipal);
					WasabiLinkACL.remove(linkNode, callerPrincipal, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms);
				}
			} else
				LinkServiceImpl.remove(linkNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms, callerPrincipal);
		}
		/* Authorization - End */
		else {
			LinkServiceImpl.remove(linkNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, true, jms, callerPrincipal);
		}
	}

	@Override
	public void rename(WasabiLinkDTO link, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ObjectDoesNotExistException, ConcurrentModificationException,
			NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node linkNode = TransferManager.convertDTO2Node(link, s);

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorizeChildreen(linkNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.remove()", "WRITE",
							"link"));
			}
		/* Authorization - End */

		Locker.checkOptLockId(linkNode, link, optLockId);
		LinkServiceImpl.rename(linkNode, name, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(linkNode, WasabiProperty.NAME, name, jms, callerPrincipal);
	}

	@Override
	public void setDestination(WasabiLinkDTO link, WasabiObjectDTO object, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSession();
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node linkNode = TransferManager.convertDTO2Node(link, s);
		Node objectNode = null;
		if (object != null) {
			objectNode = TransferManager.convertDTO2Node(object, s);
		}

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				if (!WasabiAuthorizer.authorizeChildreen(linkNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "LinkService.setDestination()",
							"WRITE", "link"));
			}
		/* Authorization - End */

		Locker.checkOptLockId(linkNode, link, optLockId);
		LinkServiceImpl.setDestination(linkNode, objectNode, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
		EventCreator.createPropertyChangedEvent(linkNode, WasabiProperty.DESTINATION, objectNode, jms, callerPrincipal);
	}
}
