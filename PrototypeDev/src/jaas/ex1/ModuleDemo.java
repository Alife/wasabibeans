package jaas.ex1;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class ModuleDemo {

	public static void main(String[] args) throws LoginException {
		LoginContext loginContext = null;
		System.setProperty("java.security.auth.login.config", "moduledemo.conf");

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
