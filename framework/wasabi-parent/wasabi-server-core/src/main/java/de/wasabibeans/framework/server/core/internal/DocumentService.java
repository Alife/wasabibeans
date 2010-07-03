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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ObjectDoesNotExistException {
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		Session s = getJCRSession();
		Node environmentNode = convertDTO2Node(environment, s);
		try {
			Node documentNode = environmentNode.addNode(WasabiNodeProperty.DOCUMENTS + "/" + name,
					WasabiNodeType.WASABI_DOCUMENT);
			s.save();

			return convertNode2DTO(documentNode);
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "document", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public Serializable getContent(WasabiDocumentDTO document) throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			Binary content = ((Property) documentNode.getPrimaryItem()).getBinary();
			ObjectInputStream oIn = new ObjectInputStream(content.getStream());
			return (Serializable) oIn.readObject();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new DocumentContentException(WasabiExceptionMessages.INTERNAL_DOCUMENT_CONTENT_LOAD, e);
		}
	}

	public void setContent(WasabiDocumentDTO document, final Serializable content)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);

		try {
			PipedInputStream pipedIn = new PipedInputStream();
			PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
			final ObjectOutputStream objectOut = new ObjectOutputStream(pipedOut);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						objectOut.writeObject(content);
					} catch (IOException io) {
						logger.error(WasabiExceptionMessages.INTERNAL_DOCUMENT_CONTENT_SAVE, io);
					}
				}
			}).start();

			Binary toSave = s.getValueFactory().createBinary(pipedIn);
			documentNode.setProperty(WasabiNodeProperty.CONTENT, toSave);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new DocumentContentException(WasabiExceptionMessages.INTERNAL_DOCUMENT_CONTENT_SAVE, io);
		}
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		Session s = getJCRSession();
		Node locationNode = convertDTO2Node(location, s);
		try {
			return convertNode2DTO(locationNode.getNode(WasabiNodeProperty.DOCUMENTS + "/" + name));
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node locationNode = convertDTO2Node(location, s);
		try {
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			NodeIterator iter = locationNode.getNode(WasabiNodeProperty.DOCUMENTS).getNodes();
			while (iter.hasNext()) {
				documents.add((WasabiDocumentDTO) convertNode2DTO(iter.nextNode()));
			}
			return documents;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			return convertNode2DTO(documentNode.getParent().getParent());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment) throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		Node newEnvironmentNode = convertDTO2Node(newEnvironment, s);
		try {
			s.move(documentNode.getPath(), newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/"
					+ documentNode.getName());
			s.save();
		} catch (ItemExistsException iee) {
			try {
				String name = documentNode.getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "document", name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public void remove(WasabiDocumentDTO document) throws ObjectDoesNotExistException, UnexpectedInternalProblemException {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			documentNode.remove();
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public void rename(WasabiDocumentDTO document, String name) throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		if (name == null) {
			logger.error(WasabiExceptionMessages.INTERNAL_NAME_NULL);
			throw new IllegalArgumentException(WasabiExceptionMessages.INTERNAL_NAME_NULL);
		}
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			s.move(documentNode.getPath(), documentNode.getParent().getPath() + "/" + name);
			s.save();
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "document", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
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

	@Override
	public boolean exists(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WasabiUserDTO getCreatedBy(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreatedOn(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiUserDTO getModifiedBy(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getModifiedOn(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRightsActive(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCreatedBy(WasabiObjectDTO object, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCreatedOn(WasabiObjectDTO object, Date creationTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setModifiedBy(WasabiObjectDTO object, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setModifiedOn(WasabiObjectDTO object, Date modificationTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRightsActive(WasabiObjectDTO object, boolean rightsActive) {
		// TODO Auto-generated method stub

	}
}
