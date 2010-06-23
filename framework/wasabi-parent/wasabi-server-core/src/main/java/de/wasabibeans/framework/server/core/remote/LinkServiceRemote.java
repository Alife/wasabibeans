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

import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;

/**
 * Interface, that defines the remote access on WasabiLinkDTO objects.
 */
@Remote
public interface LinkServiceRemote extends ObjectServiceRemote {

	public WasabiLinkDTO create(String name, WasabiObjectDTO destination, WasabiLocationDTO environment);

	public WasabiObjectDTO getDestination(WasabiLinkDTO link) throws DestinationNotFoundException;

	public WasabiLocationDTO getEnvironment(WasabiLinkDTO link);

	public WasabiLinkDTO getLinkByName(WasabiLocationDTO location, String name);

	public Collection<WasabiLinkDTO> getLinks(WasabiLocationDTO location);

	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate);

	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate,
			int depth);

	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator);

	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator, WasabiLocationDTO environment);

	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate, Date endDate);

	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth);

	public Vector<WasabiLinkDTO> getLinksOrderedByCreationDate(WasabiLocationDTO location, SortType order);

	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier);

	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment);

	public void move(WasabiLinkDTO link, WasabiLocationDTO newEnvironment);

	public void remove(WasabiLinkDTO link);

	public void rename(WasabiLinkDTO link, String name);

	public void setDestination(WasabiLinkDTO link, WasabiObjectDTO object);
}
