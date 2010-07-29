package de.wasabibeans.framework.server.core.test.autorisation;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;

@Run(RunModeType.AS_CLIENT)
public class ACLCreateDefaultTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize jcr repository
		rootRoom = testhelper.initWorkspace("default");

		// initialize database
		testhelper.initDatabase();
	}

	@Test
	public void createTest() throws WasabiException {
		// Create user
		WasabiUserDTO user = userService().create("aclCreateDefaultTestUser", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user);
		WasabiRoomDTO room1 = roomService().create("room1", usersHome);
		WasabiRoomDTO room2 = roomService().create("room2", usersHome);
		WasabiRoomDTO room3 = roomService().create("room3", usersHome);

		aclService().createDefault(room2, WasabiType.DOCUMENT,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT }, new boolean[] { true, true });
		aclService().createDefault(room2, WasabiType.CONTAINER,
				new int[] { WasabiPermission.VIEW, WasabiPermission.EXECUTE }, new boolean[] { true, true });
	}
}
