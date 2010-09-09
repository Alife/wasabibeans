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

package de.wasabibeans.framework.server.core.util;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class JmsConnector {

	private static WasabiLogger logger = WasabiLogger.getLogger(JmsConnector.class);

	private ConnectionFactory jmsConnectionFactory;
	private Queue allocatorQueue;
	private JndiConnector jndi;

	public static JmsConnector getJmsConnector() {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		return new JmsConnector(jndi);
	}

	public static JmsConnector getJmsConnector(JndiConnector jndi) {
		return new JmsConnector(jndi);
	}

	public JmsConnector(JndiConnector jndi) {
		this.jndi = jndi;
	}

	public ConnectionFactory getJmsConnectionFactory() throws UnexpectedInternalProblemException {
		if (this.jmsConnectionFactory == null) {
			this.jmsConnectionFactory = (ConnectionFactory) jndi.lookup(WasabiConstants.JNDI_JMS_DATASOURCE);
		}
		return this.jmsConnectionFactory;
	}

	public Connection getJmsConnection() throws UnexpectedInternalProblemException {
		try {
			return getJmsConnectionFactory().createConnection();
		} catch (JMSException je) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JMS_PROVIDER_FAILURE, je);
		}
	}
	
	public Queue getAllocatorQueue() throws UnexpectedInternalProblemException {
		if (this.allocatorQueue == null) {
			this.allocatorQueue = (Queue) jndi.lookup(WasabiConstants.JMS_QUEUE_ALLOCATOR);
		}
		return this.allocatorQueue;
	}

	public void close(Connection jmsConnection) {
		try {
			if (jmsConnection != null) {
				jmsConnection.close();
			}
		} catch (JMSException je) {
			logger.warn("Could not return JMS connection to JCA pool.");
		}
	}
}
