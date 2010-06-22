/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
public class DocumentService extends ObjectService
		implements DocumentServiceLocal, DocumentServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environmentDTO) {
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null.");
		}
		// if (getDocumentByName(environment, name) != null) {
		// throw new EntityExistsException("Document " + name
		// + " already exist in " + environment.getName() + ".");
		// }
		Node environmentNode = convertDTO2Node(environmentDTO);
		try {
			Node documentNode = environmentNode.addNode(name);
			documentNode.setPrimaryType(NodeType.NT_UNSTRUCTURED);
			environmentNode.getSession().save();

			return convertNode2DTO(documentNode);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Serializable getContent(WasabiDocumentDTO documentDTO) {
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			return documentNode.getProperty("content").getString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO locationDTO, String name) {
		Node locationNode = convertDTO2Node(locationDTO);
		try {
			return convertNode2DTO(locationNode.getNode(name));
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<WasabiDocumentDTO> getDocuments(WasabiLocationDTO locationDTO) {
		Node locationNode = convertDTO2Node(locationDTO);
		try {
			ArrayList<WasabiDocumentDTO> documents = new ArrayList<WasabiDocumentDTO>();
			NodeIterator iter = locationNode.getNodes();
			while (iter.hasNext()) {
				documents.add((WasabiDocumentDTO) convertNode2DTO(iter.nextNode()));
			}
			return documents;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByCreationDate(
	// WasabiLocation environment, Date startDate, Date endDate) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByCreationDateAndEnvironment");
	// // "start" and "end" must be long values, since they are stored in the
	// // database as long types for higher precision.
	// query.setParameter("start", startDate.getTime());
	// query.setParameter("end", endDate.getTime());
	// query.setParameter("environment", environment);
	//
	// return query.getResultList();
	// }

	// @Override
	// public Collection<WasabiDocument> getDocumentsByCreationDate(
	// WasabiLocation environment, Date startDate, Date endDate, int depth) {
	// Collection<WasabiDocument> allDocumentsByCreationDate = new
	// HashSet<WasabiDocument>();
	// allDocumentsByCreationDate.addAll(getDocumentsByCreationDate(
	// environment, startDate, endDate));
	// Collection<WasabiLocation> subLocations = new HashSet<WasabiLocation>();
	//
	// subLocations.addAll(environment.getContainers());
	//
	// if (environment.getClass() == WasabiRoom.class) {
	// subLocations.addAll(((WasabiRoom) environment).getRooms());
	// }
	//
	// for (WasabiLocation subLocation : subLocations) {
	// if (depth > 0) {
	// allDocumentsByCreationDate.addAll(getDocumentsByCreationDate(
	// subLocation, startDate, endDate, depth - 1));
	// } else if (depth < 0) {
	// allDocumentsByCreationDate.addAll(getDocumentsByCreationDate(
	// subLocation, startDate, endDate, depth));
	// }
	// }
	//
	// return allDocumentsByCreationDate;
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByCreator(WasabiUser
	// creator) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByCreator");
	// query.setParameter("creator", creator);
	//
	// return query.getResultList();
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByCreator(WasabiUser
	// creator,
	// WasabiLocation environment) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByCreatorAndEnvironment");
	// query.setParameter("creator", creator);
	// query.setParameter("environment", environment);
	//
	// return query.getResultList();
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByModificationDate(
	// WasabiLocation environment, Date startDate, Date endDate) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByModificationDateAndEnvironment");
	// // "start" and "end" must be long values, since they are stored in the
	// // database as long types for higher precision.
	// query.setParameter("start", startDate.getTime());
	// query.setParameter("end", endDate.getTime());
	// query.setParameter("environment", environment);
	//
	// return query.getResultList();
	// }

	// @Override
	// public Collection<WasabiDocument> getDocumentsByModificationDate(
	// WasabiLocation environment, Date startDate, Date endDate, int depth) {
	// Collection<WasabiDocument> allDocumentsByModificationDate = new
	// HashSet<WasabiDocument>();
	// allDocumentsByModificationDate.addAll(getDocumentsByModificationDate(
	// environment, startDate, endDate));
	// Collection<WasabiLocation> subLocations = new HashSet<WasabiLocation>();
	//
	// subLocations.addAll(environment.getContainers());
	//
	// if (environment.getClass() == WasabiRoom.class) {
	// subLocations.addAll(((WasabiRoom) environment).getRooms());
	// }
	//
	// for (WasabiLocation subLocation : subLocations) {
	// if (depth > 0) {
	// allDocumentsByModificationDate
	// .addAll(getDocumentsByModificationDate(subLocation,
	// startDate, endDate, depth - 1));
	// } else if (depth < 0) {
	// allDocumentsByModificationDate
	// .addAll(getDocumentsByModificationDate(subLocation,
	// startDate, endDate, depth));
	// }
	// }
	//
	// return allDocumentsByModificationDate;
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByModifier(WasabiUser
	// modifier) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByModifier");
	// query.setParameter("modifier", modifier);
	//
	// return query.getResultList();
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsByModifier(
	// WasabiUser modifier, WasabiLocation environment) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsByModifierAndEnvironment");
	// query.setParameter("modifier", modifier);
	// query.setParameter("environment", environment);
	//
	// return query.getResultList();
	// }

	// @WasabiResultFilter(type = FilterType.VIEW)
	// @SuppressWarnings("unchecked")
	// @Override
	// public Collection<WasabiDocument> getDocumentsOrderedByCreationDate(
	// WasabiLocation wasabiLocation, SortType order) {
	// if (order == SortType.DESCENDING) {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsOrderedByCreationDateDESC");
	// query.setParameter("environment", wasabiLocation);
	// return query.getResultList();
	//
	// } else {
	// Query query = entityManager
	// .createNamedQuery("WasabiDocument.getDocumentsOrderedByCreationDateASC");
	// query.setParameter("environment", wasabiLocation);
	// return query.getResultList();
	// }
	// }

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO documentDTO) {
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			return convertNode2DTO(documentNode.getParent());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// @Override
	// public boolean hasDocumentsCreatedAfter(WasabiLocation environment,
	// Long timestamp) {
	// if (getDocumentsByCreationDate(environment, new Date(timestamp),
	// new Date(Long.MAX_VALUE)).isEmpty()) {
	// return false;
	// } else {
	// return true;
	// }
	// }

	// @Override
	// public boolean hasDocumentsCreatedBefore(WasabiLocation environment,
	// Long timestamp) {
	// if (getDocumentsByCreationDate(environment, new Date(Long.MIN_VALUE),
	// new Date(timestamp)).isEmpty()) {
	// return false;
	// } else {
	// return true;
	// }
	// }

	// @Override
	// public boolean hasDocumentsModifiedAfter(WasabiLocation environment,
	// Long timestamp) {
	// if (getDocumentsByModificationDate(environment, new Date(timestamp),
	// new Date(Long.MAX_VALUE)).isEmpty()) {
	// return false;
	// } else {
	// return true;
	// }
	// }

	// @Override
	// public boolean hasDocumentsModifiedBefore(WasabiLocation environment,
	// Long timestamp) {
	// if (getDocumentsByModificationDate(environment,
	// new Date(Long.MIN_VALUE), new Date(timestamp)).isEmpty()) {
	// return false;
	// } else {
	// return true;
	// }
	// }

	public void move(WasabiDocumentDTO documentDTO, WasabiLocationDTO newEnvironmentDTO) {
		Node documentNode = convertDTO2Node(documentDTO);
		Node newEnvironmentNode = convertDTO2Node(newEnvironmentDTO);
		try {
			documentNode.getSession().move(documentNode.getPath(),
					newEnvironmentNode.getPath() + "/" + documentNode.getName());
			documentNode.getSession().save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void remove(WasabiDocumentDTO documentDTO) {
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			Session s = documentNode.getSession();
			documentNode.remove();
			s.save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void rename(WasabiDocumentDTO documentDTO, String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null.");
		}
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			documentNode.getSession().move(documentNode.getPath(),
					documentNode.getParent().getPath() + "/" + name);
			documentNode.getSession().save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setContent(WasabiDocumentDTO documentDTO, Serializable content) {
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			documentNode.setProperty("content", (String) content);
			documentNode.getSession().save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// /*
	// * (non-Javadoc)
	// * @see
	// org.wasabibeans.server.core.internal.model.service.DocumentServiceInternal#copy(org.wasabibeans.server.core.internal.model.WasabiDocument,
	// org.wasabibeans.server.core.internal.model.WasabiLocation)
	// */
	// @Override
	// public void copy(WasabiDocument wasabiDocument,
	// WasabiLocation destination) {
	// WasabiDocument copy = create(wasabiDocument.getName(), destination);
	// entityManager.flush();
	// destination.getDocuments().add(copy);
	//		
	// for (Iterator<WasabiAttribute> iterator =
	// wasabiDocument.getAttributes().iterator(); iterator
	// .hasNext();) {
	// WasabiAttribute wasabiAttribute = (WasabiAttribute) iterator.next();
	//			
	// }
	//			
	// wasabiDocument.getEnvironment().getDocuments().remove(wasabiDocument);
	// wasabiDocument.setEnvironment(newEnvironment);
	// entityManager.flush();
	// newEnvironment.getDocuments().add(wasabiDocument);
	// }
}
