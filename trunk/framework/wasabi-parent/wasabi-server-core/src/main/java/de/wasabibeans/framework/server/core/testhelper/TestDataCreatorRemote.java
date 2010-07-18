package de.wasabibeans.framework.server.core.testhelper;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Remote
public interface TestDataCreatorRemote {
	
	public WasabiRoomDTO initWorkspace(String workspacename);

	public void initDatabase();
}
