/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 *  Further information are online available at: http://www.wasabibeans.de
 */

package de.wasabibeans.framework.server.core.test.authorization.services;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class DocumentServiceTest extends WasabiRemoteTest {

	@Test
	public void createTest() throws WasabiException {
		System.out.println("=== createTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating createTestRoom at usersHome... ");
		WasabiRoomDTO createTestRoom = null;
		try {
			createTestRoom = roomService().create("createTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(createTestRoom);
		System.out.println("done.");

		System.out.print("Creating document at createTestRoom... ");
		try {
			documentService().create("testDoc", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating document at createTestRoom... ");
		try {
			documentService().create("testDoc", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	private void displayACLEntry(WasabiObjectDTO room, String name) throws WasabiException {
		Vector<WasabiACLEntryDTO> ACLEntries = new Vector<WasabiACLEntryDTO>();
		if (room != null) {
			ACLEntries = aclService().getAclEntries(room);

			System.out.println("---- ACL entries for object (" + name + ") " + objectService().getUUID(room) + " ----");

			for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntries) {
				System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
						+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id="
						+ wasabiACLEntryDTO.getParentId() + ",view=" + wasabiACLEntryDTO.getView() + ",read="
						+ wasabiACLEntryDTO.getRead() + ",insert=" + wasabiACLEntryDTO.getInsert() + ",execute="
						+ wasabiACLEntryDTO.getExecute() + ",write=" + wasabiACLEntryDTO.getWrite() + ",comment="
						+ wasabiACLEntryDTO.getComment() + ",grant=" + wasabiACLEntryDTO.getGrant() + ",start_time="
						+ wasabiACLEntryDTO.getStartTime() + ",end_time=" + wasabiACLEntryDTO.getEndTime()
						+ ",inheritance=" + wasabiACLEntryDTO.getInheritance() + ",inheritance_id="
						+ wasabiACLEntryDTO.getInheritanceId());
			}
		}
	}

	@Test
	public void getContentTest() throws WasabiException {
		System.out.println("=== getContentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getContentRoom at usersHome... ");
		WasabiRoomDTO getContentRoom = null;
		try {
			getContentRoom = roomService().create("getContentRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getContentRoom... ");
		aclService().deactivateInheritance(getContentRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at getContentRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", getContentRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to get content from testDoc... ");
		try {
			documentService().getContent(testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Try to get content from testDoc... ");
		try {
			documentService().getContent(testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDocumentByNameTest() throws WasabiException {
		System.out.println("=== getDocumentByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDocumentByNameRoom at usersHome... ");
		WasabiRoomDTO getDocumentByNameRoom = null;
		try {
			getDocumentByNameRoom = roomService().create("getDocumentByNameRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDocumentByNameRoom... ");
		aclService().deactivateInheritance(getDocumentByNameRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at getDocumentByNameRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", getDocumentByNameRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to get document testDoc1 at getDocumentByNameRoom... ");
		try {
			documentService().getDocumentByName(getDocumentByNameRoom, "testDoc");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Try to get document testDoc1 at getDocumentByNameRoom... ");
		try {
			documentService().getDocumentByName(getDocumentByNameRoom, "testDoc");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setContentTest() throws WasabiException {
		System.out.println("=== setContentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating setContentRoom at usersHome... ");
		WasabiRoomDTO setContentRoom = null;
		try {
			setContentRoom = roomService().create("setContentRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for setContentRoom... ");
		aclService().deactivateInheritance(setContentRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at setContentRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", setContentRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to set content from testDoc... ");
		try {
			documentService().setContent(testDoc, "Bratwurstbratgeraet", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Try to set content from testDoc... ");
		try {
			documentService().setContent(testDoc, "Bratwurstbratgeraet", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

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
}
