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

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;

@Local
public interface ACLServiceRemote {

	public void activateInheritance(WasabiObjectDTO wasabiObject);

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance);

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime);

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission);

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime);

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission);

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime);

	public void deactivateInheritance(WasabiObjectDTO wasabiObject);

	public void reset(WasabiObjectDTO wasabiObject);

	public void showAllACLEntries(WasabiObjectDTO wasabiObject);

	public int getPermission(WasabiACLEntryDTO wasabiACLEntry);

	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTO wasabiACLEntry);

	public boolean isAllowance(WasabiACLEntryDTO wasabiACLEntry);

	public boolean isExplicitRight(WasabiACLEntryDTO wasabiACLEntry);

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance);

	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission);

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance);

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime);

	public boolean isInheritanceAllowed(WasabiObjectDTO object);

	public Collection<WasabiACLEntryDTO> getACLEntries(WasabiObjectDTO wasabiObject);

	public Collection<WasabiACLEntryDTO> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity);
}