package de.wasabibeans.framework.server.core.testhelper;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.manager.WasabiManager;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateless
public class TestDataCreator implements TestDataCreatorRemote {

	@Override
	public void initDatabase() {
		WasabiManager.initDatabase();
		
	}

	@Override
	public WasabiRoomDTO initWorkspace(String workspacename) {
		return WasabiManager.initWorkspace("default");
	}
	
}
