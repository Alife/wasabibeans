package de.wasabibeans.framework.server.core.test.autorisation;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ACLIneritanceTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		reWaCon.defaultLogin();
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		rootRoom = testhelper.initRepository();
		testhelper.initDatabase();
		testhelper.initTestUser();
		reWaCon.logout();
		
		reWaCon.login("user", "user");
	}
	
	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void createTest() throws WasabiException {
		// Create user
		WasabiUserDTO user = userService().create("aclInheritanceTestUser", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user);
		WasabiRoomDTO room1 = roomService().create("room1", usersHome);
		WasabiRoomDTO room2 = roomService().create("room2", usersHome);
		WasabiRoomDTO room3 = roomService().create("room3", usersHome);

		aclService().activateInheritance(room2);

		System.out.println("Inheritance Room1: " + aclService().isInheritanceAllowed(room1));
		System.out.println("Inheritance Room2: " + aclService().isInheritanceAllowed(room2));
		System.out.println("Inheritance Room3: " + aclService().isInheritanceAllowed(room3));
	}
}
