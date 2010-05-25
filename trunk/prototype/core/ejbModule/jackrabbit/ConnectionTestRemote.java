package jackrabbit;
import javax.ejb.Remote;

import org.wasabibeans.server.core.transfer.model.dto.WasabiRoomDTO;

@Remote
public interface ConnectionTestRemote {
	public WasabiRoomDTO login();
}
