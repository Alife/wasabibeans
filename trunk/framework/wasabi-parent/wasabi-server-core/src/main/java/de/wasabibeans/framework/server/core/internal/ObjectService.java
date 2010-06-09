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

package de.wasabibeans.framework.server.core.internal;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.local.ObjectServiceLocal;
import de.wasabibeans.framework.server.core.remote.ObjectServiceRemote;

/**
 * Class, that implements the internal access on WasabiObject objects.
 */
@Stateless(name = "ObjectService")
public class ObjectService extends TransferManager implements ObjectServiceLocal, ObjectServiceRemote {


//	Logger log = Logger.getLogger(ObjectServiceInternal.class);
//
//	@Override
//	public boolean exists(WasabiObject wasabiObject) {
//		return entityManager.contains(wasabiObject);
//	}
//
//	@Override
//	public WasabiUser getCreatedBy(WasabiObject wasabiObject) {
//		return wasabiObject.getCreatedBy();
//	}
//
//	@Override
//	public Date getCreatedOn(WasabiObject wasabiObject) {
//		return new Date(wasabiObject.getCreatedOn());
//	}
//
//	@Override
//	public WasabiUser getModifiedBy(WasabiObject wasabiObject) {
//		return wasabiObject.getModifiedBy();
//	}
//
//	@Override
//	public Date getModifiedOn(WasabiObject wasabiObject) {
//		return new Date(wasabiObject.getModifiedOn());
//	}

	public String getName(WasabiObjectDTO objectDTO) {
		Node objectNode = convertDTO2Node(objectDTO);
		if (objectNode == null) {
			return "";
		}
		try {
			return objectNode.getName();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

//	@WasabiResultFilter(type = FilterType.VIEW)
//	@SuppressWarnings("unchecked")
//	@Override
//	public Collection<WasabiObject> getObjectsByAttributeName(
//			String attributeName) {
//		Query query = entityManager
//				.createNamedQuery("WasabiObject.getObjectsByAttributeName");
//		query.setParameter("attributeName", attributeName);
//
//		return (Collection<WasabiObject>) query.getResultList();
//	}
//
//	@WasabiResultFilter(type = FilterType.VIEW)
//	@SuppressWarnings("unchecked")
//	@Override
//	public Collection<WasabiObject> getObjectsByCreator(WasabiUser creator) {
//		Query query = entityManager
//				.createNamedQuery("WasabiObject.getObjectsByCreator");
//		query.setParameter("creator", creator);
//
//		return (Collection<WasabiObject>) query.getResultList();
//	}
//
//	@WasabiResultFilter(type = FilterType.VIEW)
//	@SuppressWarnings("unchecked")
//	@Override
//	public Collection<WasabiObject> getObjectsByModifier(WasabiUser modifier) {
//		Query query = entityManager
//				.createNamedQuery("WasabiObject.getObjectsByModifier");
//		query.setParameter("modifier", modifier);
//
//		return (Collection<WasabiObject>) query.getResultList();
//	}
//
//	@WasabiResultFilter(type = FilterType.VIEW)
//	@SuppressWarnings("unchecked")
//	@Override
//	public Collection<WasabiObject> getObjectsByName(String name) {
//		Query query = entityManager
//				.createNamedQuery("WasabiObject.getObjectsByName");
//		query.setParameter("name", name);
//
//		return (Collection<WasabiObject>) query.getResultList();
//	}

	public String getUUID(WasabiObjectDTO objectDTO) {
		Node objectNode = convertDTO2Node(objectDTO);
		if (objectNode == null) {
			return "";
		}
		try {
			return objectNode.getIdentifier();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

//	@Override
//	public boolean removeSubObjects(WasabiObject wasabiObject) {
//
//		boolean success = true;
//
//		if (wasabiObject.getType().getSuperclass() == WasabiLocation.class) {
//
//			WasabiLocation location;
//
//			location = (WasabiLocation) wasabiObject;
//
//			Collection<WasabiContainer> containers = location.getContainers();
//			Collection<WasabiContainer> removeContainers = new Vector<WasabiContainer>();
//
//			for (WasabiContainer wasabiContainer : containers) {
//				if (removeSubObjects(wasabiContainer) == true) {
//					removeContainers.add(wasabiContainer);
//				}
//			}
//
//			for (WasabiContainer wasabiContainer : removeContainers) {
//				containers.remove(wasabiContainer);
//				entityManager.remove(wasabiContainer);
//			}
//
//			if (!containers.isEmpty()) {
//				success = false;
//			}
//
//			Collection<WasabiDocument> documents = location.getDocuments();
//			Collection<WasabiDocument> removeDocuments = new Vector<WasabiDocument>();
//
//			for (WasabiDocument wasabiDocument : documents) {
//				if (removeSubObjects(wasabiDocument)) {
//					removeDocuments.add(wasabiDocument);
//				}
//			}
//
//			for (WasabiDocument wasabiDocument : removeDocuments) {
//				documents.remove(wasabiDocument);
//				entityManager.remove(wasabiDocument);
//			}
//
//			if (!documents.isEmpty()) {
//				success = false;
//			}
//
//			Collection<WasabiLink> links = location.getLinks();
//			Collection<WasabiLink> removeLinks = new Vector<WasabiLink>();
//
//			for (WasabiLink wasabiLink : links) {
//				if (removeSubObjects(wasabiLink)) {
//					removeLinks.add(wasabiLink);
//				}
//			}
//
//			for (WasabiLink wasabiLink : removeLinks) {
//				links.remove(wasabiLink);
//				entityManager.remove(wasabiLink);
//			}
//
//			if (!links.isEmpty()) {
//				success = false;
//			}
//
//			if (wasabiObject.isType(WasabiRoom.class)) {
//				WasabiRoom room;
//
//				room = (WasabiRoom) wasabiObject;
//
//				Collection<WasabiRoom> rooms = room.getRooms();
//				Collection<WasabiRoom> removeRooms = new Vector<WasabiRoom>();
//
//				for (WasabiRoom wasabiRoom : rooms) {
//					if (removeSubObjects(wasabiRoom)) {
//						removeRooms.add(wasabiRoom);
//					}
//				}
//
//				for (WasabiRoom wasabiRoom : removeRooms) {
//					rooms.remove(wasabiRoom);
//					entityManager.remove(wasabiRoom);
//				}
//
//				if (!rooms.isEmpty()) {
//					success = false;
//				}
//			}
//		} else if (wasabiObject.isType(WasabiGroup.class)) {
//			WasabiGroup group;
//			group = (WasabiGroup) wasabiObject;
//
//			Collection<WasabiGroup> groups = group.getSubGroups();
//			Collection<WasabiGroup> removeGroups = new Vector<WasabiGroup>();
//
//			for (WasabiGroup wasabiGroup : groups) {
//				if (removeSubObjects(wasabiGroup)) {
//					removeGroups.add(wasabiGroup);
//				}
//			}
//
//			for (WasabiGroup wasabiGroup : removeGroups) {
//				groups.remove(wasabiGroup);
//				entityManager.remove(wasabiGroup);
//			}
//
//			if (!groups.isEmpty()) {
//				success = false;
//			}
//		}
//
//		Collection<WasabiAttribute> attributes = wasabiObject.getAttributes();
//		Collection<WasabiAttribute> removeAttributes = new Vector<WasabiAttribute>();
//
//		for (WasabiAttribute wasabiAttribute : attributes) {
//			if (removeSubObjects(wasabiAttribute)) {
//				removeAttributes.add(wasabiAttribute);
//			}
//		}
//
//		for (WasabiAttribute wasabiAttribute : removeAttributes) {
//			attributes.remove(wasabiAttribute);
//			entityManager.remove(wasabiAttribute);
//		}
//
//		if (!attributes.isEmpty()) {
//			success = false;
//		}
//
//		if ((success == true)
//				&& (authorizationService.hasPermission(wasabiObject,
//						WasabiPermission.WRITE))) {
//			return true;
//		}
//
//		return false;
//
//	}
//
//	@Override
//	public void setCreatedBy(WasabiObject wasabiObject, WasabiUser wasabiUser) {
//		wasabiObject.setCreatedBy(wasabiUser);
//	}
//
//	@Override
//	public void setCreatedOn(WasabiObject wasabiObject, Date creationTime) {
//		wasabiObject.setCreatedOn(creationTime.getTime());
//	}
//
//	@Override
//	public void setModifiedBy(WasabiObject wasabiObject, WasabiUser wasabiUser) {
//		wasabiObject.setModifiedBy(wasabiUser);
//	}
//
//	@Override
//	public void setModifiedOn(WasabiObject wasabiObject, Date modificationTime) {
//		wasabiObject.setModifiedOn(modificationTime.getTime());
//	}
//
//	@Override
//	public boolean isRightsActive(WasabiObject wasabiObject) {
//		return wasabiObject.isRightsActive();
//	}
//
//	@Override
//	public void setRightsActive(WasabiObject wasabiObject, boolean rightsActive) {
//		wasabiObject.setRightsActive(rightsActive);		
//	}

}
