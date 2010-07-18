package de.wasabibeans.framework.server.core.testhelper;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Remote
public interface TestDataCreatorRemote {
	
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception;

	public void initDatabase();
	
	public WasabiRoomDTO initRoomServiceTest() throws Exception;
}
