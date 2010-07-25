package de.wasabibeans.framework.server.core.testhelper;

import java.util.Vector;
import java.util.concurrent.Callable;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Local
public interface TestHelperLocal {
	
	public void initDatabase();
	
	public void initRepository() throws Exception;
	
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception;
	
	public WasabiRoomDTO initRoomServiceTest() throws Exception;
	
	public <V> V call(Callable<V> callable) throws Exception;
	
	public Vector<String> createManyNodes(int number) throws Exception;
	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception;
	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception;
}
