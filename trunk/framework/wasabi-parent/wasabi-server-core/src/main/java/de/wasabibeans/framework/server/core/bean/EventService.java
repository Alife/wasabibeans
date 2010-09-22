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

package de.wasabibeans.framework.server.core.bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.event.EventSubscriptions;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.EventServiceLocal;
import de.wasabibeans.framework.server.core.remote.EventServiceRemote;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless(name = "EventService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EventService implements EventServiceLocal, EventServiceRemote {

	@Resource
	protected SessionContext ctx;

	private JndiConnector jndi;
	private JmsConnector jms;

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	public void subscribe(WasabiObjectDTO object, String jmsDestinationName, boolean isQueue)
			throws UnexpectedInternalProblemException {
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Connection jmsConnection = jms.getJmsConnection();
			// check that the parameters jmsDestinationName and isQueue actually match
			Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			try {
				if (isQueue) {
					jmsSession.createQueue(jmsDestinationName);
				} else {
					jmsSession.createTopic(jmsDestinationName);
				}
			} catch (JMSException je) {
				throw new IllegalArgumentException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.JMS_DESTINATION_INVALID, isQueue ? "queue" : "topic"));
			}

			EventSubscriptions.subscribe(object.getId(), callerPrincipal, jmsDestinationName, isQueue);
		} catch (JMSException je) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JMS_PROVIDER_FAILURE, je);
		} finally {
			jms.close();
		}
	}

	public void unsubscribe(WasabiObjectDTO object) {
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		EventSubscriptions.unsubscribe(object.getId(), callerPrincipal);
	}
}