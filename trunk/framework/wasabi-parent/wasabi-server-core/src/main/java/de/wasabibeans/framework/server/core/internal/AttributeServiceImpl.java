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
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.authorization.WasabiAttributeACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.AttributeValueException;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.EmptyNodeIterator;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class AttributeServiceImpl {

	public static Node create(String name, Serializable value, Node affiliationNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			AttributeValueException, ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			Node attributeNode;
			if (affiliationNode.hasNode(WasabiNodeProperty.ATTRIBUTES)) {
				attributeNode = affiliationNode.addNode(WasabiNodeProperty.ATTRIBUTES + "/" + name,
						WasabiNodeType.ATTRIBUTE);
			} else {
				Node attributes = affiliationNode.addNode(WasabiNodeProperty.ATTRIBUTES,
						WasabiNodeType.OBJECT_COLLECTION);
				attributeNode = attributes.addNode(name, WasabiNodeType.ATTRIBUTE);
			}
			setValue(attributeNode, value, s, false, null);
			ObjectServiceImpl.created(attributeNode, s, false, callerPrincipal, true);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiAttributeACL.ACLEntryForCreate(attributeNode);
				WasabiAttributeACL.ACLEntryTemplateForCreate(attributeNode, affiliationNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return attributeNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name), iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node create(String name, Node valueNode, Node affiliationNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			ConcurrentModificationException, ObjectDoesNotExistException {
		try {
			Node attributeNode;
			if (affiliationNode.hasNode(WasabiNodeProperty.ATTRIBUTES)) {
				attributeNode = affiliationNode.addNode(WasabiNodeProperty.ATTRIBUTES + "/" + name,
						WasabiNodeType.ATTRIBUTE);
			} else {
				Node attributes = affiliationNode.addNode(WasabiNodeProperty.ATTRIBUTES,
						WasabiNodeType.OBJECT_COLLECTION);
				attributeNode = attributes.addNode(name, WasabiNodeType.ATTRIBUTE);
			}
			setWasabiValue(attributeNode, valueNode, s, false, null);
			ObjectServiceImpl.created(attributeNode, s, false, callerPrincipal, true);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiAttributeACL.ACLEntryForCreate(attributeNode);
				WasabiAttributeACL.ACLEntryTemplateForCreate(attributeNode, affiliationNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return attributeNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_NAME, name), iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getAffiliation(Node attributeNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(attributeNode);
	}

	public static Node getAttributeByName(Node objectNode, String name) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getNode(WasabiNodeProperty.ATTRIBUTES + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getAttributeType(Node attributeNode) throws UnexpectedInternalProblemException {
		try {
			return attributeNode.getProperty(WasabiNodeProperty.TYPE).getString();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getAttributes(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getNode(WasabiNodeProperty.ATTRIBUTES).getNodes();
		} catch (PathNotFoundException pnfe) {
			return new EmptyNodeIterator();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getAttributeById(String id, Session s) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		try {
			return s.getNodeByIdentifier(id);
		} catch (ItemNotFoundException infe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages
					.get(WasabiExceptionMessages.OBJECT_DNE_ID, id), infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T getValue(Class<T> type, Node attributeNode)
			throws UnexpectedInternalProblemException, AttributeValueException {
		try {
			String actualAttributeType = getAttributeType(attributeNode);
			if (actualAttributeType == null) {
				return null;
			}
			if (actualAttributeType.equals(type.getName())) {
				Property property = attributeNode.getProperty(WasabiNodeProperty.VALUE);
				if (type == String.class) {
					return (T) property.getString();
				} else if (type == Boolean.class) {
					return (T) new Boolean(property.getBoolean());
				} else if (type == Character.class) {
					return (T) new Character(property.getString().charAt(0));
				} else if (type == Byte.class) {
					return (T) new Byte((byte) property.getLong());
				} else if (type == Short.class) {
					return (T) new Short((short) property.getLong());
				} else if (type == Integer.class) {
					return (T) new Integer((int) property.getLong());
				} else if (type == Long.class) {
					return (T) new Long(property.getLong());
				} else if (type == Float.class) {
					return (T) new Float((float) property.getDouble());
				} else if (type == Double.class) {
					return (T) new Double(property.getDouble());
				} else if (type == Date.class) {
					return (T) property.getDate().getTime();
				} else { // get arbitrary serializable value
					Binary content = property.getBinary();
					ObjectInputStream oIn = new ObjectInputStream(content.getStream());
					return (T) oIn.readObject();
				}
			} else {
				throw new AttributeValueException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_WRONG_TYPE,
						actualAttributeType));
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new AttributeValueException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_LOAD, "value",
					"attribute"), io);
		} catch (ClassNotFoundException cnfe) {
			throw new AttributeValueException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_LOAD, "value",
					"attribute"), cnfe);
		}
	}

	public static Node getWasabiValue(Node attributeNode) throws TargetDoesNotExistException,
			UnexpectedInternalProblemException, AttributeValueException {
		try {
			String actualAttributeType = getAttributeType(attributeNode);
			if (actualAttributeType == null) {
				return null;
			}
			if (actualAttributeType.contains(WasabiConstants.JCR_NS_PREFIX)) {
				return attributeNode.getProperty(WasabiNodeProperty.VALUE).getNode();
			} else {
				throw new AttributeValueException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_WRONG_TYPE,
						actualAttributeType));
			}
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void move(Node attributeNode, Node newAffiliationNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		try {
			if (newAffiliationNode.hasNode(WasabiNodeProperty.ATTRIBUTES)) {
				s.move(attributeNode.getPath(), newAffiliationNode.getPath() + "/" + WasabiNodeProperty.ATTRIBUTES
						+ "/" + attributeNode.getName());
			} else {
				Node attributes = newAffiliationNode.addNode(WasabiNodeProperty.ATTRIBUTES,
						WasabiNodeType.OBJECT_COLLECTION);
				s.move(attributeNode.getPath(), attributes.getPath() + "/" + attributeNode.getName());
			}
			ObjectServiceImpl.modified(attributeNode, s, false, callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiAttributeACL.ACLEntryForMove(attributeNode);
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.OBJECT_ALREADY_EXISTS_ENV, iee);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node attributeNode, Session s, boolean doJcrSave, boolean throwEvents, JmsConnector jms,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			ObjectServiceImpl.removeRecursive(attributeNode, s, true, jms, callerPrincipal);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node attributeNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		ObjectServiceImpl.rename(attributeNode, name, s, doJcrSave, callerPrincipal);
	}

	public static void setValue(Node attributeNode, Serializable value, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, AttributeValueException,
			ConcurrentModificationException {
		try {
			if (value == null || value instanceof String) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, (String) value);
			} else if (value instanceof Boolean) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, (Boolean) value);
			} else if (value instanceof Character) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, ((Character) value).toString());
			} else if (value instanceof Byte) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, ((Byte) value).longValue());
			} else if (value instanceof Short) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, ((Short) value).longValue());
			} else if (value instanceof Integer) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, ((Integer) value).longValue());
			} else if (value instanceof Long) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, (Long) value);
			} else if (value instanceof Float) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, ((Float) value).doubleValue());
			} else if (value instanceof Double) {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, (Double) value);
			} else if (value instanceof Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) value);
				attributeNode.setProperty(WasabiNodeProperty.VALUE, cal);
			} else { // store arbitrary serializable data as javax.jcr.Binary
				PipedInputStream pipedIn = new PipedInputStream();
				PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
				ObjectOutputStream objectOut = new ObjectOutputStream(pipedOut);
				IOException exception = null;

				ObjectOutputStreamThread oost = new ObjectOutputStreamThread(objectOut, value, exception);
				oost.start();

				Binary toSave = attributeNode.getSession().getValueFactory().createBinary(pipedIn);
				if (exception != null) {
					throw exception;
				}
				attributeNode.setProperty(WasabiNodeProperty.VALUE, toSave);
			}

			if (value != null) {
				attributeNode.setProperty(WasabiNodeProperty.TYPE, value.getClass().getName());
			} else {
				attributeNode.setProperty(WasabiNodeProperty.TYPE, (String) null);
			}
			ObjectServiceImpl.modified(attributeNode, s, false, callerPrincipal, false);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new AttributeValueException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_SAVE, "value"),
					io);
		}
	}

	public static void setWasabiValue(Node attributeNode, Node valueNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			if (valueNode != null) {
				Value value = attributeNode.getSession().getValueFactory().createValue(valueNode, true);
				attributeNode.setProperty(WasabiNodeProperty.VALUE, value);
				attributeNode.setProperty(WasabiNodeProperty.TYPE, valueNode.getPrimaryNodeType().getName());
			} else {
				attributeNode.setProperty(WasabiNodeProperty.VALUE, (String) null);
				attributeNode.setProperty(WasabiNodeProperty.TYPE, (String) null);
			}
			ObjectServiceImpl.modified(attributeNode, s, false, callerPrincipal, false);

			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// helper class for storing serializable objects -------------------------------------------------------------------
	private static class ObjectOutputStreamThread extends Thread {

		private ObjectOutputStream objectOut;
		private Serializable content;
		@SuppressWarnings("unused")
		private IOException exception;

		public ObjectOutputStreamThread(ObjectOutputStream objectOut, Serializable content, IOException exception) {
			this.objectOut = objectOut;
			this.content = content;
			this.exception = exception;
		}

		@Override
		public void run() {
			try {
				objectOut.writeObject(content);
				objectOut.close();
			} catch (IOException io) {
				exception = io;
			}
		}
	}
}
