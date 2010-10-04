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

import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.jboss.ejb3.annotation.ResourceAdapter;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.event.EventSubscriptions.SubscriptionInfo;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jmx.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = WasabiConstants.JMS_QUEUE_ALLOCATOR),
		@ActivationConfigProperty(propertyName = "user", propertyValue = WasabiConstants.JMS_EVENT_ADMIN),
		@ActivationConfigProperty(propertyName = "password", propertyValue = WasabiConstants.JMS_EVENT_ADMIN_PASSWORD) })
@ResourceAdapter(WasabiConstants.JMS_RESOURCE_ADAPTER)
public class Event2UserDestinationAllocator implements MessageListener {

	private static WasabiLogger logger = WasabiLogger.getLogger(Event2UserDestinationAllocator.class);

	private JndiConnector jndi;
	private JmsConnector jms;

	@PostConstruct
	protected void postConstruct() {
		jndi = JndiConnector.getJNDIConnector();
		jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	@Override
	public void onMessage(Message message) {
		try {
			Connection jmsConnection = jms.getJmsConnection();
			Session jmsSession = jmsConnection.createSession(true, 0);
			// create producer without specified destination
			MessageProducer jmsProducer = jmsSession.createProducer(null);
			jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// subscribers of the affected object (e.g. create new document in room, the document is the affected
			// object)
			String objectId = message.getStringProperty(WasabiEventProperty.OBJECT_ID);
			sendEvents(objectId, message, jmsProducer, jmsSession);

			// subscribers of the environment (e.g. create new document in room, the room is the environment)
			objectId = message.getStringProperty(WasabiEventProperty.ENV_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer, jmsSession);
			}

			// subscribes of the new environment (e.g. move document from roomA to roomB, roomA is the environment,
			// roomB is the new environment)
			objectId = message.getStringProperty(WasabiEventProperty.NEW_ENV_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer, jmsSession);
			}

			// special case: membership events -> subscribers of the member (e.g. add member to group, the group is the
			// affected object, but subscribers of the member must be informed as well)
			objectId = message.getStringProperty(WasabiEventProperty.MEMBER_ID);
			if (objectId != null) {
				sendEvents(objectId, message, jmsProducer, jmsSession);
			}
		} catch (Exception e) {
			logger.warn("An event could not be dispatched to all subscribers");
		} finally {
			jms.close();
		}
	}

	private void sendEvents(String objectId, Message message, MessageProducer jmsProducer, Session jmsSession) {
		Set<Entry<String, SubscriptionInfo>> subscribers = EventSubscriptions.getSubscribers(objectId);
		if (subscribers == null) {
			return;
		}
		// get all subscribers
		for (Entry<String, SubscriptionInfo> subscriber : subscribers) {
			try {
				SubscriptionInfo info = subscriber.getValue();
				// TODO check read permission
				// get temporary destination of subscriber (queue or topic) and send message
				if (info.isQueue()) {
					jmsProducer.send(jmsSession.createQueue(info.getJmsDestinationName()), message);
				} else {
					jmsProducer.send(jmsSession.createTopic(info.getJmsDestinationName()), message);
				}
			} catch (InvalidDestinationException ide) {
				// client has closed his jms session and his temporary destination does not exist any more
				// unsubscribe the client
				EventSubscriptions.unsubscribe(objectId, subscriber.getKey());
			} catch (JMSException e) {
				logger.warn("An event could not be dispatched to one subscriber");
			}
		}
	}
}
