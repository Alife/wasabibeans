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

package de.wasabibeans.framework.server.core.event;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class EventCreator {

	private static WasabiLogger logger = WasabiLogger.getLogger(EventCreator.class);

	public static void createPropertyChangedEvent(Node objectNode, String propertyName, Object value, JmsConnector jms,
			String triggeredBy) {
		try {
			Node envNode = null;
			try {
				envNode = objectNode.getParent().getParent();
			} catch (RepositoryException e) {
				// no parent available
			}

			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.PROPERTY_CHANGED);
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, objectNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, objectNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, getWasabiType(objectNode));
			if (envNode != null) {
				event.setStringProperty(WasabiEventProperty.ENV_ID, envNode.getIdentifier());
				event.setStringProperty(WasabiEventProperty.ENV_NAME, envNode.getName());
				event.setStringProperty(WasabiEventProperty.ENV_TYPE, getWasabiType(envNode));
			}
			event.setStringProperty(WasabiEventProperty.PROPERTY_NAME, propertyName);
			if (value instanceof Node) {
				event.setStringProperty(WasabiEventProperty.PROPERTY_TYPE, getWasabiType((Node) value));
			} else {
				event.setStringProperty(WasabiEventProperty.PROPERTY_TYPE, value.getClass().getName());
			}

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	public static void createCreatedEvent(Node objectNode, Node envNode, JmsConnector jms, String triggeredBy) {
		try {
			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.CREATED);
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, objectNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, objectNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, getWasabiType(objectNode));
			if (envNode != null) {
				event.setStringProperty(WasabiEventProperty.ENV_ID, envNode.getIdentifier());
				event.setStringProperty(WasabiEventProperty.ENV_NAME, envNode.getName());
				event.setStringProperty(WasabiEventProperty.ENV_TYPE, getWasabiType(envNode));
			}

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	public static void createRemovedEvent(Node objectNode, JmsConnector jms, String triggeredBy) {
		try {
			Node envNode = null;
			try {
				envNode = objectNode.getParent().getParent();
			} catch (RepositoryException e) {
				// no parent available
			}

			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.REMOVED);
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, objectNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, objectNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, getWasabiType(objectNode));
			if (envNode != null) {
				event.setStringProperty(WasabiEventProperty.ENV_ID, envNode.getIdentifier());
				event.setStringProperty(WasabiEventProperty.ENV_NAME, envNode.getName());
				event.setStringProperty(WasabiEventProperty.ENV_TYPE, getWasabiType(envNode));
			}

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	public static void createMovedEvent(Node objectNode, Node newEnv, JmsConnector jms, String triggeredBy) {
		try {
			Node envNode = null;
			try {
				envNode = objectNode.getParent().getParent();
			} catch (RepositoryException e) {
				// no parent available
			}

			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.MOVED);
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, objectNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, objectNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, getWasabiType(objectNode));
			if (envNode != null) {
				event.setStringProperty(WasabiEventProperty.ENV_ID, envNode.getIdentifier());
				event.setStringProperty(WasabiEventProperty.ENV_NAME, envNode.getName());
				event.setStringProperty(WasabiEventProperty.ENV_TYPE, getWasabiType(envNode));
			}
			event.setStringProperty(WasabiEventProperty.NEW_ENV_ID, newEnv.getIdentifier());
			event.setStringProperty(WasabiEventProperty.NEW_ENV_NAME, newEnv.getName());
			event.setStringProperty(WasabiEventProperty.NEW_ENV_TYPE, getWasabiType(envNode));

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	public static void createUserMovementEvent(Node userNode, Node roomNode, boolean entered, JmsConnector jms,
			String triggeredBy) {
		try {
			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			if (entered) {
				event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.ROOM_ENTERED);
			} else {
				event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.ROOM_LEFT);
			}
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, userNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, userNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, WasabiType.USER);
			event.setStringProperty(WasabiEventProperty.ENV_ID, roomNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.ENV_NAME, roomNode.getName());
			event.setStringProperty(WasabiEventProperty.ENV_TYPE, WasabiType.ROOM);

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	public static void createMembershipEvent(Node groupNode, Node userNode, boolean added, JmsConnector jms,
			String triggeredBy) {
		try {
			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getAllocatorQueue());
			Message event = jmsSession.createMessage();

			if (added) {
				event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.MEMBER_ADDED);
			} else {
				event.setByteProperty(WasabiEventProperty.EVENT_TYPE, WasabiEventType.MEMBER_REMOVED);
			}
			event.setStringProperty(WasabiEventProperty.TRIGGERED_BY, triggeredBy);
			event.setStringProperty(WasabiEventProperty.OBJECT_ID, groupNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.OBJECT_NAME, groupNode.getName());
			event.setStringProperty(WasabiEventProperty.OBJECT_TYPE, WasabiType.GROUP);
			event.setStringProperty(WasabiEventProperty.MEMBER_ID, userNode.getIdentifier());
			event.setStringProperty(WasabiEventProperty.MEMBER_NAME, userNode.getName());

			jmsProducer.send(event);
		} catch (Exception e) {
			logger.warn("An event could not be forwarded to the allocator.");
		} finally {
			jms.close();
		}
	}

	private static String getWasabiType(Node node) throws RepositoryException {
		return node.getPrimaryNodeType().getName().split(":")[1];
	}
}
