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

package de.wasabibeans.framework.server.core.internal;

import java.util.Collection;

import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.local.ACLServiceLocal;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;

public class ACLService implements ACLServiceLocal, ACLServiceRemote {

	@Override
	public void activateInheritance(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivateInheritance(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<WasabiACLEntryDTO> getACLEntries(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<WasabiACLEntryDTO> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPermission(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAllowance(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isExplicitRight(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInheritanceAllowed(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showAllACLEntries(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

}
