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
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.jcr.Node;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.aop.JCRSessionInterceptor;
import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.event.EventSubscriptionsLocal;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.EventServiceLocal;
import de.wasabibeans.framework.server.core.remote.EventServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@SecurityDomain("wasabi")
@Stateless(name = "EventService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors( { JCRSessionInterceptor.class })
public class EventService implements EventServiceLocal, EventServiceRemote, WasabiAOP {

	@Resource
	protected SessionContext ctx;

	@EJB
	private EventSubscriptionsLocal eventSubscriptions;

	private JcrConnector jcr;
	private JmsConnector jms;
	private JndiConnector jndi;

	public JcrConnector getJcrConnector() {
		return jcr;
	}

	public JndiConnector getJndiConnector() {
		return jndi;
	}

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
		this.jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	public void subscribe(WasabiObjectDTO object, String jmsDestinationName, boolean isQueue)
			throws UnexpectedInternalProblemException, NoPermissionException, ObjectDoesNotExistException {
		javax.jcr.Session s = jcr.getJCRSession();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node objectNode = TransferManager.convertDTO2Node(object, s);

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "EventService.subscribe()",
								"READ", "object"));
			/* Authorization - End */

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

			eventSubscriptions.subscribe(object.getId(), callerPrincipal, jmsDestinationName, isQueue);
		} catch (JMSException je) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JMS_PROVIDER_FAILURE, je);
		} finally {
			jms.close();
		}
	}

	public void unsubscribe(WasabiObjectDTO object) {
		String callerPrincipal = ctx.getCallerPrincipal().getName();
		eventSubscriptions.unsubscribe(object.getId(), callerPrincipal);
	}
}
