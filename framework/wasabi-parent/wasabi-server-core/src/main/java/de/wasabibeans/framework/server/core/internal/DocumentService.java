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
import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

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

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO locationDTO) {
		Node locationNode = convertDTO2Node(locationDTO);
		try {
			Vector<WasabiDocumentDTO> documents = new Vector<WasabiDocumentDTO>();
			NodeIterator iter = locationNode.getNodes();
			while (iter.hasNext()) {
				documents.add((WasabiDocumentDTO) convertNode2DTO(iter.nextNode()));
			}
			return documents;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO documentDTO) {
		Node documentNode = convertDTO2Node(documentDTO);
		try {
			return convertNode2DTO(documentNode.getParent());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

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
			documentNode.getSession().move(documentNode.getPath(), documentNode.getParent().getPath() + "/" + name);
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
