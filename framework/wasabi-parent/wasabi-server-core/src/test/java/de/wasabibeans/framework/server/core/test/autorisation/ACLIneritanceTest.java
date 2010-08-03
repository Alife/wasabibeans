package de.wasabibeans.framework.server.core.test.autorisation;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
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
		WasabiRoomDTO room2 = roomService().create("room2", room1);
		WasabiRoomDTO room3 = roomService().create("room3", room2);

		// set some rights for room1 and active inheritance for room2. expected result should be inherited rights from
		// room1 to room2
		aclService().create(room1, user, new int[] { WasabiPermission.VIEW, WasabiPermission.READ },
				new boolean[] { true, true });
		aclService().activateInheritance(room2);

		aclService().create(room2, user, new int[] { WasabiPermission.WRITE }, new boolean[] { true });

		// getAclEntries for room2 (recommend way)
		Vector<WasabiACLEntryDTO> ACLEntriesForRoom2 = new Vector<WasabiACLEntryDTO>();
		ACLEntriesForRoom2 = aclService().getAclEntries(room2);

		System.out.println("---- ACL entries for object " + objectService().getUUID(room2) + " ----");

		for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntriesForRoom2) {
			System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
					+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id=" + wasabiACLEntryDTO.getParentId()
					+ ",view=" + wasabiACLEntryDTO.getView() + ",read=" + wasabiACLEntryDTO.getRead() + ",insert="
					+ wasabiACLEntryDTO.getInsert() + ",execute=" + wasabiACLEntryDTO.getExecute() + ",write="
					+ wasabiACLEntryDTO.getWrite() + ",comment=" + wasabiACLEntryDTO.getComment() + ",grant="
					+ wasabiACLEntryDTO.getGrant() + ",start_time=" + wasabiACLEntryDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryDTO.getEndTime() + ",inheritance=" + wasabiACLEntryDTO.getInheritance()
					+ ",inheritance_id=" + wasabiACLEntryDTO.getInheritanceId());
		}

		// the same for room3
		aclService().activateInheritance(room3);

		// getAclEntries for room3 (recommend way)
		Vector<WasabiACLEntryDTO> ACLEntriesForRoom3 = new Vector<WasabiACLEntryDTO>();
		ACLEntriesForRoom3 = aclService().getAclEntries(room3);

		System.out.println("---- ACL entries for object " + objectService().getUUID(room3) + " ----");

		for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntriesForRoom3) {
			System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
					+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id=" + wasabiACLEntryDTO.getParentId()
					+ ",view=" + wasabiACLEntryDTO.getView() + ",read=" + wasabiACLEntryDTO.getRead() + ",insert="
					+ wasabiACLEntryDTO.getInsert() + ",execute=" + wasabiACLEntryDTO.getExecute() + ",write="
					+ wasabiACLEntryDTO.getWrite() + ",comment=" + wasabiACLEntryDTO.getComment() + ",grant="
					+ wasabiACLEntryDTO.getGrant() + ",start_time=" + wasabiACLEntryDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryDTO.getEndTime() + ",inheritance=" + wasabiACLEntryDTO.getInheritance()
					+ ",inheritance_id=" + wasabiACLEntryDTO.getInheritanceId());
		}
	}
}
