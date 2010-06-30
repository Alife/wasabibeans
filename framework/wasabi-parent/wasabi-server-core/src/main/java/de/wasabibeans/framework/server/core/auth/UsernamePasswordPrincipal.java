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

package de.wasabibeans.framework.server.core.auth;

import java.security.Principal;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;

public class UsernamePasswordPrincipal implements Principal, java.io.Serializable {

	private static final long serialVersionUID = -6659927928185347112L;

	private String name;

	public UsernamePasswordPrincipal(String name) {
		if (name == null)
			throw new IllegalArgumentException(WasabiExceptionMessages.USERNAME_PASSWORD_PRINCIPAL_NULL_NAME);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return ("NamePasswordPrincipal:  " + name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UsernamePasswordPrincipal))
			return false;
		UsernamePasswordPrincipal other = (UsernamePasswordPrincipal) obj;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
