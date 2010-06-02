/* 
 * Copyright (C) 2007-2009 
 * Thorsten Hampel, Jonas Schulte, Daniel Buese,
 * Andreas Oberhoff, Thomas Bopp, and Robert Hinn
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

package de.wasabibeans.framework.server.core.internal.model.service.impl;

import java.io.Serializable;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import de.wasabibeans.framework.server.core.internal.model.service.DocumentServiceInternal;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@Stateless(name = "DocumentServiceInternal")
public class DocumentServiceInternalImpl extends ObjectServiceInternalImpl
		implements DocumentServiceInternal {

	public Node create(String name, Node environment) {
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null.");
		}
		// if (getDocumentByName(environment, name) != null) {
		// throw new EntityExistsException("Document " + name
		// + " already exist in " + environment.getName() + ".");
		// }
		try {
			Node document = environment.addNode(name);
			document.setPrimaryType(NodeType.NT_UNSTRUCTURED);
			environment.getSession().save();

			return document;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Serializable getContent(Node wasabiDocument) {
		try {
			return wasabiDocument.getProperty("content").getString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Node getDocumentByName(Node wasabiLocation, String name) {
		try {
			return wasabiLocation.getNode(name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	public NodeIterator getDocuments(Node wasabiLocation) {
		try {
			return wasabiLocation.getNodes();
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

	public Node getEnvironment(Node wasabiDocument) {
		try {
			return wasabiDocument.getParent();
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

	public void move(Node wasabiDocument, Node newEnvironment) {
		try {
			wasabiDocument.getSession().move(wasabiDocument.getPath(),
					newEnvironment.getPath() + "/" + wasabiDocument.getName());
			wasabiDocument.getSession().save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void remove(Node wasabiDocument) {
		try {
			Session s = wasabiDocument.getSession();
			wasabiDocument.remove();
			s.save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void rename(Node wasabiDocument, String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null.");
		}
		try {
			wasabiDocument.getSession().move(wasabiDocument.getPath(),
					wasabiDocument.getParent().getPath() + "/" + name);
			wasabiDocument.getSession().save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setContent(Node wasabiDocument, Serializable content) {
		try {
			wasabiDocument.setProperty("content", (String) content);
			wasabiDocument.getSession().save();
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
