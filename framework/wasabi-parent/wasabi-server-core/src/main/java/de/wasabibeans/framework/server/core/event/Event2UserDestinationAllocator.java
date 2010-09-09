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

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jmx.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = WasabiConstants.JMS_QUEUE_ALLOCATOR) })
public class Event2UserDestinationAllocator implements MessageListener {

	private static WasabiLogger logger = WasabiLogger.getLogger(Event2UserDestinationAllocator.class);

	private JmsConnector jms;

	@PostConstruct
	protected void postConstruct() {
		jms = JmsConnector.getJmsConnector();
	}

	@Override
	public void onMessage(Message message) {
		Connection jmsConnection = null;
		try {
			jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			jmsConnection.start();
			// create producer without specified destination
			MessageProducer jmsProducer = jmsSession.createProducer(null);

			// subscribers of the affected object (e.g. create new document in room, the document is the affected
			// object)
			String objectId = message.getStringProperty(WasabiEventProperty.OBJECT_ID);
			sendEvents(objectId, message, jmsProducer);

			// subscribers of the environment (e.g. create new document in room, the room is the environment)
			objectId = message.getStringProperty(WasabiEventProperty.ENV_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer);
			}

			// subscribes of the new environment (e.g. move document from roomA to roomB, roomA is the environment,
			// roomB is the new environment)
			objectId = message.getStringProperty(WasabiEventProperty.NEW_ENV_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer);
			}

			// special case: membership events -> subscribers of the member (e.g. add member to group, the group is the
			// affected object, but subscribers of the member must be informed as well)
			objectId = message.getStringProperty(WasabiEventProperty.MEMBER_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer);
			}
		} catch (Exception e) {
			logger.warn("An event could not be dispatched to all subscribers");
		} finally {
			jms.close(jmsConnection);
		}
	}

	private void sendEvents(String objectId, Message message, MessageProducer jmsProducer) {
		for (Message registrationMessage : EventSubscriptions.getSubscribers(objectId)) {
			try {
				jmsProducer.send(registrationMessage.getJMSReplyTo(), message);
			} catch (InvalidDestinationException ide) {
				// client has closed his jms session and his temporary destination (JMSReplyTo) does not exist any more
				// unsubscribe the client
				try {
					EventSubscriptions.unsubscribe(objectId, registrationMessage
							.getStringProperty(WasabiEventRegistration.USERNAME));
				} catch (JMSException e) {
					logger.warn("Could not properly unsubscribe a subscriber");
				}
			} catch (JMSException e) {
				logger.warn("An event could not be dispatched to one subscriber");
			}
		}
	}
}
