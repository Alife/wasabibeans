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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;

import de.wasabibeans.framework.server.core.authorization.WasabiDocumentACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.pipes.filter.ContentStore;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterOutput;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSink;
import de.wasabibeans.framework.server.core.util.IOUtil;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class DocumentServiceImpl {

	private static class ObjectOutputStreamThread extends Thread {

		private Serializable content;
		@SuppressWarnings("unused")
		private IOException exception;
		private ObjectOutputStream objectOut;

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

	public static void addContentRef(Node documentNode, ContentStore filter, String ref, String mimeType, Long size,
			boolean isContentAvailable) throws ConcurrentModificationException, UnexpectedInternalProblemException {
		try {
			String name = Calendar.getInstance().getTimeInMillis() + " " + (int) (Math.random() * 1000);
			Node contentrefNode = documentNode.addNode(WasabiNodeProperty.CONTENT_REFS + "/" + name,
					WasabiNodeType.CONTENT_REF);
			contentrefNode.setProperty(WasabiNodeProperty.DOCUMENT, documentNode);
			contentrefNode.setProperty(WasabiNodeProperty.FILTER_CLASS, filter.getClass().getName());
			contentrefNode.setProperty(WasabiNodeProperty.REF, ref);
			contentrefNode.setProperty(WasabiNodeProperty.MIME_TYPE, mimeType);
			contentrefNode.setProperty(WasabiNodeProperty.SIZE, size);
			contentrefNode.setProperty(WasabiNodeProperty.IS_CONTENT_AVAILABLE, isContentAvailable);

			GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
				}

				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					return f.getAnnotation(FilterOutput.class) != null || f.getAnnotation(FilterInput.class) != null;
				}
			});
			contentrefNode.setProperty(WasabiNodeProperty.JSONDATA, gsonBuilder.create().toJson(filter));
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_CREATION_FAILURE, "attribute"), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node create(String name, Node environmentNode, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException {
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
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_CREATION_FAILURE, "document"), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static ContentStore createFilter(Node contentref) throws JsonParseException,
			UnexpectedInternalProblemException {
		try {
			return (ContentStore) new GsonBuilder().registerTypeAdapter(Filter.Point.class,
					new InstanceCreator<Filter.Point>() {
						@Override
						public Filter.Point createInstance(Type type) {
							return new Filter.Point(0, 0);
						}
					}).create().fromJson(getJsonData(contentref), Class.forName(getFilterClass(contentref)));
		} catch (ClassNotFoundException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_PROBLEM, e);
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
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_VALUE_LOAD,
					"content", "document"), e);
		}
	}

	public static Serializable getContentPiped(Node documentNode, Node contentref)
			throws UnexpectedInternalProblemException, DocumentContentException {
		try {
			if (contentref == null && getContentRefs(documentNode).hasNext()) {
				contentref = getContentRefs(documentNode).nextNode();
			}

			if (contentref == null) {
				// nothing found -> no content
				return null;
			} else {
				ContentStore contentStore = createFilter(contentref);
				InputStream in = contentStore.getContent(contentref);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				return IOUtil.convert2Serializable(out.toByteArray());
			}
		} catch (IOException io) {
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_VALUE_LOAD,
					"content", "document"), io);
		} catch (ClassNotFoundException cnfe) {
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_VALUE_LOAD,
					"content", "document"), cnfe);
		}
	}

	public static NodeIterator getContentRefs(Node documentNode) throws UnexpectedInternalProblemException {
		try {
			return documentNode.getNode(WasabiNodeProperty.CONTENT_REFS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getDocument(Node contentrefNode) throws TargetDoesNotExistException,
			UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.DOCUMENT).getNode();
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.INTERNAL_REFERENCE_INVALID, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getDocumentById(String id, Session s) throws UnexpectedInternalProblemException {
		try {
			return s.getNodeByIdentifier(id);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
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

	public static Node getEnvironment(Node documentNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(documentNode);
	}

	public static String getFilterClass(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.FILTER_CLASS).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getJsonData(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.JSONDATA).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// -------------------------- Wasabi Pipes --------------------------------------------------------------

	public static String getMimeType(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.MIME_TYPE).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getNearestRoom(Node documentNode) throws UnexpectedInternalProblemException {
		try {
			Node locationNode = getEnvironment(documentNode);
			while (!locationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)) {
				locationNode = ObjectServiceImpl.getEnvironment(locationNode);
			}
			return locationNode;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getRef(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.REF).getString();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Long getSize(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.SIZE).getLong();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
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

	public static Boolean isContentAvailable(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.IS_CONTENT_AVAILABLE).getBoolean();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void move(Node documentNode, Node newEnvironmentNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			documentNode.getSession().move(documentNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/" + documentNode.getName());
			ObjectServiceImpl.modified(documentNode, documentNode.getSession(), callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiDocumentACL.ACLEntryForMove(documentNode, s);
			/* ACL Environment - End */
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

	public static void remove(Node documentNode) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		ObjectServiceImpl.remove(documentNode);
	}

	public static void rename(Node documentNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(documentNode, name, callerPrincipal);
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
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_VALUE_SAVE,
					"content"), io);
		}
	}

	// ---------------------------- helper class --------------------------------------------------------------------

	public static void setContentPiped(Node documentNode, Serializable content, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean, String callerPrincipal) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException {
		try {
			Node nearestRoomNode = getNearestRoom(documentNode);
			Node pipelineNode = RoomServiceImpl.getPipeline(nearestRoomNode);
			if (pipelineNode == null) {
				new DocumentSink().filter(null, new Filter.DocumentInfo(documentNode, callerPrincipal), IOUtil
						.convert2Byte(content), s, jms, sharedFilterBean);
			} else {
				FilterServiceImpl.apply(pipelineNode, documentNode, content, s, jms, sharedFilterBean, callerPrincipal);
			}
		} catch (IOException io) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_PROBLEM, io);
		}
	}
}
