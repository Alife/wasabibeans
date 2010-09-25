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

/**
 * Created by IntelliJ IDEA. User: markus Date: 12.03.2010 Time: 08:28:15 To change this template use File | Settings |
 * File Templates.
 */
public abstract class AuthTokenDelegate<T extends AuthTokenProvider.Parameter> {
	protected final AuthTokenProvider provider;
	T parameter;

	public AuthTokenDelegate(AuthTokenProvider provider) {
		this.provider = provider;
	}

	public void startAuth() {
		provider.startAuth(this);
	}

	public void confirmAuth() {
		provider.confirmAuth(this);
	}

	public String getName() {
		return provider.name;
	}

	public abstract String getToken();

	protected abstract void setToken(String token);

	public abstract void deleteToken();

	public URL getUrl() {
		return parameter == null ? null : parameter.url;
	}

	protected String getAttributeName() {
		return "auth_provider_" + getName();
	}
}
