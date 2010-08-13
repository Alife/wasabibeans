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

import javax.annotation.Resource;
import javax.ejb.SessionContext;
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
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

	@Resource
	private SessionContext sessionContext;

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node environmentNode = TransferManager.convertDTO2Node(environment, s);
			Node documentNode = DocumentServiceImpl.create(name, environmentNode, sessionContext.getCallerPrincipal()
					.getName(), s);
			s.save();
			return TransferManager.convertNode2DTO(documentNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE + "::"
					+ re.toString(), re);
		} finally {
			s.logout();
		}
	}

	public Serializable getContent(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			return DocumentServiceImpl.getContent(documentNode);
		} finally {
			s.logout();
		}
	}

	public void setContent(WasabiDocumentDTO document, Serializable content) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			DocumentServiceImpl.setContent(documentNode, content);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
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

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			return TransferManager.convertNode2DTO(DocumentServiceImpl.getEnvironment(documentNode));
		} finally {
			s.logout();
		}
	}

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			Node newEnvironmentNode = TransferManager.convertDTO2Node(newEnvironment, s);
			DocumentServiceImpl.move(documentNode, newEnvironmentNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
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

	public void rename(WasabiDocumentDTO document, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		Session s = jcr.getJCRSession();
		try {
			Node documentNode = TransferManager.convertDTO2Node(document, s);
			DocumentServiceImpl.rename(documentNode, name);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			s.logout();
		}
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsOrderedByCreationDate(WasabiLocationDTO location, SortType order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasDocumentsCreatedAfter(WasabiLocationDTO environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasDocumentsCreatedBefore(WasabiLocationDTO environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasDocumentsModifiedAfter(WasabiLocationDTO environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasDocumentsModifiedBefore(WasabiLocationDTO environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}
}
