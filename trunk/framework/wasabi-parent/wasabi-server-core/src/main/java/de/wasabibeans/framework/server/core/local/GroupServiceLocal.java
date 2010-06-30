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

package de.wasabibeans.framework.server.core.local;

import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;

/**
 * Interface, that defines the local access on WasabiGroupDTO objects.
 */
@Local
public interface GroupServiceLocal extends ObjectServiceLocal {

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

	public Vector<WasabiGroupDTO> getTopLevelGroups();

}
