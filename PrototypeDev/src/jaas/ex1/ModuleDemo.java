package jaas.ex1;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

public class ModuleDemo {

	public static void main(String[] args) throws LoginException {
		LoginContext loginContext = null;
		// System.setProperty("java.security.auth.login.config", "moduledemo.conf");

		Configuration.setConfiguration(new Configuration() {
			public AppConfigurationEntry[] getAppConfigurationEntry(String name) {

				HashMap<String, String> properties = new HashMap<String, String>();
				properties.put("userfile", "userdb");

				return new AppConfigurationEntry[] { new AppConfigurationEntry("jaas.ex1.UserFileLoginModule",
						LoginModuleControlFlag.REQUIRED, properties) };
			}

			public void refresh() {
			}
		});

		try {
			ConsoleCallbackHandler callbackHandler = new ConsoleCallbackHandler();
			loginContext = new LoginContext("UserFileDemo", callbackHandler);
		} catch (LoginException e) {
			System.err.println("login context creation failed: " + e.getMessage());
			System.exit(1);
		}

		try {
			loginContext.login();
		} catch (LoginException e) {
			System.out.println("authentication failed: " + e.getMessage());
			System.exit(1);
		}
		System.out.println("authentication succeeded");

	}

}
