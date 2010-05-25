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

package org.wasabibeans.server.core.local.model.service.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.wasabibeans.server.core.internal.model.service.ObjectServiceInternal;
import org.wasabibeans.server.core.local.model.service.ObjectServiceLocal;
import org.wasabibeans.server.core.transfer.model.dto.TransferManager;
import org.wasabibeans.server.core.transfer.model.dto.WasabiObjectDTO;

/**
 * Class, that implements the local access on WasabiObjectDTO objects.
 */
@Stateless(name = "ObjectServiceLocal")
public class ObjectServiceLocalImpl extends TransferManager implements
		ObjectServiceLocal {

	@EJB
	private ObjectServiceInternal objectService;

	public String getName(WasabiObjectDTO object) {
		return objectService.getName(convertDTO2Node(object));
	}

	public String getUUID(WasabiObjectDTO object) {
		return objectService.getUUID(convertDTO2Node(object));
	}
}
