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
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
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
import de.wasabibeans.framework.server.core.util.EmptyNodeIterator;
import de.wasabibeans.framework.server.core.util.IOUtil;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class DocumentServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			Node documentNode;
			if (environmentNode.hasNode(WasabiNodeProperty.DOCUMENTS)) {
				documentNode = environmentNode.addNode(WasabiNodeProperty.DOCUMENTS + "/" + name,
						WasabiNodeType.DOCUMENT);
			} else {
				Node documents = environmentNode
						.addNode(WasabiNodeProperty.DOCUMENTS, WasabiNodeType.OBJECT_COLLECTION);
				documentNode = documents.addNode(name, WasabiNodeType.DOCUMENT);
			}
			ObjectServiceImpl.created(documentNode, s, false, callerPrincipal, true);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE) {
				WasabiDocumentACL.ACLEntryForCreate(documentNode);
				WasabiDocumentACL.ACLEntryTemplateForCreate(documentNode, environmentNode, callerPrincipal, s);
			}
			/* ACL Environment - End */

			if (doJcrSave) {
				s.save();
			}
			return documentNode;
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

	public static Serializable getContent(Node documentNode) throws UnexpectedInternalProblemException,
			DocumentContentException {
		try {
			Binary content = documentNode.getProperty(WasabiNodeProperty.CONTENT).getBinary();
			ObjectInputStream oIn = new ObjectInputStream(content.getStream());
			return (Serializable) oIn.readObject();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_LOAD,
					"content", "document"), e);
		}
	}

	public static Node getDocumentById(String id, Session s) throws UnexpectedInternalProblemException,
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
		} catch (PathNotFoundException pnfe) {
			return new EmptyNodeIterator();
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

	public static void move(Node documentNode, Node newEnvironmentNode, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		try {
			if (newEnvironmentNode.hasNode(WasabiNodeProperty.DOCUMENTS)) {
				s.move(documentNode.getPath(), newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.DOCUMENTS + "/"
						+ documentNode.getName());
			} else {
				Node documents = newEnvironmentNode.addNode(WasabiNodeProperty.DOCUMENTS,
						WasabiNodeType.OBJECT_COLLECTION);
				s.move(documentNode.getPath(), documents.getPath() + "/" + documentNode.getName());
			}
			ObjectServiceImpl.modified(documentNode, s, false, callerPrincipal, false);

			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiDocumentACL.ACLEntryForMove(documentNode);
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

	public static void remove(Node documentNode, Session s, boolean doJcrSave, boolean throwEvents, JmsConnector jms,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			ObjectServiceImpl.removeRecursive(documentNode, s, true, jms, callerPrincipal);

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

	public static void rename(Node documentNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		ObjectServiceImpl.rename(documentNode, name, s, doJcrSave, callerPrincipal);
	}

	public static void setContent(Node documentNode, Serializable content, Session s, boolean doJcrSave,
			String callerPrincipal) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException {
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
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_SAVE,
					"content"), io);
		}
	}

	// -------------------------- Wasabi Pipes --------------------------------------------------------------

	public static void addContentRef(Node documentNode, ContentStore filter, String ref, String mimeType, Long size,
			boolean isContentAvailable, Session s, boolean doJcrSave, String callerPrincipal)
			throws ConcurrentModificationException, UnexpectedInternalProblemException {
		try {
			if (!documentNode.hasNode(WasabiNodeProperty.CONTENT_REFS)) {
				documentNode.addNode(WasabiNodeProperty.CONTENT_REFS, WasabiNodeType.CONTENT_REFS);
			}

			Calendar timestamp = Calendar.getInstance();
			Node contentrefNode = null;
			boolean ok = false;
			while (!ok) {
				try {
					// Math.random() in node name -> in case contentrefs are created too quickly
					// while-loop -> in case that even the use of Math.random() does not lead to a unique node-name
					String nodeName = "" + timestamp.getTimeInMillis() + ((int) (Math.random() * 1000));
					contentrefNode = documentNode.addNode(WasabiNodeProperty.CONTENT_REFS + "/" + nodeName,
							WasabiNodeType.CONTENT_REF);
					ok = true;
				} catch (ItemExistsException iee) {
					// do nothing -> stay in while-loop
				}
			}

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
			ObjectServiceImpl.modified(documentNode, s, false, callerPrincipal, false);

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

	public static void setContentPiped(Node documentNode, Serializable content, Session s, boolean doJcrSave,
			JmsConnector jms, SharedFilterBean sharedFilterBean, String callerPrincipal)
			throws UnexpectedInternalProblemException, TargetDoesNotExistException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException {
		try {
			Node nearestRoomNode = getNearestRoom(documentNode);
			Node pipelineNode = RoomServiceImpl.getPipeline(nearestRoomNode);
			if (pipelineNode == null) {
				new DocumentSink().filter(null, new Filter.DocumentInfo(documentNode, callerPrincipal), IOUtil
						.convert2Byte(content), s, jms, sharedFilterBean);
			} else {
				FilterServiceImpl.apply(pipelineNode, documentNode, content, s, jms, sharedFilterBean, callerPrincipal);
			}

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
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_SAVE,
					"content"), io);
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

	public static Serializable getContentPiped(Node documentNode, Node contentref)
			throws UnexpectedInternalProblemException, DocumentContentException, TargetDoesNotExistException {
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
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_LOAD,
					"content", "document"), io);
		} catch (ClassNotFoundException cnfe) {
			throw new DocumentContentException(WasabiExceptionMessages.get(WasabiExceptionMessages.VALUE_LOAD,
					"content", "document"), cnfe);
		}
	}

	public static Node getDocument(Node contentrefNode) throws TargetDoesNotExistException,
			UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.DOCUMENT).getNode();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (ItemNotFoundException infe) {
			throw new TargetDoesNotExistException(WasabiExceptionMessages.TARGET_NOT_FOUND, infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getContentRefs(Node documentNode) throws UnexpectedInternalProblemException {
		try {
			return documentNode.getNode(WasabiNodeProperty.CONTENT_REFS).getNodes();
		} catch (PathNotFoundException pnfe) {
			return new EmptyNodeIterator();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getFilterClass(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.FILTER_CLASS).getString();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getJsonData(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.JSONDATA).getString();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getMimeType(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.MIME_TYPE).getString();
		} catch (PathNotFoundException pnfe) {
			return null;
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
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Long getSize(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.SIZE).getLong();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Boolean isContentAvailable(Node contentrefNode) throws UnexpectedInternalProblemException {
		try {
			return contentrefNode.getProperty(WasabiNodeProperty.IS_CONTENT_AVAILABLE).getBoolean();
		} catch (PathNotFoundException pnfe) {
			return false;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// ---------------------------- helper class --------------------------------------------------------------------

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
}
