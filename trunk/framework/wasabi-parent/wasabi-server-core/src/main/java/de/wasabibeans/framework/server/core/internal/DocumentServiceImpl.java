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

import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.authorization.WasabiDocumentACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class DocumentServiceImpl {

	public static Node create(String name, Node environmentNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		try {
			Node documentNode = environmentNode.addNode(WasabiNodeProperty.DOCUMENTS + "/" + name,
					WasabiNodeType.DOCUMENT);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiDocumentACL.ACLEntryForCreate(documentNode, s);
				WasabiDocumentACL.ACLEntryTemplateForCreate(documentNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			return documentNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "document", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Serializable getContent(Node documentNode) throws UnexpectedInternalProblemException,
			DocumentContentException {
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

	public static void setContent(Node documentNode, Serializable content) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException {
		try {
			PipedInputStream pipedIn = new PipedInputStream();
			PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
			ObjectOutputStream objectOut = new ObjectOutputStream(pipedOut);
			IOException exception = null;

			ObjectOutputStreamThread oost = new ObjectOutputStreamThread(objectOut, content, exception);
			oost.start();

			Binary toSave = documentNode.getSession().getValueFactory().createBinary(pipedIn);
			if (exception != null) {
				throw exception;
			}

			documentNode.setProperty(WasabiNodeProperty.CONTENT, toSave);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new DocumentContentException(WasabiExceptionMessages.INTERNAL_DOCUMENT_CONTENT_SAVE, io);
		}
	}

	public static Node getDocumentByName(Node locationNode, String name) throws UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		try {
			return locationNode.getNode(WasabiNodeProperty.DOCUMENTS + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getDocuments(Node locationNode) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.DOCUMENTS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node documentNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(documentNode);
	}

	public static void move(Node documentNode, Node newEnvironmentNode) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		try {
			documentNode.getSession().move(documentNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/" + documentNode.getName());
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

	public static void remove(Node documentNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(documentNode);
	}

	public static void rename(Node documentNode, String name) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(documentNode, name);
	}

	public static Vector<Node> getDocumentsByCreationDate(Node environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByCreationDate(Node environment, Date startDate, Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByCreator(WasabiUserDTO creator, Node environment) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByModificationDate(Node environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByModificationDate(Node environment, Date startDate, Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsByModifier(WasabiUserDTO modifier, Node environment) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<Node> getDocumentsOrderedByCreationDate(Node location, SortType order) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean hasDocumentsCreatedAfter(Node environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	public static boolean hasDocumentsCreatedBefore(Node environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	public static boolean hasDocumentsModifiedAfter(Node environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	public static boolean hasDocumentsModifiedBefore(Node environment, Long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}
}