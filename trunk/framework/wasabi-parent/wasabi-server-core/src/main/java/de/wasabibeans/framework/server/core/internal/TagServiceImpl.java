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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class TagServiceImpl {

	public static void addTag(Node objectNode, String tag, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			if (!objectNode.hasNode(WasabiNodeProperty.TAGS)) {
				objectNode.addNode(WasabiNodeProperty.TAGS, WasabiNodeType.TAGS);
			}

			Calendar timestamp = Calendar.getInstance();
			Node tagNode = null;
			boolean ok = false;
			while (!ok) {
				try {
					// Math.random() in node name -> in case tags are created too quickly
					// while-loop -> in case that even the use of Math.random() does not lead to a unique node-name
					String nodeName = "" + timestamp.getTimeInMillis() + ((int) (Math.random() * 1000));
					tagNode = objectNode.addNode(WasabiNodeProperty.TAGS + "/" + nodeName, WasabiNodeType.TAG);
					ok = true;
				} catch (ItemExistsException iee) {
					// do nothing -> stay in while-loop
				}
			}
			tagNode.setProperty(WasabiNodeProperty.TEXT, tag);
			tagNode.setProperty(WasabiNodeProperty.CREATED_ON, timestamp);
			Node currentUser = UserServiceImpl.getUserByName(callerPrincipal, s);
			tagNode.setProperty(WasabiNodeProperty.CREATED_BY, currentUser);

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

	public static void clearTags(Node objectNode, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			for (NodeIterator ni = objectNode.getNode(WasabiNodeProperty.TAGS).getNodes(); ni.hasNext();) {
				ni.nextNode().remove();
			}

			if (doJcrSave) {
				s.save();
			}
		} catch (PathNotFoundException pnfe) {
			// not tags exist -> do nothing
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Vector<Node> getDocumentsByTags(Node environmentNode, Vector<String> tags)
			throws UnexpectedInternalProblemException {
		// TODO is this method really useful? no idea how to optimize the search for a collection of tags... (searching
		// for exactly one tag is easier - maybe make as many queries as there are tags to be searched for and then
		// unite the results?... a performance test would have to be made)
		Vector<Node> result = new Vector<Node>();
		for (NodeIterator docs = DocumentServiceImpl.getDocuments(environmentNode); docs.hasNext();) {
			Node doc = docs.nextNode();
			if (getTags(doc).containsAll(tags)) {
				result.add(doc);
			}
		}
		return result;
	}

	public static Map<String, Integer> getMostUsedDocumentTags(Node environmentNode, int limit)
			throws UnexpectedInternalProblemException {
		// TODO operations like 'select distinct', or 'count', or 'limit' known from SQL are not supported by JCR-SQL2
		// as specified in the JSR-283 -> consequently this method cannot be implemented by help of a more efficient
		// query
		Map<String, Integer> documentTags = new HashMap<String, Integer>();
		for (NodeIterator docs = DocumentServiceImpl.getDocuments(environmentNode); docs.hasNext();) {
			for (String tag : getTags(docs.nextNode())) {
				Integer count = documentTags.get(tag);
				if (count == null) {
					documentTags.put(tag, 1);
				} else {
					documentTags.put(tag, ++count);
				}

			}
		}
		return documentTags;
	}

	public static Vector<Node> getObjectsByTag(String tag, Session s) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.TAG, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", WasabiNodeProperty.TEXT),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(tag)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			Vector<Node> result = new Vector<Node>();
			for (NodeIterator ni = query.execute().getNodes(); ni.hasNext();) {
				result.add(ObjectServiceImpl.getEnvironment(ni.nextNode()));
			}
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

	}

	public static Vector<String> getTags(Node objectNode) throws UnexpectedInternalProblemException {
		Vector<String> result = new Vector<String>();
		try {
			for (NodeIterator ni = objectNode.getNode(WasabiNodeProperty.TAGS).getNodes(); ni.hasNext();) {
				result.add(ni.nextNode().getProperty(WasabiNodeProperty.TEXT).getString());
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void removeTag(Node objectNode, String tag, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			for (NodeIterator ni = objectNode.getNode(WasabiNodeProperty.TAGS).getNodes(); ni.hasNext();) {
				Node tagNode = ni.nextNode();
				if (tag.equals(tagNode.getProperty(WasabiNodeProperty.TEXT).getString())) {
					tagNode.remove();
				}
			}

			if (doJcrSave) {
				s.save();
			}
		} catch (PathNotFoundException pnfe) {
			// no tags exist -> do nothing
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
