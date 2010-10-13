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

public class WasabiCertificateHandle {

	private static long counter = 0;
	private String objectUUID;
	private int permission;
	private String userUUID;

	public WasabiCertificateHandle() {
		counter++;
	}

	@SuppressWarnings("static-access")
	public long getID() {
		return this.counter;
	}

	public String getObjectUUID() {
		return this.objectUUID;
	}

	public int getPermission() {
		return this.permission;
	}

	public String getUserUUID() {
		return this.userUUID;
	}

	public void setObjectUUID(String objectUUID) {
		this.objectUUID = objectUUID;
	}

	public void setPermission(int permission) {
		this.permission = permission;
	}

	public void setUserUUID(String userUUID) {
		this.userUUID = userUUID;
	}
}
