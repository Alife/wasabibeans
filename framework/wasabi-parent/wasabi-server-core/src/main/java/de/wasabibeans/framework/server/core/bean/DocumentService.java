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
import java.util.Date;
import java.util.Vector;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.event.WasabiProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

	@EJB
	SharedFilterBean sharedFilterBean;

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ObjectDoesNotExistException,
			ConcurrentModificationException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Locker.recognizeLockTokens(s, environment);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(environmentNode, callerPrincipal, new int[] { WasabiPermission.INSERT,
						WasabiPermission.WRITE }, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "DocumentService.create()",
							"INSERT or WRITE", "environment"));
			/* Authorization - End */

			Node documentNode = DocumentServiceImpl.create(name, environmentNode, s, callerPrincipal);
			s.save();
			EventCreator.createCreatedEvent(documentNode, environmentNode, jms, callerPrincipal);
			return TransferManager.convertNode2DTO(documentNode, environment);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public WasabiValueDTO getContent(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node documentNode = TransferManager.convertDTO2Node(document, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		
		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(documentNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "DocumentService.getContent()",
						"READ", "document"));
		/* Authorization - End */
		
		Long optLockId = ObjectServiceImpl.getOptLockId(documentNode);
		return TransferManager.convertValue2DTO(DocumentServiceImpl.getContentPiped(documentNode, null), optLockId);
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		Node documentNode = DocumentServiceImpl.getDocumentByName(locationNode, name);
		
		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(documentNode, callerPrincipal, new int[] { WasabiPermission.VIEW, WasabiPermission.READ }, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION_RETURN, "DocumentService.getDocumentByName()",
						"VIEW or READ", "document"));
		/* Authorization - End */
		
		return TransferManager.convertNode2DTO(documentNode, location);
	}

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		NodeIterator ni = DocumentServiceImpl.getDocuments(locationNode);
		while (ni.hasNext()) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByCreationDate(environmentNode, startDate, endDate)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByCreationDate(environmentNode, startDate, endDate, depth)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (NodeIterator ni = DocumentServiceImpl.getDocumentsByCreator(creatorNode); ni.hasNext();) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node creatorNode = TransferManager.convertDTO2Node(creator, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByCreator(creatorNode, environmentNode)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByModificationDate(environmentNode, startDate, endDate)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByModificationDate(environmentNode, startDate, endDate,
				depth)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (NodeIterator ni = DocumentServiceImpl.getDocumentsByModifier(modifierNode); ni.hasNext();) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (Node document : DocumentServiceImpl.getDocumentsByModifier(modifierNode, environmentNode)) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document, environment));
		}
		return documents;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(location, s);
		Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
		for (NodeIterator ni = DocumentServiceImpl.getDocumentsOrderedByCreationDate(locationNode, order); ni.hasNext();) {
			documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode(), location));
		}
		return documents;
	}

	public WasabiValueDTO getEnvironment(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node documentNode = TransferManager.convertDTO2Node(document, s);
		Long optLockId = ObjectServiceImpl.getOptLockId(documentNode);
		return TransferManager.convertValue2DTO(DocumentServiceImpl.getEnvironment(documentNode), optLockId);
	}

	@Override
	public boolean hasDocumentsCreatedAfter(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		return DocumentServiceImpl.hasDocumentsCreatedAfter(environmentNode, timestamp);
	}

	@Override
	public boolean hasDocumentsCreatedBefore(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		return DocumentServiceImpl.hasDocumentsCreatedBefore(environmentNode, timestamp);
	}

	@Override
	public boolean hasDocumentsModifiedAfter(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		return DocumentServiceImpl.hasDocumentsModifiedAfter(environmentNode, timestamp);
	}

	@Override
	public boolean hasDocumentsModifiedBefore(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Node environmentNode = TransferManager.convertDTO2Node(environment, s);
		return DocumentServiceImpl.hasDocumentsModifiedBefore(environmentNode, timestamp);
	}

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException {
		Node documentNode = null;
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			documentNode = TransferManager.convertDTO2Node(document, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
			Locker.recognizeLockTokens(s, document, newEnvironment);
			Locker.acquireLock(documentNode, document, false, s, locker);
			Locker.checkOptLockId(documentNode, document, optLockId);
			DocumentServiceImpl.move(documentNode, newEnvironmentNode, callerPrincipal);
			s.save();
			EventCreator.createMovedEvent(documentNode, newEnvironmentNode, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(documentNode, document, s, locker);
		}
	}

	public void remove(WasabiDocumentDTO document) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException {
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			EventCreator.createRemovedEvent(documentNode, jms, callerPrincipal);
			Locker.recognizeLockTokens(s, document);
			DocumentServiceImpl.remove(documentNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public void rename(WasabiDocumentDTO document, String name, Long optLockId) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node documentNode = null;
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			documentNode = TransferManager.convertDTO2Node(document, s);
			Locker.recognizeLockTokens(s, document);
			Locker.acquireLock(documentNode, document, false, s, locker);
			Locker.checkOptLockId(documentNode, document, optLockId);
			DocumentServiceImpl.rename(documentNode, name, callerPrincipal);
			s.save();
			EventCreator.createPropertyChangedEvent(documentNode, WasabiProperty.NAME, name, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(documentNode, document, s, locker);
		}
	}

	public void setContent(WasabiDocumentDTO document, Serializable content, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException,
			ConcurrentModificationException, NoPermissionException {
		Node documentNode = null;
		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			documentNode = TransferManager.convertDTO2Node(document, s);
			
			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(documentNode, callerPrincipal, WasabiPermission.WRITE, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "DocumentService.setContent()",
							"WRITE", "document"));
			/* Authorization - End */
			
			Locker.recognizeLockTokens(s, document);
			Locker.acquireLock(documentNode, document, false, s, locker);
			Locker.checkOptLockId(documentNode, document, optLockId);
			DocumentServiceImpl.setContentPiped(documentNode, content, s, jms, sharedFilterBean, callerPrincipal);
			s.save();
			EventCreator
					.createPropertyChangedEvent(documentNode, WasabiProperty.CONTENT, content, jms, callerPrincipal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (TargetDoesNotExistException tdne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.PIPES_NOT_FOUND, tdne);
		} catch (LockingException e) {
			// cannot happen
		} finally {
			Locker.releaseLock(documentNode, document, s, locker);
		}
	}
}
