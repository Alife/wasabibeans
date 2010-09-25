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

package de.wasabibeans.framework.server.core.pipes.auth;

import java.net.URL;

public abstract class AuthTokenProvider<T extends AuthTokenProvider.Parameter> {
	protected final String name;

	public AuthTokenProvider(String name) {
		this.name = name;
	}

	public abstract void startAuth(AuthTokenDelegate<T> delegate);

	public abstract void confirmAuth(AuthTokenDelegate<T> delegate);

	public String getName() {
		return name;
	}

	public static class Parameter {
		protected final URL url;

		Parameter(URL url) {
			this.url = url;
		}
	}
}
