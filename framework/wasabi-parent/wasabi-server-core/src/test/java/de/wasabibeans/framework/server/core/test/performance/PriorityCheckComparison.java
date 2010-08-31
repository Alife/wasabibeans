package de.wasabibeans.framework.server.core.test.performance;

import java.sql.SQLException;
import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
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
	public void createTest() throws WasabiException, SQLException, ClassNotFoundException {
		WasabiGroupDTO g1 = groupService().create("g1", null);
		WasabiGroupDTO g2 = groupService().create("g2", g1);
		WasabiGroupDTO g3 = groupService().create("g3", g1);
		WasabiGroupDTO g4 = groupService().create("g4", g2);
		WasabiGroupDTO g5 = groupService().create("g5", g2);
		WasabiGroupDTO g6 = groupService().create("g6", g4);
		WasabiGroupDTO g7 = groupService().create("g7", g3);

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		groupService().addMember(g5, user);
		groupService().addMember(g6, user);
		groupService().addMember(g7, user);
		
		System.out.print("Creating performance at usersHome... ");
		WasabiRoomDTO performance = null;
		try {
			performance = roomService().create("performance", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for performance... ");
		aclService().deactivateInheritance(performance);
		System.out.println("done.");
		
		System.out.print("Setting INSERT with allowance as groupRight at groupRightsRoom... ");
		aclService().create(usersHome, g1, WasabiPermission.INSERT, true);
		System.out.println("done.");

		long start = System.currentTimeMillis();

		for (int i = 0; i < 100; i++) {
			roomService().create("room" + i, performance);
			//System.out.println("Raum " + i + " erstellt.");
		}

		long end = System.currentTimeMillis();

		// list nodes
		long start1 = System.currentTimeMillis();

		//Vector<WasabiRoomDTO> rooms = roomService().getRooms(usersHome);
		// for (WasabiRoomDTO wasabiRoomDTO : rooms) {
		// roomService().getName(wasabiRoomDTO);
		// }

		long end1 = System.currentTimeMillis();

		System.out.println("Runtime create: " + (end - start));
		System.out.println("Runtime getRooms: " + (end1 - start1));

	}
}
