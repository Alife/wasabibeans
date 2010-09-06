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
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ObjectDoesNotExistException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Node documentNode = DocumentServiceImpl
					.create(name, environmentNode, s, ctx.getCallerPrincipal().getName());
			s.save();
			return TransferManager.convertNode2DTO(documentNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	public WasabiValueDTO getContent(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(documentNode);
			return TransferManager.convertValue2DTO(DocumentServiceImpl.getContent(documentNode), optLockId);
		} finally {
			s.logout();
		}
	}

	public void setContent(WasabiDocumentDTO document, Serializable content, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException,
			ConcurrentModificationException {
		Node documentNode = null;
		Session s = jcr.getJCRSession();
		try {
			documentNode = TransferManager.convertDTO2Node(document, s);
			Locker.acquireLock(documentNode, document, optLockId, s, locker);
			DocumentServiceImpl.setContent(documentNode, content, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(documentNode, s, locker);
			s.logout();
		}
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			return TransferManager.convertNode2DTO(DocumentServiceImpl.getDocumentByName(locationNode, name));
		} finally {
			s.logout();
		}
	}

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			NodeIterator ni = DocumentServiceImpl.getDocuments(locationNode);
			while (ni.hasNext()) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	public WasabiValueDTO getEnvironment(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			Long optLockId = ObjectServiceImpl.getOptLockId(documentNode);
			return TransferManager.convertValue2DTO(DocumentServiceImpl.getEnvironment(documentNode), optLockId);
		} finally {
			s.logout();
		}
	}

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException {
		Node documentNode = null;
		Session s = jcr.getJCRSession();
		try {
			documentNode = TransferManager.convertDTO2Node(document, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
			Locker.acquireLock(documentNode, document, optLockId, s, locker);
			DocumentServiceImpl.move(documentNode, newEnvironmentNode, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(documentNode, s, locker);
			s.logout();
		}
	}

	public void remove(WasabiDocumentDTO document) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			DocumentServiceImpl.remove(documentNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	public void rename(WasabiDocumentDTO document, String name, Long optLockId) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Node documentNode = null;
		Session s = jcr.getJCRSession();
		try {
			documentNode = TransferManager.convertDTO2Node(document, s);
			Locker.acquireLock(documentNode, document, optLockId, s, locker);
			DocumentServiceImpl.rename(documentNode, name, ctx.getCallerPrincipal().getName());
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(documentNode, s, locker);
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl.getDocumentsByCreationDate(environmentNode, startDate, endDate)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl.getDocumentsByCreationDate(environmentNode, startDate, endDate,
					depth)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (NodeIterator ni = DocumentServiceImpl.getDocumentsByCreator(creatorNode); ni.hasNext();) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node creatorNode = TransferManager.convertDTO2Node(creator, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl.getDocumentsByCreator(creatorNode, environmentNode)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl
					.getDocumentsByModificationDate(environmentNode, startDate, endDate)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl.getDocumentsByModificationDate(environmentNode, startDate,
					endDate, depth)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (NodeIterator ni = DocumentServiceImpl.getDocumentsByModifier(modifierNode); ni.hasNext();) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node modifierNode = TransferManager.convertDTO2Node(modifier, s);
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (Node document : DocumentServiceImpl.getDocumentsByModifier(modifierNode, environmentNode)) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(document));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node locationNode = TransferManager.convertDTO2Node(location, s);
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			for (NodeIterator ni = DocumentServiceImpl.getDocumentsOrderedByCreationDate(locationNode, order); ni
					.hasNext();) {
				documents.add((WasabiDocumentDTO) TransferManager.convertNode2DTO(ni.nextNode()));
			}
			return documents;
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean hasDocumentsCreatedAfter(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			return DocumentServiceImpl.hasDocumentsCreatedAfter(environmentNode, timestamp);
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean hasDocumentsCreatedBefore(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			return DocumentServiceImpl.hasDocumentsCreatedBefore(environmentNode, timestamp);
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean hasDocumentsModifiedAfter(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			return DocumentServiceImpl.hasDocumentsModifiedAfter(environmentNode, timestamp);
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean hasDocumentsModifiedBefore(WasabiLocationDTO environment, Long timestamp)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			return DocumentServiceImpl.hasDocumentsModifiedBefore(environmentNode, timestamp);
		} finally {
			s.logout();
		}
	}
}
