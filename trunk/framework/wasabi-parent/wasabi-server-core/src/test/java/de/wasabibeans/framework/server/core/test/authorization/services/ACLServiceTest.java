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
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ACLServiceTest extends WasabiRemoteTest {

	@Test
	public void activateInheritanceTest() throws WasabiException {
		System.out.println("=== activateInheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating activateInheritanceTestRoom at usersHome... ");
		WasabiRoomDTO activateInheritanceTestRoom = null;
		try {
			activateInheritanceTestRoom = roomService().create("activateInheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for activateInheritanceTestRoom... ");
		aclService().create(activateInheritanceTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for activateInheritanceTestRoom... ");
		aclService().create(activateInheritanceTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		displayACLEntry(activateInheritanceTestRoom, "activateInheritanceTestRoom");

		System.out.print("Deactivating inheritance for activateInheritanceTestRoom... ");
		aclService().deactivateInheritance(activateInheritanceTestRoom);
		System.out.println("done.");

		System.out.print("Delete GRANT as userRight for activateInheritanceTestRoom... ");
		aclService().remove(activateInheritanceTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Activating inheritance for activateInheritanceTestRoom... ");
		try {
			aclService().activateInheritance(activateInheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void createDefaultTest() throws WasabiException {
		System.out.println("=== createDefaultTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating createDefaultTestRoom at usersHome... ");
		WasabiRoomDTO createDefaultTestRoom = null;
		try {
			createDefaultTestRoom = roomService().create("createDefaultTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, READ, INSERT as userRight for createDefaultTestRoom... ");
		aclService().create(createDefaultTestRoom, user,
				new int[] { WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.GRANT },
				new boolean[] { true, true, true });
		System.out.println("done.");

		System.out.print("Deactivating inheritance for createDefaultTestRoom... ");
		aclService().deactivateInheritance(createDefaultTestRoom);
		System.out.println("done.");

		System.out.print("Setting TemplateRight for createDefaultTestRoom... ");
		aclService().createDefault(createDefaultTestRoom, WasabiType.ROOM, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at createDefaultTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", createDefaultTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(someRoom, "someRoom");

		System.out.print("Delete GRANT as userRight for createDefaultTestRoom... ");
		aclService().remove(createDefaultTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Setting TemplateRight for createDefaultTestRoom... ");
		try {
			aclService().createDefault(createDefaultTestRoom, WasabiType.ROOM, WasabiPermission.EXECUTE, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

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

		displayACLEntry(createTestRoom, "createTestRoom");

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(createTestRoom);
		System.out.println("done.");

		System.out.print("Delete GRANT as userRight for createTestRoom... ");
		aclService().remove(createTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for createTestRoom... ");
		try {
			aclService().create(createTestRoom, user, WasabiPermission.READ, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void deactivateInheritanceTest() throws WasabiException {
		System.out.println("=== deactivateInheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating deactivateInheritanceTestRoom at usersHome... ");
		WasabiRoomDTO deactivateInheritanceTestRoom = null;
		try {
			deactivateInheritanceTestRoom = roomService().create("deactivateInheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, READ, INSERT as userRight for deactivateInheritanceTestRoom... ");
		aclService().create(deactivateInheritanceTestRoom, user,
				new int[] { WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.GRANT },
				new boolean[] { true, true, true });
		System.out.println("done.");

		System.out.print("Creating someRoom at deactivateInheritanceTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", deactivateInheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someSubRoom at someRoom... ");
		WasabiRoomDTO someSubRoom = null;
		try {
			someSubRoom = roomService().create("someSubRoom", someRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for someRoom... ");
		aclService().create(someRoom, user, new int[] { WasabiPermission.WRITE }, new boolean[] { true });
		System.out.println("done.");

		System.out.print("Setting READ as userRight for someRoom... ");
		aclService().create(someRoom, user, new int[] { WasabiPermission.READ }, new boolean[] { true });
		System.out.println("done.");

		System.out.print("Setting READ as userRight for someSubRoom... ");
		aclService().create(someSubRoom, user, new int[] { WasabiPermission.READ }, new boolean[] { true });
		System.out.println("done.");

		displayACLEntry(someRoom, "someRoom");
		displayACLEntry(someSubRoom, "someSubRoom");

		System.out.print("Deactivating inheritance for deactivateInheritanceTestRoom... ");
		aclService().deactivateInheritance(deactivateInheritanceTestRoom);
		System.out.println("done.");

		displayACLEntry(deactivateInheritanceTestRoom, "deactivateInheritanceTestRoom");
		displayACLEntry(someRoom, "someRoom");
		displayACLEntry(someSubRoom, "someSubRoom");

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
