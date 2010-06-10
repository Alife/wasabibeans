package de.wasabibeans.framework.server.core.jcrTest;
import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

@Local
public interface ConnectionTestLocal {
	public WasabiRoomDTO login();
	public String printNodeTypes();
}
