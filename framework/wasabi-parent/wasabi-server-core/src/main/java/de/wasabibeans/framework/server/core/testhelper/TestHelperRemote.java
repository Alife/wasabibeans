package de.wasabibeans.framework.server.core.testhelper;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Remote
public interface TestHelperRemote {
	
	public void initDatabase();
	
	public void initRepository() throws Exception;
	
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception;
	
	public WasabiRoomDTO initRoomServiceTest() throws Exception;
}
