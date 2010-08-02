package de.wasabibeans.framework.server.core.test.autorisation;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;

@Run(RunModeType.AS_CLIENT)
public class ACLCreateDefaultTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize jcr repository
		rootRoom = testhelper.initRepository();

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
		aclService().createDefault(room2, WasabiType.CONTAINER, new int[] { WasabiPermission.WRITE },
				new boolean[] { true });

		Vector<WasabiACLEntryTemplateDTO> ACLEntriesForRoom2 = new Vector<WasabiACLEntryTemplateDTO>();
		ACLEntriesForRoom2 = aclService().getDefaultAclEntries(room2);

		System.out.println("---- Default ACL entries for location " + objectService().getUUID(room2) + " ----");

		for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTO : ACLEntriesForRoom2) {
			System.out.println("[id=" + wasabiDefaultACLEntryDTO.getId() + ",location_id="
					+ objectService().getUUID(room2) + ",wasabi_type=" + wasabiDefaultACLEntryDTO.getWasabiType()
					+ ",view=" + wasabiDefaultACLEntryDTO.getView() + ",read=" + wasabiDefaultACLEntryDTO.getRead()
					+ ",insert=" + wasabiDefaultACLEntryDTO.getInsert() + ",execute="
					+ wasabiDefaultACLEntryDTO.getExecute() + ",write=" + wasabiDefaultACLEntryDTO.getWrite()
					+ ",comment=" + wasabiDefaultACLEntryDTO.getComment() + ",grant="
					+ wasabiDefaultACLEntryDTO.getGrant() + ",start_time=" + wasabiDefaultACLEntryDTO.getStartTime()
					+ ",end_time=" + wasabiDefaultACLEntryDTO.getEndTime());
		}
	}
}
