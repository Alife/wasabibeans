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

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;

/**
 * Interface, that defines the remote access on WasabiGroupDTO objects.
 */
@Remote
public interface GroupServiceRemote extends ObjectServiceRemote {

	public void addMember(WasabiGroupDTO group, WasabiUserDTO user);

	public WasabiGroupDTO create(String name, WasabiGroupDTO parentGroup);

	public String getDisplayName(WasabiGroupDTO group);

	public WasabiGroupDTO getGroupByName(String groupName);

	public Vector<WasabiGroupDTO> getGroupsByDisplayName(String displayName);

	public WasabiUserDTO getMemberByName(WasabiGroupDTO group, String userName);

	public Vector<WasabiUserDTO> getMembers(WasabiGroupDTO group);

	public WasabiGroupDTO getParentGroup(WasabiGroupDTO group);

	public WasabiGroupDTO getSubGroupByName(WasabiGroupDTO group, String name);

	public Vector<WasabiGroupDTO> getSubGroups(WasabiGroupDTO group);

	public boolean isDirectMember(WasabiGroupDTO group, WasabiUserDTO user);

	public boolean isMember(WasabiGroupDTO group, WasabiUserDTO user);

	public void move(WasabiGroupDTO group, WasabiGroupDTO newParentGroup);

	public void remove(WasabiGroupDTO group);

	public void removeMember(WasabiGroupDTO group, WasabiUserDTO user);

	public void rename(WasabiGroupDTO group, String name);

	public void setDisplayName(WasabiGroupDTO group, String displayName);

	public Vector<WasabiGroupDTO> getAllGroups();
}
