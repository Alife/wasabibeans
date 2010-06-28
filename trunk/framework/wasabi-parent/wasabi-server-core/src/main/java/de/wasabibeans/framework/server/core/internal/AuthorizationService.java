/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.internal;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.AuthorizationServiceLocal;
import de.wasabibeans.framework.server.core.remote.AuthorizationServiceRemote;

public class AuthorizationService implements AuthorizationServiceLocal,AuthorizationServiceRemote {

	@Override
	public boolean hasPermission(WasabiObjectDTO wasabiObject, WasabiUserDTO wasabiUser, int permission) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPermission(WasabiObjectDTO wasabiObject, int permission) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean returnTrue() {
		// TODO Auto-generated method stub
		return false;
	}

}
