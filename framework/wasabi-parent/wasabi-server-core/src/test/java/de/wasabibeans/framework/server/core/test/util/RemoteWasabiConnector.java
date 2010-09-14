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

import java.util.HashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import de.wasabibeans.framework.server.core.authentication.SimpleUsrPwdCallbackHandler;

/**
 * Class for remote JAAS authentication and remote EJB lookups. Must be used by tests which run in AS_CLIENT mode. The
 * applied JNDI properties for JNDI lookups are specified in src/test/resources/jndi.properties.
 * 
 */
public class RemoteWasabiConnector {

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

		// set a jaas configuration
		Configuration.setConfiguration(new Configuration() {
			public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
				HashMap<String, String> options = new HashMap<String, String>();

				return new AppConfigurationEntry[] { new AppConfigurationEntry("org.jboss.security.ClientLoginModule",
						LoginModuleControlFlag.REQUIRED, options) };

			}

			public void refresh() {

			}
		});

		// authenticate via jaas (that is, use ClientLoginModule of jboss which does not really authenticate ->
		// authentication must happen on the server)
		// name parameter doesn't matter due to use of anonymous class above
		loginContext = new LoginContext("doesntMatter", new SimpleUsrPwdCallbackHandler(username, pwd));
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

	public void disconnect() {
		try {
			if (loggedIn) {
				loginContext.logout();
			}
			if (connected) {
				initialContext.close();
			}
		} catch (LoginException e) {
			System.out.println("Could not properly close login context.");
		} catch (NamingException e) {
			System.out.println("Could not properly close jndi context.");
		}
		loggedIn = false;
		connected = false;
	}

	public Object lookup(String name) throws NamingException, LoginException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		return initialContext.lookup(name + "/remote");
	}

	public Object lookupGeneral(String name) throws NamingException, LoginException {
		if (!connected) {
			throw new NoInitialContextException("Not connected.");
		}
		return initialContext.lookup(name);
	}
}
