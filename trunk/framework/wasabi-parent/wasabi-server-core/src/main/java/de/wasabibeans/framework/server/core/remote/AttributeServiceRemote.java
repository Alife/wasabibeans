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

import java.io.Serializable;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;

/**
 * Interface, that defines the remote access on WasabiAttributeDTO objects.
 */
@Remote
public interface AttributeServiceRemote extends ObjectServiceRemote {

	public WasabiAttributeDTO create(String name, Serializable value, WasabiObjectDTO affiliation);

	public WasabiAttributeDTO create(String name, WasabiObjectDTO value, WasabiObjectDTO affiliation);

	public WasabiObjectDTO getAffiliation(WasabiAttributeDTO attribute);

	public WasabiAttributeDTO getAttributeByName(WasabiObjectDTO object, String name);

	public Vector<WasabiAttributeDTO> getAttributes(WasabiObjectDTO object);

	public String getAttributeType(WasabiAttributeDTO attribute);

	public <T extends Serializable> T getValue(Class<T> type, WasabiAttributeDTO attribute);

	public WasabiObjectDTO getWasabiValue(WasabiAttributeDTO attribute);

	public void move(WasabiAttributeDTO attribute, WasabiObjectDTO newAffiliation);

	public void remove(WasabiAttributeDTO attribute);

	public void rename(WasabiAttributeDTO attribute, String name);

	public void setValue(WasabiAttributeDTO attribute, Serializable value);

	public void setWasabiValue(WasabiAttributeDTO attribute, WasabiObjectDTO value);

}
