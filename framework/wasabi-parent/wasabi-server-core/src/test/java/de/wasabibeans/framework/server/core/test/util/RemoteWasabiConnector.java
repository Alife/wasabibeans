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

import de.wasabibeans.framework.server.core.auth.SimpleUsrPwdCallbackHandler;

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
		return initialContext.lookup(name);
	}
}
