package de.wasabibeans.framework.server.core.manager;

import javax.ejb.Remote;

@Remote
public interface WasabiManagerRemote {
	public void initWorkspace(String workspacename);
}
