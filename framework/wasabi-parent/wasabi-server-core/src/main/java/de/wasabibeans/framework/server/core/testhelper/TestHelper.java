package de.wasabibeans.framework.server.core.testhelper;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;

import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.manager.WasabiManager;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateful
public class TestHelper implements TestHelperRemote {

	private Node wasabiRootRoom;

	@Override
	public void initDatabase() {
		WasabiManager.initDatabase();

	}
	
	@Override
	public void initRepository() throws Exception {
		WasabiManager.initRepository();
	}

	@Override
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception {
		wasabiRootRoom = WasabiManager.initWorkspace("default");
		return TransferManager.convertNode2DTO(wasabiRootRoom);
	}

	@Override
	public WasabiRoomDTO initRoomServiceTest() throws Exception {
		return TransferManager.convertNode2DTO(wasabiRootRoom.addNode(WasabiNodeProperty.ROOMS + "/" + "room1",
				WasabiNodeType.WASABI_ROOM));
	}

}
