package de.wasabibeans.framework.server.core.jcrTest;
import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Remote
public interface ConnectionTestRemote {
	public WasabiRoomDTO login();
	public String printNodeTypes();
}