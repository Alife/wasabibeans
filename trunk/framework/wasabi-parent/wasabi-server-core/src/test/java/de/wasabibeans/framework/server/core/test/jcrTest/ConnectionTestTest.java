package de.wasabibeans.framework.server.core.test.jcrTest;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.auth.SqlLoginModule;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.jcrTest.ConnectionTest;
import de.wasabibeans.framework.server.core.jcrTest.ConnectionTestRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class ConnectionTestTest extends Arquillian {

	// @Before
	// public void setUp() throws LoginException, NamingException {
	//
	// WasabiConnection wasabiConnection = new WasabiConnection();
	// wasabiConnection.connect("localhost", "1099");
	// wasabiConnection.login("root", "meerrettich");
	// }

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // auth
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(ConnectionTest.class.getPackage()); // ejb under test
		
		return testArchive;
	}

	@Test
	public void jcrTest() throws LoginException, NamingException {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		ConnectionTestRemote conTestBean = (ConnectionTestRemote) reWaCon.lookup("ConnectionTest/remote");
		WasabiRoomDTO root = conTestBean.login();
		assert root != null;
	}

}
