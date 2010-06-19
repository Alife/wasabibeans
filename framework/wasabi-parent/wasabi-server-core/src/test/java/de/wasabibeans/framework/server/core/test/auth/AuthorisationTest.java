package de.wasabibeans.framework.server.core.test.auth;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.embedded.Bootstrap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.wasabibeans.framework.server.core.common.WasabiConnection;

public class AuthorisationTest {
	
	@BeforeClass
	public static void startJBoss() throws Exception {
		Bootstrap stiefelriemen = Bootstrap.getInstance();
		stiefelriemen.bootstrap();
		
	}

/*	@Before
	public void setUp() throws LoginException, NamingException {

		WasabiConnection wasabiConnection = new WasabiConnection();
		wasabiConnection.connect("localhost", "1099");
		wasabiConnection.login("root", "meerrettich");
	}
*/
	@Test
	public void test() {
		assert 1 == 1;
	}
}
