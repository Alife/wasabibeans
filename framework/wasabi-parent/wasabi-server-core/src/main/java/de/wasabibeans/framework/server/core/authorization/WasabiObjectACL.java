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
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.event.EventCreator;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.AttributeServiceImpl;
import de.wasabibeans.framework.server.core.internal.ContainerServiceImpl;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.LinkServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class WasabiObjectACL {

	public static void remove(Node objectNode, String callerPrincipal, Session s, boolean doJcrSave,
			boolean throwEvents, JmsConnector jms) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		try {
			removeRecursive(objectNode, callerPrincipal, s, throwEvents, jms);
			if (doJcrSave) {
				s.save();
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void removeACLEntriesRecursive(Node objectNode) throws UnexpectedInternalProblemException {
		Vector<Node> childreen = ACLServiceImpl.getChildren(objectNode);
		if (childreen.size() == 0)
			WasabiObjectSQL.SqlQueryForRemove(ObjectServiceImpl.getUUID(objectNode));
		else
			for (Node node : childreen)
				removeACLEntriesRecursive(node);
	}

	private static int removeRecursive(Node objectNode, String callerPrincipal, Session s, boolean throwEvents,
			JmsConnector jms) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			int childNodes = 0;
			Vector<Node> childreenNodes = new Vector<Node>();
			String objectType = objectNode.getPrimaryNodeType().getName();

			if (objectType.equals(WasabiNodeType.ROOM)) {
				NodeIterator RoomChildreen = RoomServiceImpl.getRooms(objectNode);
				NodeIterator ContainerChildreen = ContainerServiceImpl.getContainers(objectNode);
				NodeIterator AttributeChildreen = AttributeServiceImpl.getAttributes(objectNode);
				NodeIterator LinkChildreen = LinkServiceImpl.getLinks(objectNode);
				NodeIterator DocumentChildreen = DocumentServiceImpl.getDocuments(objectNode);

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
				NodeIterator ContainerChildreen = ContainerServiceImpl.getContainers(objectNode);
				NodeIterator AttributeChildreen = AttributeServiceImpl.getAttributes(objectNode);
				NodeIterator LinkChildreen = LinkServiceImpl.getLinks(objectNode);
				NodeIterator DocumentChildreen = DocumentServiceImpl.getDocuments(objectNode);

				while (ContainerChildreen.hasNext())
					childreenNodes.add(ContainerChildreen.nextNode());

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());

				while (LinkChildreen.hasNext())
					childreenNodes.add(LinkChildreen.nextNode());

				while (DocumentChildreen.hasNext())
					childreenNodes.add(DocumentChildreen.nextNode());
			} else if (objectType.equals(WasabiNodeType.DOCUMENT)) {
				NodeIterator AttributeChildreen = AttributeServiceImpl.getAttributes(objectNode);

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());
			} else if (objectNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ATTRIBUTE)) {
				NodeIterator AttributeChildreen = AttributeServiceImpl.getAttributes(objectNode);

				while (AttributeChildreen.hasNext())
					childreenNodes.add(AttributeChildreen.nextNode());
			} else if (objectNode.getPrimaryNodeType().getName().equals(WasabiNodeType.GROUP)) {
				NodeIterator GroupChildreen = GroupServiceImpl.getSubGroups(objectNode);

				while (GroupChildreen.hasNext())
					childreenNodes.add(GroupChildreen.nextNode());
			}

			if (childreenNodes.size() == 0) {
				if (WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s)) {
					String objectUUID = objectNode.getIdentifier();

					// Remove database rows with object UUID
					WasabiObjectSQL.SqlQueryForRemove(objectUUID);

					// Remove database rows (templates) where location (room or container) is object UUID
					if (objectType.equals(WasabiNodeType.ROOM))
						WasabiRoomSQL.SQLQueryForRemove(objectUUID);
					if (objectType.equals(WasabiNodeType.CONTAINER))
						WasabiContainerSQL.SQLQueryForRemove(objectUUID);

					if (throwEvents) {
						EventCreator.createRemovedEvent(objectNode, jms, callerPrincipal);
					}
					ObjectServiceImpl.remove(objectNode, s, false);

					return 0;
				} else
					return 1;
			} else {
				for (Node node : childreenNodes) {
					childNodes = childNodes + removeRecursive(node, callerPrincipal, s, throwEvents, jms);
				}
			}

			if (childNodes == 0 && WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s)) {
				String objectUUID = objectNode.getIdentifier();
				WasabiObjectSQL.SqlQueryForRemove(objectUUID);
				WasabiRoomSQL.SQLQueryForRemove(objectUUID);

				if (throwEvents) {
					EventCreator.createRemovedEvent(objectNode, jms, callerPrincipal);
				}
				ObjectServiceImpl.remove(objectNode, s, false);

				return 0;
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
		return 1;
	}
}
