/* 
 * Copyright (C) 2007-2009 
 * Thorsten Hampel, Jonas Schulte, Daniel Buese,
 * Andreas Oberhoff, Thomas Bopp, and Robert Hinn
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

package de.wasabibeans.framework.server.core.remote.model.service.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import de.wasabibeans.framework.server.core.local.model.service.ObjectServiceLocal;
import de.wasabibeans.framework.server.core.remote.model.service.ObjectService;
import de.wasabibeans.framework.server.core.transfer.model.dto.WasabiObjectDTO;

/**
 * Class, that implements the remote access on WasabiObjectDTO objects.
 */
@Stateless(name = "ObjectService")
// @SecurityDomain("wasabi")
// @WebService(name = "ObjectService")
// @SOAPBinding(style = SOAPBinding.Style.RPC, parameterStyle =
// ParameterStyle.WRAPPED, use = Use.LITERAL)
// @WebContext(contextRoot = "/wasabibeans/services", authMethod = "BASIC",
// secureWSDLAccess = true)
public class ObjectServiceImpl implements ObjectService {

	@EJB
	private ObjectServiceLocal objectService;

	public String getName(WasabiObjectDTO object) {
		return objectService.getName(object);
	}

	public String getUUID(WasabiObjectDTO object) {
		return objectService.getUUID(object);
	}
}
