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
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ContentNotSerializableException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

/**
 * Class, that implements the internal access on WasabiDocuments objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "DocumentService")
public class DocumentService extends ObjectService implements DocumentServiceLocal, DocumentServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment) {
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
			String msg = "Document " + name + " already exists.";
			logger.warn(msg, iee);
			throw new ObjectAlreadyExistsException(msg, name);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public Serializable getContent(WasabiDocumentDTO document) {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			Value value = ((Property) documentNode.getPrimaryItem()).getValue();
			switch (value.getType()) {
			case PropertyType.BINARY:
				try {
					byte[] bytes = new byte[(int) value.getBinary().getSize()];
					value.getBinary().read(bytes, 0);
					return bytes;
				} catch (Exception e) {
					String msg = "The content of the document " + documentNode.getName() + " cannot be serialized.";
					logger.warn(msg);
					throw new ContentNotSerializableException(msg, documentNode.getName());
				}
			case PropertyType.BOOLEAN:
				return value.getBoolean();
			case PropertyType.DATE:
				return value.getDate();
			case PropertyType.DECIMAL:
				return value.getDecimal();
			case PropertyType.DOUBLE:
				return value.getDouble();
			case PropertyType.LONG:
				return value.getLong();
			case PropertyType.NAME:
				return value.getString();
			case PropertyType.PATH:
				return value.getString();
			case PropertyType.REFERENCE:
				return value.getString();
			case PropertyType.STRING:
				return value.getString();
			case PropertyType.URI:
				return value.getString();
			case PropertyType.WEAKREFERENCE:
				return value.getString();
			default:
				return null;
			}
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public void setContent(WasabiDocumentDTO document, Serializable content) {
		// Session s = getJCRSession();
		// Node documentNode = convertDTO2Node(documentDTO, s);
		// Binary bin; InputStream in = new InputStream();
		// ByteArrayInputStream baos = new ByteArrayOutputStream();
		// ObjectOutputStream pf =new ObjectOutputStream(baos).writeObject(content);
		// s.getValueFactory().
		// // stream closed in the finally
		// out = new ObjectOutputStream JavaDoc(outputStream);
		// out.writeObject(obj);
		//		              
		// } catch (IOException JavaDoc ex) {
		// throw new SerializationException(ex);
		// } finally {
		// try {
		// if (out != null) {
		// out.close();
		// }
		// } catch (IOException JavaDoc ex) {
		// // ignore;
		// }
		// }
		//
		// Read more: http://kickjava.com/src/org/apache/commons/lang/SerializationUtils.java.htm#ixzz0rrpAh7C1
		//
		//		
		//		
		// try {
		// Property currentContent = (Property) documentNode.getPrimaryItem();
		// if (content instanceof String) {
		// currentContent.setValue((String) content);
		// } else if (content instanceof Date) {
		// Calendar cal = Calendar.getInstance();
		// cal.setTime((Date) content);
		// currentContent.setValue(cal);
		// } else if (content instanceof Calendar) {
		// currentContent.setValue((Calendar) content);
		// } else if (content instanceof Double) {
		// currentContent.setValue((Double) content);
		// } else if (content instanceof Boolean) {
		// currentContent.setValue((Boolean) content);
		// } else if (content instanceof BigDecimal) {
		// currentContent.setValue((BigDecimal) content);
		// } else if (content instanceof Long) {
		// currentContent.setValue((Long) content);
		// } else {
		// }
		// s.save();
		// } catch (RepositoryException re) {
		// logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		// throw new RuntimeException(re);
		// }
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name) {
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
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location) {
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
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document) {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			return convertNode2DTO(documentNode.getParent().getParent());
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment) {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		Node newEnvironmentNode = convertDTO2Node(newEnvironment, s);
		try {
			s.move(documentNode.getPath(), newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/"
					+ documentNode.getName());
			s.save();
		} catch (ItemExistsException iee) {
			try {
				String msg = "Document " + documentNode.getName() + " already exists.";
				logger.warn(msg, iee);
				throw new ObjectAlreadyExistsException(msg, documentNode.getName());
			} catch (RepositoryException re) {
				logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
				throw new RuntimeException(re);
			}
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public void remove(WasabiDocumentDTO document) {
		Session s = getJCRSession();
		Node documentNode = convertDTO2Node(document, s);
		try {
			documentNode.remove();
			s.save();
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	public void rename(WasabiDocumentDTO document, String name) {
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
			String msg = "Document " + name + " already exists.";
			logger.warn(msg, iee);
			throw new ObjectAlreadyExistsException(msg, name);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
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
