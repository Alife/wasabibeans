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
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class RightsTest extends WasabiRemoteTest {

	private void displayACLEntry(WasabiObjectDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
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
	public void RightTest() throws WasabiException {
		System.out.println("=== RightTest() ===");

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

		System.out.print("Creating rightsRoom at usersHome... ");
		WasabiRoomDTO rightsRoom = null;
		try {
			rightsRoom = roomService().create("rightsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for rightsRoom... ");
		aclService().deactivateInheritance(rightsRoom);
		System.out.println("done.");

		System.out.print("Creating inheritedRightsRooms at rightsRoom... ");
		WasabiRoomDTO inheritedRightsRoom = null;
		try {
			inheritedRightsRoom = roomService().create("inheritedRightsRoom", rightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// inherited group right section
		System.out.print("Setting INSERT with allowance as groupRight at rightsRoom... ");
		aclService().create(rightsRoom, g2, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating inheritedRightsRoom at rightsRoom... ");
		try {
			inheritedRightsRoom = roomService().create("inheritedRightsRoom", rightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom1 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// explicit group right section
		System.out.print("Setting INSERT with forbiddance as groupRight at rightsRoom... ");
		aclService().create(rightsRoom, g2, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Creating someRoom2 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom2 = null;
		try {
			someRoom2 = roomService().create("someRoom2", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as groupRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, g3, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom2 at inheritedRightsRoom... ");
		try {
			someRoom2 = roomService().create("someRoom2", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// inherited user right section
		System.out.print("Setting INSERT with forbidance as groupRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, g3, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Creating someRoom3 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom3 = null;
		try {
			someRoom3 = roomService().create("someRoom3", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userRight at rightsRoom... ");
		aclService().create(rightsRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom3 at inheritedRightsRoom... ");
		try {
			someRoom3 = roomService().create("someRoom3", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// explicit user right section
		System.out.print("Setting INSERT with forbidance as userRight at rightsRoom... ");
		aclService().create(rightsRoom, user, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Creating someRoom4 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom4 = null;
		try {
			someRoom4 = roomService().create("someRoom4", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom4 at inheritedRightsRoom... ");
		try {
			someRoom4 = roomService().create("someRoom4", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// inherited group time right section
		System.out.print("Setting INSERT with forbidance as userRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, user, WasabiPermission.INSERT, false);
		System.out.println("done.");

		System.out.print("Creating someRoom5 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom5 = null;
		try {
			someRoom5 = roomService().create("someRoom5", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as groupTimeRight at rightsRoom... ");
		aclService().create(rightsRoom, g4, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom5 at inheritedRightsRoom... ");
		try {
			someRoom5 = roomService().create("someRoom5", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// explicit group time right section
		System.out.print("Setting INSERT with forbiddance as groupTimeRight at rightsRoom... ");
		aclService().create(rightsRoom, g4, WasabiPermission.INSERT, false, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom6 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom6 = null;
		try {
			someRoom6 = roomService().create("someRoom6", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as groupTimeRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, g4, WasabiPermission.INSERT, true,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom6 at inheritedRightsRoom... ");
		try {
			someRoom6 = roomService().create("someRoom6", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// inherited user time rights section
		System.out.print("Setting INSERT with forbiddance as groupTimeRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, g4, WasabiPermission.INSERT, false,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom7 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom7 = null;
		try {
			someRoom7 = roomService().create("someRoom7", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userTimeRight at rightsRoom... ");
		aclService().create(rightsRoom, user, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom7 at inheritedRightsRoom... ");
		try {
			someRoom7 = roomService().create("someRoom7", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

		// explicit user time rights section
		System.out.print("Setting INSERT with forbiddance as userTimeRight at rightsRoom... ");
		aclService().create(rightsRoom, user, WasabiPermission.INSERT, false, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom8 at inheritedRightsRoom... ");
		WasabiRoomDTO someRoom8 = null;
		try {
			someRoom8 = roomService().create("someRoom8", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT with allowance as userTimeRight at inheritedRightsRoom... ");
		aclService().create(inheritedRightsRoom, user, WasabiPermission.INSERT, true,
				java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");

		System.out.print("Creating someRoom8 at inheritedRightsRoom... ");
		try {
			someRoom8 = roomService().create("someRoom8", inheritedRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritedRightsRoom, "inheritedRightsRoom");

	}
}
