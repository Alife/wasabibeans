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

package de.wasabibeans.framework.server.core.internal.model.service;

import javax.ejb.Local;
import javax.jcr.Node;

/**
 * Interface, that defines the internal access on WasabiObject objects.
 */
@Local(de.wasabibeans.framework.server.core.internal.model.service.impl.ObjectServiceInternalImpl.class)
public interface ObjectServiceInternal {

	//public boolean exists(WasabiObject wasabiObject);

	//public WasabiUser getCreatedBy(WasabiObject wasabiObject);

	//public Date getCreatedOn(WasabiObject wasabiObject);

	//public WasabiUser getModifiedBy(WasabiObject wasabiObject);

	//public Date getModifiedOn(WasabiObject wasabiObject);
	
	//public boolean isRightsActive(WasabiObject wasabiObject);

	public String getName(Node node);

	//public Collection<WasabiObject> getObjectsByAttributeName(
	//		String attributeName);

	//public Collection<WasabiObject> getObjectsByCreator(WasabiUser creator);

	//public Collection<WasabiObject> getObjectsByModifier(WasabiUser modifier);

	//public Collection<WasabiObject> getObjectsByName(String name);

	public String getUUID(Node node);

	//public boolean removeSubObjects(WasabiObject wasabiObject);

	//public void setCreatedBy(WasabiObject wasabiObject, WasabiUser wasabiUser);

	//public void setCreatedOn(WasabiObject wasabiObject, Date creationTime);

	//public void setModifiedBy(WasabiObject wasabiObject, WasabiUser wasabiUser);

	//public void setModifiedOn(WasabiObject wasabiObject, Date modificationTime);
	
	//public void setRightsActive(WasabiObject wasabiObject, boolean rightsActive);

}
