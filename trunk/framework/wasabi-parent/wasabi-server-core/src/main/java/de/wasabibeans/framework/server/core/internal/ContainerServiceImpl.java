package de.wasabibeans.framework.server.core.internal;

import java.util.Date;
import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class ContainerServiceImpl {

	public static Node create(String name, Node environmentNode, Session s, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			Node containerNode = environmentNode.addNode(WasabiNodeProperty.CONTAINERS + "/" + name,
					WasabiNodeType.CONTAINER);
			ObjectServiceImpl.created(containerNode, s, callerPrincipal, true);
			return containerNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "container", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getContainerByName(Node locationNode, String name) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.CONTAINERS + "/" + name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getContainers(Node locationNode) throws UnexpectedInternalProblemException {
		try {
			return locationNode.getNode(WasabiNodeProperty.CONTAINERS).getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their creation-date is not before the given {@code startDate} and not after the given
	 * {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByCreationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		for (NodeIterator ni = getContainers(environmentNode); ni.hasNext();) {
			Node container = ni.nextNode();
			Date creationDate = ObjectServiceImpl.getCreatedOn(container);
			if (!creationDate.before(startDate) && !creationDate.after(endDate)) {
				result.add(container);
			}
		}
		return result;
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct or indirect (up to the given {@code depth}) sub-containers of the
	 * wasabi-environment represented by the given {@code environmentNode}. 3) Their creation-date is not before the
	 * given {@code startDate} and not after the given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct sub-containers; {@code depth < 0} -> all direct and indirect
	 *            sub-containers
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
			throws UnexpectedInternalProblemException {
		try {
			Vector<Node> allContainersByCreationDate = new Vector<Node>();
			allContainersByCreationDate.addAll(getContainersByCreationDate(environmentNode, startDate, endDate));

			if (environmentNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)) {
				for (NodeIterator ni = RoomServiceImpl.getRooms(environmentNode); ni.hasNext();) {
					makeRecursiveCallForGetContainersByCreationDate(allContainersByCreationDate, ni.nextNode(),
							startDate, endDate, depth);
				}
			}
			for (NodeIterator ni = getContainers(environmentNode); ni.hasNext();) {
				makeRecursiveCallForGetContainersByCreationDate(allContainersByCreationDate, ni.nextNode(), startDate,
						endDate, depth);
			}
			return allContainersByCreationDate;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// helper method for getContainersByCreationDate(Node environmentNode, Date startDate, Date endDate, int depth)
	private static void makeRecursiveCallForGetContainersByCreationDate(Vector<Node> allContainersByCreationDate,
			Node environmentNode, Date startDate, Date endDate, int depth) throws UnexpectedInternalProblemException {
		if (depth > 0) {
			allContainersByCreationDate.addAll(getContainersByCreationDate(environmentNode, startDate, endDate,
					depth - 1));
		} else if (depth < 0) {
			allContainersByCreationDate.addAll(getContainersByCreationDate(environmentNode, startDate, endDate, depth));
		}
	}

	public static NodeIterator getContainersByCreator(Node creatorNode) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			Session s = creatorNode.getSession();
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.CONTAINER, "s1");
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

	public static Vector<Node> getContainersByCreator(Node creatorNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		try {
			Vector<Node> result = new Vector<Node>();
			for (NodeIterator ni = ContainerServiceImpl.getContainers(environmentNode); ni.hasNext();) {
				Node container = ni.nextNode();
				if (creatorNode.getIdentifier().equals(ObjectServiceImpl.getCreatedBy(container).getIdentifier())) {
					result.add(container);
				}
			}
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their modification-date is not before the given {@code startDate} and not after the
	 * given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByModificationDate(Node environmentNode, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		for (NodeIterator ni = getContainers(environmentNode); ni.hasNext();) {
			Node container = ni.nextNode();
			Date creationDate = ObjectServiceImpl.getModifiedOn(container);
			if (!creationDate.before(startDate) && !creationDate.after(endDate)) {
				result.add(container);
			}
		}
		return result;
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct or indirect (up to the given {@code depth}) sub-containers of the
	 * wasabi-environment represented by the given {@code environmentNode}. 3) Their modification-date is not before the
	 * given {@code startDate} and not after the given {@code endDate}.
	 * 
	 * @param environmentNode
	 * @param startDate
	 * @param endDate
	 * @param depth
	 *            {@code depth = 0} -> only direct sub-containers; {@code depth < 0} -> all direct and indirect
	 *            sub-containers
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<Node> getContainersByModificationDate(Node environmentNode, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException {
		try {
			Vector<Node> allContainersByCreationDate = new Vector<Node>();
			allContainersByCreationDate.addAll(getContainersByModificationDate(environmentNode, startDate, endDate));

			if (environmentNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)) {
				for (NodeIterator ni = RoomServiceImpl.getRooms(environmentNode); ni.hasNext();) {
					makeRecursiveCallForGetContainersByModificationDate(allContainersByCreationDate, ni.nextNode(),
							startDate, endDate, depth);
				}
			}
			for (NodeIterator ni = getContainers(environmentNode); ni.hasNext();) {
				makeRecursiveCallForGetContainersByModificationDate(allContainersByCreationDate, ni.nextNode(),
						startDate, endDate, depth);
			}
			return allContainersByCreationDate;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// helper method for getContainersByModificationDate(Node environmentNode, Date startDate, Date endDate, int depth)
	private static void makeRecursiveCallForGetContainersByModificationDate(Vector<Node> allContainersByCreationDate,
			Node environmentNode, Date startDate, Date endDate, int depth) throws UnexpectedInternalProblemException {
		if (depth > 0) {
			allContainersByCreationDate.addAll(getContainersByModificationDate(environmentNode, startDate, endDate,
					depth - 1));
		} else if (depth < 0) {
			allContainersByCreationDate.addAll(getContainersByModificationDate(environmentNode, startDate, endDate,
					depth));
		}
	}

	public static NodeIterator getContainersByModifier(Node modifierNode) throws UnexpectedInternalProblemException {
		try {
			// get the factories
			Session s = modifierNode.getSession();
			ValueFactory vf = s.getValueFactory();
			QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

			// build the query components: columns, source, constraint, orderings
			// ("SELECT columns FROM source WHERE constraints ORDER BY orderings")
			Selector selector = qomf.selector(WasabiNodeType.CONTAINER, "s1");
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

	public static Vector<Node> getContainersByModifier(Node modifierNode, Node environmentNode)
			throws UnexpectedInternalProblemException {
		try {
			Vector<Node> result = new Vector<Node>();
			for (NodeIterator ni = ContainerServiceImpl.getContainers(environmentNode); ni.hasNext();) {
				Node container = ni.nextNode();
				if (modifierNode.getIdentifier().equals(ObjectServiceImpl.getModifiedBy(container).getIdentifier())) {
					result.add(container);
				}
			}
			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Returns all JCR nodes for which the following conditions are true: 1) They represent wasabi-containers. 2) Their
	 * represented wasabi-containers are direct sub-containers of the wasabi-environment represented by the given
	 * {@code environmentNode}. 3) Their names match the given regular expression {@code regex}.
	 * 
	 * @param environmentNode
	 * @param regex
	 * @return
	 * @throws UnexpectedInternalProblemException
	 * @see http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#sum
	 */
	public static Vector<Node> getContainersByNamePattern(Node environmentNode, String regex)
			throws UnexpectedInternalProblemException {
		Vector<Node> result = new Vector<Node>();
		for (NodeIterator ni = ContainerServiceImpl.getContainers(environmentNode); ni.hasNext();) {
			Node container = ni.nextNode();
			if (ObjectServiceImpl.getName(container).matches(regex)) {
				result.add(container);
			}
		}
		return result;
	}

	public static Node getEnvironment(Node containerNode) throws UnexpectedInternalProblemException {
		return ObjectServiceImpl.getEnvironment(containerNode);
	}

	public static void move(Node containerNode, Node newEnvironmentNode, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			containerNode.getSession().move(containerNode.getPath(),
					newEnvironmentNode.getPath() + "/" + WasabiNodeProperty.CONTAINERS + "/" + containerNode.getName());
			ObjectServiceImpl.modified(containerNode, containerNode.getSession(), callerPrincipal, false);
		} catch (ItemExistsException iee) {
			try {
				String name = containerNode.getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "container", name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}

	}

	public static void remove(Node containerNode) throws UnexpectedInternalProblemException {
		ObjectServiceImpl.remove(containerNode);
	}

	public static void rename(Node containerNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		ObjectServiceImpl.rename(containerNode, name, callerPrincipal);
	}
}
