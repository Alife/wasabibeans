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
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class DocumentServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			Node documentNode = environmentNode.addNode(WasabiNodeProperty.DOCUMENTS + "/" + name,
					WasabiNodeType.DOCUMENT);
			ObjectServiceImpl.created(documentNode, s, callerPrincipal, true);

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

	public static void setContent(Node documentNode, Serializable content, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException {
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
			ObjectServiceImpl.modified(documentNode, documentNode.getSession(), callerPrincipal, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new DocumentContentException(WasabiExceptionMessages.INTERNAL_DOCUMENT_CONTENT_SAVE, io);
		}
	}

	public static Node getDocumentByName(Node locationNode, String name) throws UnexpectedInternalProblemException {
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

	public static void move(Node documentNode, Node newEnvironmentNode, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			documentNode.getSession().move(documentNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/" + documentNode.getName());
			ObjectServiceImpl.modified(documentNode, documentNode.getSession(), callerPrincipal, false);
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

	public static void rename(Node documentNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(documentNode, name, callerPrincipal);
	}

	public static Vector<Node> getDocumentsByCreationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.DOCUMENTS, startDate,
				endDate);
	}

	public static Vector<Node> getDocumentsByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreationDate(environmentNode, WasabiNodeProperty.DOCUMENTS, startDate,
				endDate, depth);
	}

	public static NodeIterator getDocumentsByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, WasabiNodeType.DOCUMENT);
	}

	public static Vector<Node> getDocumentsByCreator(Node creatorNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByCreator(creatorNode, environmentNode, WasabiNodeProperty.DOCUMENTS);
	}

	public static Vector<Node> getDocumentsByModificationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.DOCUMENTS, startDate,
				endDate);
	}

	public static Vector<Node> getDocumentsByModificationDate(Node environmentNode, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModificationDate(environmentNode, WasabiNodeProperty.DOCUMENTS, startDate,
				endDate, depth);
	}

	public static NodeIterator getDocumentsByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, WasabiNodeType.DOCUMENT);
	}

	public static Vector<Node> getDocumentsByModifier(Node modifierNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesByModifier(modifierNode, environmentNode, WasabiNodeProperty.DOCUMENTS);
	}

	public static NodeIterator getDocumentsOrderedByCreationDate(Node locationNode, SortType order)
			throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getNodesOrderedByCreationDate(locationNode, WasabiNodeProperty.DOCUMENTS, order);
	}

	public static boolean hasDocumentsCreatedAfter(Node environmentNode, Long timestamp)
			throws UnexpectedInternalProblemException {
		return !getDocumentsByCreationDate(environmentNode, new Date(timestamp), null).isEmpty();
	}

	public static boolean hasDocumentsCreatedBefore(Node environmentNode, Long timestamp)
			throws UnexpectedInternalProblemException {
		return !getDocumentsByCreationDate(environmentNode, null, new Date(timestamp)).isEmpty();
	}

	public static boolean hasDocumentsModifiedAfter(Node environmentNode, Long timestamp)
			throws UnexpectedInternalProblemException {
		return !getDocumentsByModificationDate(environmentNode, new Date(timestamp), null).isEmpty();
	}

	public static boolean hasDocumentsModifiedBefore(Node environmentNode, Long timestamp)
			throws UnexpectedInternalProblemException {
		return !getDocumentsByModificationDate(environmentNode, null, new Date(timestamp)).isEmpty();
	}
}
