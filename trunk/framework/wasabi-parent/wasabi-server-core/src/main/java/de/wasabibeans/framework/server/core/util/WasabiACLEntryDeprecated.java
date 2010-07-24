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

package de.wasabibeans.framework.server.core.util;

import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;

public class WasabiACLEntryDeprecated {

	private long id;
	private int permission;
	private boolean allowance;
	private WasabiIdentityDTO wasabiIdentity;

	public WasabiACLEntryDeprecated() {
	}

	public boolean getAllowance() {
		return this.allowance;
	}

	public long getId() {
		return this.id;
	}

	public WasabiIdentityDTO getWasabiIdentity() {
		return this.wasabiIdentity;
	}

	public int getPermission() {
		return this.permission;
	}

	public void setAllowance(boolean allowance) {
		this.allowance = allowance;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setWasabiIdentity(WasabiIdentityDTO wasabiIdentity) {
		this.wasabiIdentity = wasabiIdentity;
	}

	public void setPermission(int permission) {
		this.permission = permission;
	}
}
