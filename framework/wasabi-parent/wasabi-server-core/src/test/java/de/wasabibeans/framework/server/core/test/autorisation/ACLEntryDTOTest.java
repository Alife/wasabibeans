package de.wasabibeans.framework.server.core.test.autorisation;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
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

	@SuppressWarnings("deprecation")
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
		Vector<WasabiACLEntryDTODeprecated> ACLEntriesForDocument1Deprecated = new Vector<WasabiACLEntryDTODeprecated>();
		ACLEntriesForDocument1Deprecated = aclService().getACLEntries(document1);

		System.out.println("---- ACL entries for object " + objectService().getUUID(document1)
				+ " (depreciated way)----");

		for (WasabiACLEntryDTODeprecated wasabiACLEntryDTODeprecated : ACLEntriesForDocument1Deprecated) {
			System.out.println("[id=" + wasabiACLEntryDTODeprecated.getId() + ",identity_id="
					+ wasabiACLEntryDTODeprecated.getIdentity() + ",permission="
					+ wasabiACLEntryDTODeprecated.getPermission() + ",allowance="
					+ wasabiACLEntryDTODeprecated.isAllowance());
		}

		// getAclEntries (recommend way)
		Vector<WasabiACLEntryDTO> ACLEntriesForDocument1 = new Vector<WasabiACLEntryDTO>();
		ACLEntriesForDocument1 = aclService().getAclEntries(document1);

		System.out.println("---- ACL entries for object " + objectService().getUUID(document1) + " (future way)----");

		for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntriesForDocument1) {
			System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserID()
					+ ",group_id=" + wasabiACLEntryDTO.getGrant() + ",parent_id=" + wasabiACLEntryDTO.getParentID()
					+ ",view=" + wasabiACLEntryDTO.getView() + ",read=" + wasabiACLEntryDTO.getRead() + ",insert="
					+ wasabiACLEntryDTO.getInsert() + ",execute=" + wasabiACLEntryDTO.getExecute() + ",write="
					+ wasabiACLEntryDTO.getWrite() + ",comment=" + wasabiACLEntryDTO.getComment() + ",grant="
					+ wasabiACLEntryDTO.getGrant() + ",start_time=" + wasabiACLEntryDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryDTO.getEndTime());
		}

		// getACLEntriesByIdentity (depreciated way)
		Vector<WasabiACLEntryDTODeprecated> ACLEntriesForDocument1AndUser2Deprecated = new Vector<WasabiACLEntryDTODeprecated>();
		ACLEntriesForDocument1AndUser2Deprecated = aclService().getACLEntriesByIdentity(document1, user2);

		System.out.println("---- ACL entries by identity " + userService().getUUID(user2) + " for object"
				+ objectService().getUUID(document1) + " (depreciated way)----");

		for (WasabiACLEntryDTODeprecated wasabiACLEntryByIdentityDTODeprecated : ACLEntriesForDocument1AndUser2Deprecated) {
			System.out.println("[id=" + wasabiACLEntryByIdentityDTODeprecated.getId() + ",identity_id="
					+ wasabiACLEntryByIdentityDTODeprecated.getIdentity() + ",permission="
					+ wasabiACLEntryByIdentityDTODeprecated.getPermission() + ",allowance="
					+ wasabiACLEntryByIdentityDTODeprecated.isAllowance());
		}

		// getAclEntriesByIdentity (recommend way)
		Vector<WasabiACLEntryDTO> ACLEntriesByIdentityForDocument1AndUser2 = new Vector<WasabiACLEntryDTO>();
		ACLEntriesByIdentityForDocument1AndUser2 = aclService().getAclEntriesByIdentity(document1, user2);

		System.out.println("---- ACL entries by identity " + userService().getUUID(user2) + " for object"
				+ objectService().getUUID(document1) + " (future way)----");

		for (WasabiACLEntryDTO wasabiACLEntryByIdentityDTO : ACLEntriesByIdentityForDocument1AndUser2) {
			System.out.println("[id=" + wasabiACLEntryByIdentityDTO.getId() + ",user_id="
					+ wasabiACLEntryByIdentityDTO.getUserID() + ",group_id=" + wasabiACLEntryByIdentityDTO.getGrant()
					+ ",parent_id=" + wasabiACLEntryByIdentityDTO.getParentID() + ",view="
					+ wasabiACLEntryByIdentityDTO.getView() + ",read=" + wasabiACLEntryByIdentityDTO.getRead()
					+ ",insert=" + wasabiACLEntryByIdentityDTO.getInsert() + ",execute="
					+ wasabiACLEntryByIdentityDTO.getExecute() + ",write=" + wasabiACLEntryByIdentityDTO.getWrite()
					+ ",comment=" + wasabiACLEntryByIdentityDTO.getComment() + ",grant="
					+ wasabiACLEntryByIdentityDTO.getGrant() + ",start_time="
					+ wasabiACLEntryByIdentityDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryByIdentityDTO.getEndTime());
		}

	}
}