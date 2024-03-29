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
import java.util.Date;
import java.util.Vector;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;

import de.wasabibeans.framework.server.core.authorization.WasabiCertificate;
import de.wasabibeans.framework.server.core.authorization.WasabiObjectACL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.EmptyNodeIterator;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class ObjectServiceImpl {

	public static Node get(String id, Session s) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		try {
			return s.getNodeByIdentifier(id);
		} catch (ItemNotFoundException infe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages
					.get(WasabiExceptionMessages.OBJECT_DNE_ID, id), infe);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getName(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getName();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getUUID(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getIdentifier();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getParent().getParent();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node objectNode, String name, Session s, boolean doJcrSave, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			objectNode.getSession().move(objectNode.getPath(), objectNode.getParent().getPath() + "/" + name);
			ObjectServiceImpl.modified(objectNode, s, false, callerPrincipal, false);

			if (doJcrSave) {
				s.save();
			}
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

	public static void remove(Node objectNode, Session s, boolean doJcrSave) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		try {
			/* ACL Environment - Begin */
			if (WasabiConstants.ACL_ENTRY_ENABLE)
				WasabiObjectACL.removeACLEntriesRecursive(objectNode);
			/* ACL Environment - End */

			objectNode.remove();

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

	public static void removeRecursive(Node objectNode, Session s, boolean throwEvents, JmsConnector jms,
			String callerPrincipal) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		for (Node node : ACLServiceImpl.getChildren(objectNode)) {
			removeRecursive(node, s, throwEvents, jms, callerPrincipal);
		}

		if (throwEvents) {
			EventCreator.createRemovedEvent(objectNode, jms, callerPrincipal);
		}

		/* WasabiCertificate - Begin */
		if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
			WasabiCertificate.invalidateCertificateByObject(objectNode, new int[] { WasabiPermission.VIEW,
					WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.INSERT,
					WasabiPermission.WRITE, WasabiPermission.GRANT }, new int[] { 0, 0, 0, 0, 0, 0, 0 });
		/* WasabiCertificate - End */

		ObjectServiceImpl.remove(objectNode, s, false);
	}

	public static Node getCreatedBy(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.CREATED_BY).getNode();
		} catch (PathNotFoundException pnfe) { // created by not set
			return null;
		} catch (ItemNotFoundException infe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Date getCreatedOn(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.CREATED_ON).getDate().getTime();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getModifiedBy(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.MODIFIED_BY).getNode();
		} catch (PathNotFoundException pnfe) { // modified by not set
			return null;
		} catch (ItemNotFoundException infe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Date getModifiedOn(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.MODIFIED_ON).getDate().getTime();
		} catch (PathNotFoundException pnfe) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Vector<Node> getObjectsByAttributeName(String attributeName, Session s)
			throws UnexpectedInternalProblemException {
		try {
			Vector<Node> result = new Vector<Node>();
			for (NodeIterator ni = getNodesByTypeAndName(WasabiNodeType.ATTRIBUTE, attributeName, s); ni.hasNext();) {
				result.add(ni.nextNode().getParent().getParent());
			}
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getObjectsByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		return getNodesByCreator(creatorNode, WasabiNodeType.OBJECT);
	}

	public static NodeIterator getObjectsByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		return getNodesByModifier(modifierNode, WasabiNodeType.OBJECT);
	}

	public static NodeIterator getObjectsByName(String name, Session s) throws UnexpectedInternalProblemException {
		return getNodesByTypeAndName(WasabiNodeType.OBJECT, name, s);
	}

	/**
	 * Returns all JCR nodes of the given {@code nodeType}.
	 * 
	 * @param nodeType
	 *            e.g. WasabiNodeType.CONTAINER
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodesByType(String nodeType, Session s) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");

			// build the query
			Query query = qomf.createQuery(selector, null, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes that have the given {@code nodeType} and the given {@code name}:
	 * 
	 * @param nodeType
	 *            e.g. WasabiNodeType.CONTAINER
	 * @param name
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodesByTypeAndName(String nodeType, String name, Session s)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.comparison(qomf.nodeName("s1"),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(name)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes of the given {@code nodeType} of which the given {@code String-property} has the given
	 * {@code value}.
	 * 
	 * @param nodeType
	 *            e.g. WasabiNodeType.CONTAINER
	 * @param property
	 *            e.g. WasabiNodeProperty.DISPLAY_NAME
	 * @param value
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodeByPropertyStringValue(String nodeType, String property, String value, Session s)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();

			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", property),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(value)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes of the given {@code nodeType} of which the given {@code Boolean-property} has the given
	 * {@code value}.
	 * 
	 * @param nodeType
	 *            e.g. WasabiNodeType.PIPELINE
	 * @param property
	 *            e.g. WasabiNodeProperty.EMBEDABBLE
	 * @param value
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodeByPropertyBooleanValue(String nodeType, String property, boolean value, Session s)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			ValueFactory vf = s.getValueFactory();

			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", property),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(value)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are child-nodes of the given {@code
	 * parentNode}. 2) They belong to the given child-node category {@code whichChildren}. 3) Their creation-date is not
	 * before the given {@code startDate} and not after the given {@code endDate}. One, but only one, of the two
	 * parameters {@code startDate} and {@code endDate} may be {@code null}, meaning no limit in one direction.
	 * 
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByCreationDate(Node parentNode, String whichChildren, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		try {
			for (NodeIterator ni = parentNode.getNode(whichChildren).getNodes(); ni.hasNext();) {
				Node node = ni.nextNode();
				Date creationDate = ObjectServiceImpl.getCreatedOn(node);
				if (startDate != null && endDate != null) {
					if (!creationDate.before(startDate) && !creationDate.after(endDate)) {
						result.add(node);
					}
				} else if (startDate == null) {
					if (!creationDate.after(endDate)) {
						result.add(node);
					}
				} else if (endDate == null) {
					if (!creationDate.before(startDate)) {
						result.add(node);
					}
				}
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true ordered by their creation-date: 1) They are
	 * child-nodes of the given {@code parentNode}. 2) They belong to the given child-node category {@code
	 * whichChildren}.
	 * 
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @param order
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodesOrderedByCreationDate(Node parentNode, String whichChildren, SortType order)
			throws UnexpectedInternalProblemException {
		try {
			Node childrenCategoryParent = parentNode.getNode(whichChildren);
			String nodeType = whichChildren.substring(0, whichChildren.length() - 1);

			// get the factories
			Session s = parentNode.getSession();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.childNode("s1", childrenCategoryParent.getPath());
			Ordering ordering = null;
			if (order == SortType.ASCENDING) {
				ordering = qomf.ascending(qomf.propertyValue("s1", WasabiNodeProperty.CREATED_ON));
			} else {
				ordering = qomf.descending(qomf.propertyValue("s1", WasabiNodeProperty.CREATED_ON));
			}

			// build the query
			Query query = qomf.createQuery(selector, constraint, new Ordering[] { ordering }, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (PathNotFoundException pnfe) {
			return new EmptyNodeIterator();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are direct or indirect (up to the
	 * given {@code depth}) child-nodes of the given {@code parentNode}. 2) They belong to the given child-node category
	 * {@code whichChildren}. 3) Their creation-date is not before the given {@code startDate} and not after the given
	 * {@code endDate}.
	 * 
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct child-nodes; {@code depth < 0} -> all direct and indirect child-nodes
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByCreationDate(Node parentNode, String whichChildren, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException {
		try {
			Vector<Node> allNodesByCreationDate = new Vector<Node>();
			allNodesByCreationDate.addAll(getNodesByCreationDate(parentNode, whichChildren, startDate, endDate));

			if (parentNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)) {
				for (NodeIterator ni = RoomServiceImpl.getRooms(parentNode); ni.hasNext();) {
					makeRecursiveCallForGetNodesByCreationDate(allNodesByCreationDate, ni.nextNode(), whichChildren,
							startDate, endDate, depth);
				}
			}
			for (NodeIterator ni = ContainerServiceImpl.getContainers(parentNode); ni.hasNext();) {
				makeRecursiveCallForGetNodesByCreationDate(allNodesByCreationDate, ni.nextNode(), whichChildren,
						startDate, endDate, depth);
			}
			return allNodesByCreationDate;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// helper method for getNodesByCreationDate(Node parentNode, String whichChildren, Date startDate,
	// Date endDate, int depth)
	private static void makeRecursiveCallForGetNodesByCreationDate(Vector<Node> allNodesByCreationDate,
			Node parentNode, String whichChildren, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		if (depth > 0) {
			allNodesByCreationDate.addAll(getNodesByCreationDate(parentNode, whichChildren, startDate, endDate,
					depth - 1));
		} else if (depth < 0) {
			allNodesByCreationDate.addAll(getNodesByCreationDate(parentNode, whichChildren, startDate, endDate, depth));
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are child-nodes of the given {@code
	 * parentNode}. 2) They belong to the given child-node category {@code whichChildren}. 3) Their modification-date is
	 * not before the given {@code startDate} and not after the given {@code endDate}. One, but only one, of the two
	 * parameters {@code startDate} and {@code endDate} may be {@code null}, meaning no limit in one direction.
	 * 
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByModificationDate(Node parentNode, String whichChildren, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		try {
			for (NodeIterator ni = parentNode.getNode(whichChildren).getNodes(); ni.hasNext();) {
				Node node = ni.nextNode();
				Date modificationDate = ObjectServiceImpl.getModifiedOn(node);
				if (startDate != null && endDate != null) {
					if (!modificationDate.before(startDate) && !modificationDate.after(endDate)) {
						result.add(node);
					}
				} else if (startDate == null) {
					if (!modificationDate.after(endDate)) {
						result.add(node);
					}
				} else if (endDate == null) {
					if (!modificationDate.before(startDate)) {
						result.add(node);
					}
				}
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are direct or indirect (up to the
	 * given {@code depth}) child-nodes of the given {@code parentNode}. 2) They belong to the given child-node category
	 * {@code whichChildren}. 3) Their modification-date is not before the given {@code startDate} and not after the
	 * given {@code endDate}.
	 * 
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct child-nodes; {@code depth < 0} -> all direct and indirect child-nodes
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByModificationDate(Node parentNode, String whichChildren, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException {
		try {
			Vector<Node> allNodesByCreationDate = new Vector<Node>();
			allNodesByCreationDate.addAll(getNodesByModificationDate(parentNode, whichChildren, startDate, endDate));

			if (parentNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)) {
				for (NodeIterator ni = RoomServiceImpl.getRooms(parentNode); ni.hasNext();) {
					makeRecursiveCallForGetNodesByModificationDate(allNodesByCreationDate, ni.nextNode(),
							whichChildren, startDate, endDate, depth);
				}
			}
			for (NodeIterator ni = ContainerServiceImpl.getContainers(parentNode); ni.hasNext();) {
				makeRecursiveCallForGetNodesByModificationDate(allNodesByCreationDate, ni.nextNode(), whichChildren,
						startDate, endDate, depth);
			}
			return allNodesByCreationDate;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// helper method for getNodesByModificationDate(Node parentNode, String whichChildren, Date startDate,
	// Date endDate, int depth)
	private static void makeRecursiveCallForGetNodesByModificationDate(Vector<Node> allNodesByCreationDate,
			Node parentNode, String whichChildren, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		if (depth > 0) {
			allNodesByCreationDate.addAll(getNodesByModificationDate(parentNode, whichChildren, startDate, endDate,
					depth - 1));
		} else if (depth < 0) {
			allNodesByCreationDate.addAll(getNodesByModificationDate(parentNode, whichChildren, startDate, endDate,
					depth));
		}
	}

	/**
	 * Returns all JCR nodes of the given {@code nodeType} that have been created by the wasabi-user represented by the
	 * given {@code creatorNode}.
	 * 
	 * @param creatorNode
	 * @param nodeType
	 *            e.g. WasabiNodeType.CONTAINER
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodesByCreator(Node creatorNode, String nodeType)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			Session s = creatorNode.getSession();
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", WasabiNodeProperty.CREATED_BY),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(creatorNode)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are child-nodes of the given {@code
	 * parentNode}. 2) They belong to the given child-node category {@code whichChildren}. 3) They have been created by
	 * the wasabi-user represented by the given {@code creatorNode}.
	 * 
	 * @param creatorNode
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByCreator(Node creatorNode, Node parentNode, String whichChildren)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		try {
			for (NodeIterator ni = parentNode.getNode(whichChildren).getNodes(); ni.hasNext();) {
				Node node = ni.nextNode();
				Node actualCreator = ObjectServiceImpl.getCreatedBy(node);
				if (actualCreator != null && creatorNode.getIdentifier().equals(actualCreator.getIdentifier())) {
					result.add(node);
				}
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes of the given {@code nodeType} that have been modified by the wasabi-user represented by the
	 * given {@code creatorNode}.
	 * 
	 * @param modifierNode
	 * @param nodeType
	 *            e.g. WasabiNodeType.CONTAINER
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static NodeIterator getNodesByModifier(Node modifierNode, String nodeType)
			throws UnexpectedInternalProblemException {
		try {
			// get the factories
			Session s = modifierNode.getSession();
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(nodeType, "s1");
			Constraint constraint = qomf.comparison(qomf.propertyValue("s1", WasabiNodeProperty.MODIFIED_BY),
					QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomf.literal(vf.createValue(modifierNode)));

			// build the query
			Query query = qomf.createQuery(selector, constraint, null, null);

			// execute and return result
			return query.execute().getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are child-nodes of the given {@code
	 * parentNode}. 2) They belong to the given child-node category {@code whichChildren}. 3) They have been modified by
	 * the wasabi-user represented by the given {@code creatorNode}.
	 * 
	 * @param modifierNode
	 * @param parentNode
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByModifier(Node modifierNode, Node parentNode, String whichChildren)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		try {
			for (NodeIterator ni = parentNode.getNode(whichChildren).getNodes(); ni.hasNext();) {
				Node node = ni.nextNode();
				Node actualModifier = ObjectServiceImpl.getModifiedBy(node);
				if (actualModifier != null && modifierNode.getIdentifier().equals(actualModifier.getIdentifier())) {
					result.add(node);
				}
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They are child-nodes of the given {@code
	 * parentNode}. 2) They belong to the given child-node category {@code whichChildren}. 3) Their names match the
	 * given regular expression {@code regex}.
	 * 
	 * @param parentNode
	 * @param regex
	 *            see http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#sum
	 * @param whichChildren
	 *            e.g. WasabiNodeProperty.CONTAINERS
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getNodesByNamePattern(Node parentNode, String regex, String whichChildren)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		try {
			for (NodeIterator ni = parentNode.getNode(whichChildren).getNodes(); ni.hasNext();) {
				Node node = ni.nextNode();
				if (ObjectServiceImpl.getName(node).matches(regex)) {
					result.add(node);
				}
			}
			return result;
		} catch (PathNotFoundException pnfe) {
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setCreatedBy(Node objectNode, Node userNode, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Value value = userNode != null ? s.getValueFactory().createValue(userNode, true) : null;
			objectNode.setProperty(WasabiNodeProperty.CREATED_BY, value);

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

	public static void setCreatedOn(Node objectNode, Date creationTime, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		if (creationTime == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"creationTime"));
		}
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(creationTime);
			objectNode.setProperty(WasabiNodeProperty.CREATED_ON, cal);

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

	public static void setModifiedBy(Node objectNode, Node userNode, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Value value = userNode != null ? s.getValueFactory().createValue(userNode, true) : null;
			objectNode.setProperty(WasabiNodeProperty.MODIFIED_BY, value);

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

	public static void setModifiedOn(Node objectNode, Date modificationTime, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		if (modificationTime == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"modificationTime"));
		}
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(modificationTime);
			objectNode.setProperty(WasabiNodeProperty.MODIFIED_ON, cal);

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

	public static long getOptLockId(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
		} catch (PathNotFoundException pnfe) {
			return 0L;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setOptLockId(Node objectNode, long optLockId, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			objectNode.setProperty(WasabiNodeProperty.OPT_LOCK_ID, optLockId);

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

	/**
	 * Sets the wasabi:createdOn, wasabi:createdBy, wasabi:modifiedOn, wasabi:modifiedBy, and wasabi:optLockId
	 * properties for the given {@code objectNode}. If the the given {@code callerPrincipal} is {@code null} and the
	 * given {@code nullEntryEnabled} is {@code true}, the properties wasabi:createdBy and wasabi:modifiedBy will be set
	 * to {@code null}. If the the given {@code callerPrincipal} is {@code null} and the given {@code nullEntryEnabled}
	 * is {@code false}, none of the properties will be set.
	 * 
	 * @param objectNode
	 * @param s
	 * @param callerPrincipal
	 * @param nullEntryEnabled
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void created(Node objectNode, Session s, boolean doJcrSave, String callerPrincipal,
			boolean nullEntryEnabled) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Node currentUser = null;
			if (callerPrincipal != null) {
				currentUser = UserServiceImpl.getUserByName(callerPrincipal, s);
			} else {
				if (!nullEntryEnabled) {
					return;
				}
			}
			Date timestamp = Calendar.getInstance().getTime();
			ObjectServiceImpl.setCreatedOn(objectNode, timestamp, s, false);
			ObjectServiceImpl.setCreatedBy(objectNode, currentUser, s, false);
			ObjectServiceImpl.setModifiedOn(objectNode, timestamp, s, false);
			ObjectServiceImpl.setModifiedBy(objectNode, currentUser, s, false);

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

	/**
	 * Sets the wasabi:modifiedOn, wasabi:modifiedBy, and wasabi:optLockId properties for the given {@code objectNode}.
	 * If the the given {@code callerPrincipal} is {@code null} and the given {@code nullEntryEnabled} is {@code true},
	 * the property wasabi:modifiedBy will be set to {@code null}. If the the given {@code callerPrincipal} is {@code
	 * null} and the given {@code nullEntryEnabled} is {@code false}, none of the properties will be set.
	 * 
	 * @param objectNode
	 * @param s
	 * @param callerPrincipal
	 * @param nullEntryEnabled
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void modified(Node objectNode, Session s, boolean doJcrSave, String callerPrincipal,
			boolean nullEntryEnabled) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Node currentUser = null;
			if (callerPrincipal != null) {
				currentUser = UserServiceImpl.getUserByName(callerPrincipal, s);
			} else {
				if (!nullEntryEnabled) {
					return;
				}
			}
			ObjectServiceImpl.setModifiedOn(objectNode, Calendar.getInstance().getTime(), s, false);
			ObjectServiceImpl.setModifiedBy(objectNode, currentUser, s, false);
			long newOptLockId = ObjectServiceImpl.getOptLockId(objectNode) + 1;
			if (newOptLockId < 0) {
				// do not use values < 0 (after Long.MAX_VALUE has been reached)
				newOptLockId = 0;
			}
			ObjectServiceImpl.setOptLockId(objectNode, newOptLockId, s, false);

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
}
