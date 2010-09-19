package de.wasabibeans.framework.server.core.aop;

import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

public abstract class WasabiService {

	protected JndiConnector jndi;
	protected JcrConnector jcr;
	protected JmsConnector jms;
	
	protected JcrConnector getJcrConnector() {
		return jcr;
	}
}
