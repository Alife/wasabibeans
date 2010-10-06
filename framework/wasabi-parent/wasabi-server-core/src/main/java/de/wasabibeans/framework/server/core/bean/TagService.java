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

import java.util.Map;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.TagServiceImpl;
import de.wasabibeans.framework.server.core.local.TagServiceLocal;
import de.wasabibeans.framework.server.core.remote.TagServiceRemote;

/**
 * Class, that implements the internal access on Tags of WasabiObject objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "TagService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TagService extends ObjectService implements TagServiceLocal, TagServiceRemote {

	@Override
	public void addTag(WasabiObjectDTO object, String tag) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException, NoPermissionException {
		if (tag == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"tag"));
		}

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.COMMENT, s))
				throw new NoPermissionException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "TagService.addTag()", "COMMENT",
								"object"));
		/* Authorization - End */

		TagServiceImpl.addTag(objectNode, tag, s, WasabiConstants.JCR_SAVE_PER_METHOD, callerPrincipal);
	}

	@Override
	public void clearTags(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "TagService.clearTags()", "WRITE",
						"object"));
		/* Authorization - End */

		TagServiceImpl.clearTags(objectNode, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByTags(WasabiLocationDTO environment, Vector<String> tags)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (tags == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"tags"));
		}

		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> result = new Vector<WasabiDocumentDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node node : TagServiceImpl.getDocumentsByTags(environmentNode, tags))
				if (WasabiAuthorizer.authorize(node, callerPrincipal, WasabiPermission.VIEW, s))
					result.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(node, environment));
		}
		/* Authorization - End */
		else
			for (Node node : TagServiceImpl.getDocumentsByTags(environmentNode, tags))
				result.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(node, environment));

		return result;
	}

	@Override
	public Map<String, Integer> getMostUsedDocumentTags(WasabiLocationDTO environment, int limit)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		return TagServiceImpl.getMostUsedDocumentTags(environmentNode, limit);
		// TODO: Maybe ACL Support :-)
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByTag(String tag) throws UnexpectedInternalProblemException {
		if (tag == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"tag"));
		}

		Session s = jcr.getJCRSession();
		Vector<WasabiObjectDTO> result = new Vector<WasabiObjectDTO>();
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			for (Node node : TagServiceImpl.getObjectsByTag(tag, s))
				if (WasabiAuthorizer.authorize(node, callerPrincipal, WasabiPermission.VIEW, s))
					result.add(TransferManager.convertNode2DTO(node));
		}
		/* Authorization - End */
		else
			for (Node node : TagServiceImpl.getObjectsByTag(tag, s))
				result.add(TransferManager.convertNode2DTO(node));

		return result;
	}

	@Override
	public Vector<String> getTags(WasabiObjectDTO object) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException {
		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.VIEW, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "TagService.getTags()", "VIEW", "object"));
		/* Authorization - End */

		return TagServiceImpl.getTags(objectNode);
	}

	@Override
	public void removeTag(WasabiObjectDTO object, String tag) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException, NoPermissionException {
		if (tag == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"tag"));
		}

		Session s = jcr.getJCRSession();
		Node objectNode = TransferManager.convertDTO2Node(object, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "TagService.removeTag()", "WRITE",
						"object"));
		/* Authorization - End */

		TagServiceImpl.removeTag(objectNode, tag, s, WasabiConstants.JCR_SAVE_PER_METHOD);
	}
}