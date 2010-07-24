package de.wasabibeans.framework.server.core.test.autorisation;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;

@Run(RunModeType.AS_CLIENT)
public class ACLEntryDTOTest extends WasabiRemoteTest {

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
		WasabiUserDTO user1 = userService().create("aclTestUser1", "password");
		WasabiUserDTO user2 = userService().create("aclTestUser2", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO user1Home = userService().getHomeRoom(user1);
		WasabiRoomDTO user2Home = userService().getHomeRoom(user2);
		WasabiDocumentDTO document1 = documentService().create("UserTestDocu1", user1Home);
		WasabiDocumentDTO document2 = documentService().create("UserTestDocu2", user2Home);
		aclService().create(document1, user1, new int[] { WasabiPermission.VIEW, WasabiPermission.READ },
				new boolean[] { true, true });
		aclService().create(document2, user2, new int[] { WasabiPermission.VIEW, WasabiPermission.READ },
				new boolean[] { true, true });
		aclService().create(document1, user2,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.WRITE },
				new boolean[] { true, true, true });

		// getACLEntries (depreciated way)
		Vector<WasabiACLEntryDTODeprecated> ACLEntriesForDocument1 = new Vector<WasabiACLEntryDTODeprecated>();
		ACLEntriesForDocument1 = aclService().getACLEntries(document1);

		System.out.println("---- ACL entries for " + objectService().getUUID(document2) + "----");

		for (WasabiACLEntryDTODeprecated wasabiACLEntryDTODeprecated : ACLEntriesForDocument1) {
			System.out.println("[id=" + wasabiACLEntryDTODeprecated.getId() + ",identity_id="
					+ wasabiACLEntryDTODeprecated.getIdentity() + ",permission="
					+ wasabiACLEntryDTODeprecated.getPermission() + ",allowance="
					+ wasabiACLEntryDTODeprecated.isAllowance());
		}
	}
}
