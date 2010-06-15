package de.wasabibeans.framework.server.core.test.auth;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;

import de.wasabibeans.framework.server.core.common.WasabiConnection;

public class AuthorisationTest {

	@Before
	public void setUp() throws LoginException, NamingException {

		WasabiConnection wasabiConnection = new WasabiConnection();
		wasabiConnection.connect("localhost", "1099");
		wasabiConnection.login("root", "meerrettich");
	}
	
	@Test
	public void test() {
		System.out.println("HelloWorld");
	}
}
