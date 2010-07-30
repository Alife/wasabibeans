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

package de.wasabibeans.framework.server.core.test.util;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import de.wasabibeans.framework.server.core.authentication.SimpleUsrPwdCallbackHandler;

/**
 * Class for local JAAS authentication and local EJB lookups. Must be used by tests which run in IN_CONTAINER mode.
 */
public class LocalWasabiConnector {

	private LoginContext loginContext = null;
	private InitialContext initialContext = null;
	private boolean loggedIn = false;
	private boolean connected = false;

	public void connect() throws NamingException {
		initialContext = new InitialContext();
		connected = true;
	}

	public void login(String username, String pwd) throws LoginException, NoInitialContextException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		if (loggedIn) {
			throw new LoginException("Already logged in.");
		}

		// authenticate via jaas (that is, use ClientLoginModule of jboss which does not really authenticate ->
		// authentication happens for each service call due to configured security domain)
		loginContext = new LoginContext("client-login", new SimpleUsrPwdCallbackHandler(username, pwd));
		loginContext.login();

		loggedIn = true;
	}

	public void defaultConnectAndLogin() throws LoginException, NamingException {
		connect();
		login("root", "meerrettich");
	}

	public void defaultLogin() throws LoginException, NoInitialContextException {
		login("root", "meerrettich");
	}

	public void logout() throws LoginException {
		if (loggedIn) {
			loginContext.logout();
		}
		loggedIn = false;
	}

	public void disconnect() throws LoginException, NamingException {
		if (loggedIn) {
			loginContext.logout();
		}
		if (connected) {
			initialContext.close();
		}
		loggedIn = false;
		connected = false;
	}

	public Object lookup(String name) throws NamingException, LoginException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		if (!loggedIn) {
			throw new LoginException("Not logged in.");
		}
		return initialContext.lookup("test/" + name + "/local");
	}

	public Object lookupGeneral(String name) throws NamingException, LoginException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		if (!loggedIn) {
			throw new LoginException("Not logged in.");
		}
		return initialContext.lookup(name);
	}

	public void bind(String name, Object o) throws LoginException, NamingException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		if (!loggedIn) {
			throw new LoginException("Not logged in.");
		}
		initialContext.bind(name, o);
	}

	public void unbind(String name) throws LoginException, NamingException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		if (!loggedIn) {
			throw new LoginException("Not logged in.");
		}
		try {
			initialContext.unbind(name);
		} catch (NameNotFoundException nnfe) {

		}
	}
}
