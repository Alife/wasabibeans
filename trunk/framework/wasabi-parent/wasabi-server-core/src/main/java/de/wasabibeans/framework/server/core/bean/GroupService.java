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

import java.util.Vector;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.local.GroupServiceLocal;
import de.wasabibeans.framework.server.core.remote.GroupServiceRemote;

/**
 * Class, that implements the internal access on WasabiGroup objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "GroupService")
public class GroupService extends ObjectService implements GroupServiceLocal, GroupServiceRemote {

	@Override
	public void addMember(WasabiGroupDTO group, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public WasabiGroupDTO create(String name, WasabiGroupDTO parentGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiGroupDTO> getAllGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayName(WasabiGroupDTO group) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiGroupDTO getGroupByName(String groupName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiGroupDTO> getGroupsByDisplayName(String displayName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiUserDTO getMemberByName(WasabiGroupDTO group, String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiUserDTO> getMembers(WasabiGroupDTO group) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiGroupDTO getParentGroup(WasabiGroupDTO group) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiGroupDTO getSubGroupByName(WasabiGroupDTO group, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiGroupDTO> getSubGroups(WasabiGroupDTO group) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiGroupDTO> getTopLevelGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDirectMember(WasabiGroupDTO group, WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMember(WasabiGroupDTO group, WasabiUserDTO user) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(WasabiGroupDTO group, WasabiGroupDTO newParentGroup) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiGroupDTO group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeMember(WasabiGroupDTO group, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiGroupDTO group, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDisplayName(WasabiGroupDTO group, String displayName) {
		// TODO Auto-generated method stub

	}

}
