/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *  
 *  Further information are online available at: http://www.wasabibeans.de
 */

package de.wasabibeans.framework.server.core.bean;

import java.util.Date;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.ContainerServiceLocal;
import de.wasabibeans.framework.server.core.remote.ContainerServiceRemote;

/**
 * Class, that implements the internal access on WasabiContainer objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "ContainerService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ContainerService extends ObjectService implements ContainerServiceLocal, ContainerServiceRemote {

	@Override
	public WasabiContainerDTO create(String name, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiContainerDTO getContainerByName(WasabiLocationDTO location, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainers(WasabiLocationDTO location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiContainerDTO> getContainersByNamePattern(WasabiLocationDTO environment, String pattern) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiLocationDTO getEnvironment(WasabiContainerDTO container) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(WasabiContainerDTO container, WasabiLocationDTO newEnvironment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiContainerDTO container) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiContainerDTO container, String name) {
		// TODO Auto-generated method stub

	}

}