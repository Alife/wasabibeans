package de.wasabibeans.framework.server.core.aop;

import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

public interface WasabiAOP {

	public JndiConnector getJndiConnector();

	public JcrConnector getJcrConnector();
}
