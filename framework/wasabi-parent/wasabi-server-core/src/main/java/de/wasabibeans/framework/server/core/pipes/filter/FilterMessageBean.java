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

package de.wasabibeans.framework.server.core.pipes.filter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = WasabiConstants.JMS_QUEUE_PIPELINE),
		@ActivationConfigProperty(propertyName = "user", propertyValue = WasabiConstants.JMS_EVENT_ADMIN),
		@ActivationConfigProperty(propertyName = "password", propertyValue = WasabiConstants.JMS_EVENT_ADMIN_PASSWORD) })
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FilterMessageBean implements MessageListener {

	@EJB
	private SharedFilterBean sharedFilterBean;

	private JndiConnector jndi;
	private JcrConnector jcr;
	private JmsConnector jms;

	@PostConstruct
	protected void postConstruct() {
		jndi = JndiConnector.getJNDIConnector();
		jcr = JcrConnector.getJCRConnector(jndi);
		jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	@Override
	public void onMessage(Message message) {
		try {
			SharedFilterBean.Task task = sharedFilterBean.getTaskForExecution(message.getLongProperty("taskId"));
			Wire wire = task.getWire();
			Filter.DocumentInfo info = task.getInfo().clone();
			byte[] data = task.getData();

			wire.to.sink.filter(wire, info, data, jcr.getJCRSessionTx(), jms, sharedFilterBean);
			sharedFilterBean.finishTask(task);
		} catch (JMSException e) {
			throw new RuntimeException(e);
		} catch (UnexpectedInternalProblemException e) {
			throw new RuntimeException(e);
		}
	}
}
