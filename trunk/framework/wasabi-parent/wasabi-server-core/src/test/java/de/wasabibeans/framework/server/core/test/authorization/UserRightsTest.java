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

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class UserRightsTest extends WasabiRemoteTest {

	private void displayACLEntry(WasabiObjectDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
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

	@Test
	public void userInheritedTimeRightsTest() throws WasabiException {
		System.out.println("=== userInheritedTimeRightsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating userInheritedTimeRightsRoom at usersHome... ");
		WasabiRoomDTO userInheritedTimeRightsRoom = null;
		try {
			userInheritedTimeRightsRoom = roomService().create("userInheritedTimeRightsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(userInheritedTimeRightsRoom, "userInheritedTimeRightsRoom");

		System.out.print("Deactivating inheritance for userInheritedTimeRightsRoom... ");
		aclService().deactivateInheritance(userInheritedTimeRightsRoom);
		System.out.println("done.");

		displayACLEntry(userInheritedTimeRightsRoom, "userInheritedTimeRightsRoom");

		System.out.print("Setting INSERT as userRight for userTimeInheritedRightsRoom... ");
		aclService().create(userInheritedTimeRightsRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(userInheritedTimeRightsRoom, "userInheritedTimeRightsRoom");

		System.out.print("Creating subRoom1 at userTimeInheritedRightsRoom... ");
		WasabiRoomDTO subRoom1 = null;
		try {
			subRoom1 = roomService().create("subRoom1", userInheritedTimeRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(subRoom1, "subRoom1");

		System.out.print("Creating subSubRoom1 at subRoom1... ");
		WasabiRoomDTO subSubRoom1 = null;
		try {
			subSubRoom1 = roomService().create("subSubRoom1", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Creating someRoom1 at subRoom1... ");
		try {
			roomService().create("someRoom1", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom1 at subSubRoom1... ");
		try {
			roomService().create("someRoom1", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with forbiddance as userTimeRight for userInheritedTimeRightsRoom... ");
		aclService().create(userInheritedTimeRightsRoom, user, WasabiPermission.INSERT, false,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		displayACLEntry(userInheritedTimeRightsRoom, "userInheritedTimeRightsRoom");
		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Creating someRoom2 at subRoom1... ");
		try {
			roomService().create("someRoom2", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom2 at subSubRoom1... ");
		try {
			roomService().create("someRoom2", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userTimeRight for subRoom1... ");
		aclService().create(subRoom1, user, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Creating someRoom3 at subRoom1... ");
		try {
			roomService().create("someRoom3", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom3 at subSubRoom1... ");
		try {
			roomService().create("someRoom3", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userRight for subSubRoom1... ");
		aclService().create(subSubRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Creating someRoom4 at subSubRoom1... ");
		try {
			roomService().create("someRoom4", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void userRightTest() throws WasabiException {
		System.out.println("=== userRightsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating userRightsRoom at usersHome... ");
		WasabiRoomDTO userRightsRoom = null;
		try {
			userRightsRoom = roomService().create("userRightsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(userRightsRoom, "userRightsRoom");

		System.out.print("Deactivating inheritance for userRightsRoom... ");
		aclService().deactivateInheritance(userRightsRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT with allowance as userRight at userRightsRoom... ");
		aclService().create(userRightsRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(userRightsRoom, "userRightsRoom");

		System.out.print("Creating subRoom1 at userRightsRoom... ");
		WasabiRoomDTO subRoom1 = null;
		try {
			subRoom1 = roomService().create("subRoom1", userRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(subRoom1, "subRoom1");

		System.out.print("Creating subSubRoom1 at subRoom1... ");
		WasabiRoomDTO subSubRoom1 = null;
		try {
			subSubRoom1 = roomService().create("subSubRoom1", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Setting INSERT with forbiddance as userRight at subRoom1... ");
		aclService().create(subRoom1, user, WasabiPermission.INSERT, false);
		System.out.println("done.");

		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Creating someRoom1 at subSubRoom1... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userTimeRight at userRightsRoom... ");
		aclService().create(userRightsRoom, user, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123455));
		System.out.println("done.");
		
		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");
		
		System.out.print("Creating someRoom2 at subSubRoom1... ");
		WasabiRoomDTO someRoom2 = null;
		try {
			someRoom2 = roomService().create("someRoom2", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void userTimeRightsTest() throws WasabiException {
		System.out.println("=== userTimeRightsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating userTimeRightsRoom at usersHome... ");
		WasabiRoomDTO userTimeRightsRoom = roomService().create("userTimeRightsRoom", usersHome);
		System.out.println("done.");

		displayACLEntry(userTimeRightsRoom, "userTimeRightsRoom");

		System.out.print("Deacticating inheritance for userTimeRightsRoom... ");
		aclService().deactivateInheritance(userTimeRightsRoom);
		System.out.println("done.");

		displayACLEntry(userTimeRightsRoom, "userTimeRightsRoom");

		System.out.print("Creating userTimeRightsAllowanceRoom at userTimeRightsRoom... ");
		try {
			roomService().create("userTimeRightsAllowanceRoom", userTimeRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userTimeRight for userTimeRightsRoom... ");
		aclService().create(userTimeRightsRoom, user, WasabiPermission.INSERT, true,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		displayACLEntry(userTimeRightsRoom, "userTimeRightsRoom");

		System.out.print("Creating userTimeRightsAllowanceRoom at userTimeRightsRoom... ");
		try {
			roomService().create("userTimeRightsAllowanceRoom", userTimeRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with forbiddance as userTimeRight for userTimeRightsRoom... ");
		aclService().create(userTimeRightsRoom, user, WasabiPermission.INSERT, false,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		displayACLEntry(userTimeRightsRoom, "userTimeRightsRoom");

		System.out.print("Creating userTimeRightsForbiddanceRoom at userTimeRightsRoom... ");
		try {
			roomService().create("userTimeRightsForbiddanceRoom", userTimeRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}
}
