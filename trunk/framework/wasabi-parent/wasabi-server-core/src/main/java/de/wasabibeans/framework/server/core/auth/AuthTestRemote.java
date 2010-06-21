package de.wasabibeans.framework.server.core.auth;

import javax.ejb.Remote;

@Remote
public interface AuthTestRemote {
	public String HelloWorld();
}
