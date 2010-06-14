package de.wasabibeans.framework.server.core.common;

import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import de.wasabibeans.framework.server.core.auth.SimpleUsrPwdCallbackHandler;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;

public class WasabiConnection {

	private boolean connected = false;
	private boolean loggedIn = false;
	private InitialContext initialContext = null;
	private LoginContext loginContext = null;
	private String username;

	private hashAlgorithms hashAlgorithm = hashAlgorithms.SHA;

	Hashtable<String, String> environment = new Hashtable<String, String>();

	public WasabiConnection() {

	}

	public void connect(String host, String port) throws NamingException {
		if (host == null || port == null)
			throw new IllegalArgumentException(WasabiExceptionMessages.WASABI_CONNECTION_ILLEGAL_ARGUMENTS);

		environment.clear();

		//environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
		//environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
		environment.put(Context.PROVIDER_URL, host + ":" + port);

		initialContext = new InitialContext(environment);

		connected = true;
	}

	public void login(String username, String password) throws NamingException, LoginException {

		if (!connected)
			throw new NoInitialContextException(WasabiExceptionMessages.WASABI_CONNECTION_NOT_CONNECTED);
		if (loggedIn)
			throw new LoginException(WasabiExceptionMessages.WASABI_CONNECTION_ALREADY_LOGGED_IN);

		// TODO: Modulspezifische Passwortverschlüsseltung noch berücksichtigen

		Configuration.setConfiguration(new Configuration() {
			public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
				HashMap<String, String> properties = new HashMap<String, String>();

				return new AppConfigurationEntry[] { new AppConfigurationEntry(
						"de.wasabibeans.framework.server.core.auth.UsernamePasswordLoginModule",
						LoginModuleControlFlag.REQUIRED, properties) };
			}

			public void refresh() {

			}
		});

		loginContext = new LoginContext("wasabi", new SimpleUsrPwdCallbackHandler(username, password));
		loginContext.login();
		this.username = username;
		initialContext = new InitialContext(environment);
		loggedIn = true;

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

	public void logout() throws LoginException, NamingException {
		if (loggedIn) {
			loginContext.logout();
		}
		if (connected) {
			initialContext.close();
		}
		loggedIn = false;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isLoggedIn() {
		return loggedIn;
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

	public InitialContext getInitialContext() {
		return initialContext;
	}

	public LoginContext getLoginContext() {
		return loginContext;
	}

	public void changeInitialContext(String username) {

		try {
			environment.remove(Context.SECURITY_PRINCIPAL);
			environment.put(Context.SECURITY_PRINCIPAL, username);

			initialContext = new InitialContext(environment);
		} catch (NamingException e) {
		}
	}
}
