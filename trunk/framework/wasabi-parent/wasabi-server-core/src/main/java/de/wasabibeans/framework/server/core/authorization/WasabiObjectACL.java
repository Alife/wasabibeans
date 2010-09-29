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

package de.wasabibeans.framework.server.core.authorization;

import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;

public class WasabiObjectACL {

	public static void remove(Node objectNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		removeRecursive(objectNode, callerPrincipal, s);
	}

	private static int removeRecursive(Node objectNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			int childNodes = 0;
			Vector<Node> childreenNodes = new Vector<Node>();
			String objectType = objectNode.getPrimaryNodeType().getName();

			if (objectType.equals(WasabiNodeType.ROOM)) {
				NodeIterator RoomChildreen = objectNode.getNode(WasabiNodeProperty.ROOMS).getNodes();
				NodeIterator ContainerChildreen = objectNode.getNode(WasabiNodeProperty.CONTAINERS).getNodes();
				NodeIterator AttributeChildreen = objectNode.getNode(WasabiNodeProperty.ATTRIBUTES).getNodes();
				NodeIterator LinkChildreen = objectNode.getNode(WasabiNodeProperty.LINKS).getNodes();
				NodeIterator DocumentChildreen = objectNode.getNode(WasabiNodeProperty.DOCUMENTS).getNodes();

				while (RoomChildreen.hasNext())
					childreenNodes.add(RoomChildreen.nextNode());

				while (ContainerChildreen.hasNext())
					childreenNodes.add(ContainerChildreen.nextNode());

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());

				while (LinkChildreen.hasNext())
					childreenNodes.add(LinkChildreen.nextNode());

				while (DocumentChildreen.hasNext())
					childreenNodes.add(DocumentChildreen.nextNode());
			} else if (objectType.equals(WasabiNodeType.CONTAINER)) {
				NodeIterator ContainerChildreen = objectNode.getNode(WasabiNodeProperty.CONTAINERS).getNodes();
				NodeIterator AttributeChildreen = objectNode.getNode(WasabiNodeProperty.ATTRIBUTES).getNodes();
				NodeIterator LinkChildreen = objectNode.getNode(WasabiNodeProperty.LINKS).getNodes();
				NodeIterator DocumentChildreen = objectNode.getNode(WasabiNodeProperty.DOCUMENTS).getNodes();

				while (ContainerChildreen.hasNext())
					childreenNodes.add(ContainerChildreen.nextNode());

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());

				while (LinkChildreen.hasNext())
					childreenNodes.add(LinkChildreen.nextNode());

				while (DocumentChildreen.hasNext())
					childreenNodes.add(DocumentChildreen.nextNode());
			} else if (objectType.equals(WasabiNodeType.DOCUMENT)) {
				NodeIterator AttributeChildreen = objectNode.getNode(WasabiNodeProperty.ATTRIBUTES).getNodes();

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());
			} else if (objectNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ATTRIBUTE)) {
				NodeIterator AttributeChildreen = objectNode.getNode(WasabiNodeProperty.ATTRIBUTES).getNodes();

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());
			}

			if (childreenNodes.size() == 0) {
				if (WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s)) {
					String objectUUID = objectNode.getIdentifier();
					
					//Remove database rows with object UUID
					WasabiObjectSQL.SqlQueryForRemove(objectUUID);
					
					//Remove database rows (templates) where location (room or container) is object UUID
					if (objectType.equals(WasabiNodeType.ROOM))
						WasabiRoomSQL.SQLQueryForRemove(objectUUID);
					if (objectType.equals(WasabiNodeType.CONTAINER))
						WasabiContainerSQL.SQLQueryForRemove(objectUUID);
					
					ObjectServiceImpl.remove(objectNode);

					return 0;
				} else
					return 1;
			} else {
				for (Node node : childreenNodes) {
					childNodes = childNodes + removeRecursive(node, callerPrincipal, s);
				}
			}

			if (childNodes == 0 && WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s)) {
				String objectUUID = objectNode.getIdentifier();
				WasabiObjectSQL.SqlQueryForRemove(objectUUID);
				WasabiRoomSQL.SQLQueryForRemove(objectUUID);
				ObjectServiceImpl.remove(objectNode);

				return 0;
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
		return 1;
	}

	public static void removeACLEntriesRecursive(Node objectNode) throws UnexpectedInternalProblemException {
		Vector<Node> childreen = ACLServiceImpl.getChildren(objectNode);
		if (childreen.size() == 0)
			WasabiObjectSQL.SqlQueryForRemove(ObjectServiceImpl.getUUID(objectNode));
		else
			for (Node node : childreen)
				removeACLEntriesRecursive(node);
	}
}
