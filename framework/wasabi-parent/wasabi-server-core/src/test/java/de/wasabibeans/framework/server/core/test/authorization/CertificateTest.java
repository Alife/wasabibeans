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

package de.wasabibeans.framework.server.core.test.authorization;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class CertificateTest extends WasabiRemoteTest {

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
	public void TimeEntryTest() throws WasabiException, InterruptedException {
		System.out.println("=== TimeEntryTest() ===");

		// Create user
		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO home = userService().getHomeRoom(user).getValue();

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", home);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for testRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(
				testRoom,
				user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Setting a ACL time entry for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.EXECUTE, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 50000));
		System.out.println("done.");

		System.out.print("Setting a ACL time entry for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.COMMENT, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 50000));
		System.out.println("done.");

		displayACLEntry(testRoom, "testRoom");

		System.out.print("Check if Cert for user, testRoom and COMMENT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Using hasPermission...");
		try {
			authorizationService().hasPermission(testRoom, user, WasabiPermission.COMMENT);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Check if Cert for user, testRoom and COMMENT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Lets sleep some minutes...ZZZzzzZZZ...zzzzZZZZzzz...");
		Thread.sleep(120000);
		System.out.println("Waking up...what a wonderful day :-)");

		displayACLEntry(testRoom, "testRoom");

		System.out.print("Check if Cert for user, testRoom and COMMENT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void forbiddanceTest() throws WasabiException {
		System.out.println("=== forbiddanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating aRoom at usersHome... ");
		WasabiRoomDTO aRoom = null;
		try {
			aRoom = roomService().create("aRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for aRoom... ");
		aclService().deactivateInheritance(aRoom);
		System.out.println("done.");

		aclService().remove(aRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating someRoom at aRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(aRoom, "aRoom");

		System.out.print("Settin INSERT as forbidden user for aRoom... ");
		aclService().create(aRoom, user, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Creating someRoom1 at aRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void inheritanceTest() throws WasabiException {
		System.out.println("=== inheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating aRoom at usersHome... ");
		WasabiRoomDTO aRoom = null;
		try {
			aRoom = roomService().create("aRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT forbiddance as userRight for usersHome... ");
		aclService().create(usersHome, user, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Setting WRITE forbiddance as userRight for usersHome... ");
		aclService().create(usersHome, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for aRoom... ");
		aclService().deactivateInheritance(aRoom);
		System.out.println("done.");

		aclService().remove(aRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating someRoom at aRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom1 at aRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(someRoom1, "someRoom1");

		System.out.print("Activating inheritance for aRoom... ");
		aclService().activateInheritance(aRoom);
		System.out.println("done.");

		displayACLEntry(someRoom1, "someRoom1");

		System.out.print("Creating someRoom2 at someRoom1... ");
		WasabiRoomDTO someRoom2 = null;
		try {
			someRoom2 = roomService().create("someRoom2", someRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void removeMemberTest() throws WasabiException {
		System.out.println("=== removeMemberTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as groupRight for group wasabi... ");
		aclService().create(wasabiGroup, wasabiGroup, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as groupRight for group wasabi... ");
		aclService().create(wasabiGroup, wasabiGroup, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating aRoom at usersHome... ");
		WasabiRoomDTO aRoom = null;
		try {
			aRoom = roomService().create("aRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Adding user to testGroup...");
		try {
			groupService().addMember(testGroup, user);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing INSERT as groupRight for group wasabi... ");
		aclService().remove(wasabiGroup, wasabiGroup, WasabiPermission.INSERT);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for aRoom... ");
		aclService().deactivateInheritance(aRoom);
		System.out.println("done.");

		aclService().remove(
				aRoom,
				user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.INSERT, WasabiPermission.VIEW,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Setting INSERT as groupRight for aRoom... ");
		aclService().create(aRoom, testGroup, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting WRITE as groupRight for aRoom... ");
		aclService().create(aRoom, testGroup, WasabiPermission.WRITE, true);
		System.out.println("done.");

		displayACLEntry(aRoom, "aRoom");

		System.out.print("Creating someRoom at aRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing user from testGroup...");
		try {
			groupService().removeMember(testGroup, user);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(aRoom, "aRoom");

		System.out.print("Creating someRoom1 at aRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void removeRightTest() throws WasabiException {
		System.out.println("=== removeRightTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating aRoom at usersHome... ");
		WasabiRoomDTO aRoom = null;
		try {
			aRoom = roomService().create("aRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for aRoom... ");
		aclService().deactivateInheritance(aRoom);
		System.out.println("done.");

		aclService().remove(aRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating someRoom at aRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", aRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(aRoom, "aRoom");

		System.out.print("Removing INSERT as user for aRoom... ");
		aclService().remove(aRoom, user, WasabiPermission.INSERT);
		System.out.println("done.");

		System.out.print("Creating someRoom1 at aRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", aRoom);
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
