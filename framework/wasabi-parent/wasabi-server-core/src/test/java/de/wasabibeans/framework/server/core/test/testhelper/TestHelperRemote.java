package de.wasabibeans.framework.server.core.test.testhelper;

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Remote
public interface TestHelperRemote {
	
	public void initDatabase();
	
	public void initRepository() throws Exception;
	
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception;
	
	public WasabiRoomDTO initRoomServiceTest() throws Exception;
	
	public Vector<String> createManyNodes(int number) throws Exception;
	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception;
	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception;
}
