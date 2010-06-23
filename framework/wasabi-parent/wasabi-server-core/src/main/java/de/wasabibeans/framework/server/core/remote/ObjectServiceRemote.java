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

package de.wasabibeans.framework.server.core.remote;

import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;

/**
 * Interface, that defines the remote access on WasabiObjectDTO objects.
 */
@Remote
public interface ObjectServiceRemote {

	public boolean exists(WasabiObjectDTO object);

	public WasabiUserDTO getCreatedBy(WasabiObjectDTO object);

	public Date getCreatedOn(WasabiObjectDTO object);

	public WasabiUserDTO getModifiedBy(WasabiObjectDTO object);

	public Date getModifiedOn(WasabiObjectDTO object);

	public boolean isRightsActive(WasabiObjectDTO object);

	public String getName(WasabiObjectDTO object);

	public Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName);

	public Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator);

	public Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier);

	public Vector<WasabiObjectDTO> getObjectsByName(String name);

	public String getUUID(WasabiObjectDTO object);

	public void setCreatedBy(WasabiObjectDTO object, WasabiUserDTO user);

	public void setCreatedOn(WasabiObjectDTO object, Date creationTime);

	public void setModifiedBy(WasabiObjectDTO object, WasabiUserDTO user);

	public void setModifiedOn(WasabiObjectDTO object, Date modificationTime);

	public void setRightsActive(WasabiObjectDTO object, boolean rightsActive);

}
