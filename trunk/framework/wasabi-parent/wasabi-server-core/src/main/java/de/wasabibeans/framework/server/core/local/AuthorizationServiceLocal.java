package de.wasabibeans.framework.server.core.local;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;

@Local
public interface AuthorizationServiceLocal {

	public boolean hasPermission(WasabiObjectDTO wasabiObject,
			WasabiUserDTO wasabiUser, int permission);

	public boolean hasPermission(WasabiObjectDTO wasabiObject, int permission);

	public boolean returnTrue();

}
