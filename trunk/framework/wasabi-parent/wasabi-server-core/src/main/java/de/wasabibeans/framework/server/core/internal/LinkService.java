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

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.local.LinkServiceLocal;
import de.wasabibeans.framework.server.core.remote.LinkServiceRemote;

/**
 * Class, that implements the internal access on WasabiLink objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "LinkService")
public class LinkService extends ObjectService implements LinkServiceLocal, LinkServiceRemote {

	@Override
	public WasabiLinkDTO create(String name, WasabiObjectDTO destination, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiObjectDTO getDestination(WasabiLinkDTO link) throws DestinationNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiLocationDTO getEnvironment(WasabiLinkDTO link) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiLinkDTO getLinkByName(WasabiLocationDTO location, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinks(WasabiLocationDTO location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate,
			int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiLinkDTO> getLinksOrderedByCreationDate(WasabiLocationDTO location, SortType order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(WasabiLinkDTO link, WasabiLocationDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiLinkDTO link) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiLinkDTO link, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDestination(WasabiLinkDTO link, WasabiObjectDTO object) {
		// TODO Auto-generated method stub

	}

}
