package de.wasabibeans.framework.server.core.test.performance;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class PriorityCheckComparison extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void createTest() throws WasabiException {

		// Create user
		WasabiUserDTO user = userService().create("testUser", "password");
		WasabiUserDTO loginUser = userService().getUserByName("user");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user);
		aclService().create(usersHome, loginUser, WasabiPermission.INSERT, true);

		long start = System.currentTimeMillis();
		
		for (int i = 0; i < 200; i++) {
			roomService().create("room" + i, usersHome);
			System.out.println("Raum " + i + " erstellt.");
		}
		
		long end = System.currentTimeMillis();

		
		
		//list nodes
		long start1 = System.currentTimeMillis();
		
		Vector<WasabiRoomDTO> rooms = roomService().getRooms(usersHome);
		for (WasabiRoomDTO wasabiRoomDTO : rooms) {
			System.out.println(roomService().getName(wasabiRoomDTO));
		}
		
		long end1 = System.currentTimeMillis();
		
		System.out.println("Runtime create: " + (end - start));
		System.out.println("Runtime getRooms: " + (end1 - start1));
		
		
		

	}
}
