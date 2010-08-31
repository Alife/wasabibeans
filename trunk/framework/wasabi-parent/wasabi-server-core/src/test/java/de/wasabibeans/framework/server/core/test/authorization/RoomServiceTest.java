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
public class RoomServiceTest extends WasabiRemoteTest {

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

	@Test
	public void getRoomsTest() throws WasabiException {
		System.out.println("=== getRoomsTest() ===");
		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getRoomsTestRoom at usersHome... ");
		WasabiRoomDTO getRoomsTestRoom = null;
		try {
			getRoomsTestRoom = roomService().create("getRoomsTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(getRoomsTestRoom, "getRoomsTestRoom");

		System.out.print("Deactivating inheritance for getRoomsTestRoom... ");
		aclService().deactivateInheritance(getRoomsTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(getRoomsTestRoom, "getRoomsTestRoom");

		System.out.print("Creating room1 at getRoomsTestRoom... ");
		WasabiRoomDTO room1 = null;
		try {
			room1 = roomService().create("room1", getRoomsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(room1, "room1");

		System.out.print("Creating room2 at getRoomsTestRoom... ");
		WasabiRoomDTO room2 = null;
		try {
			room2 = roomService().create("room2", getRoomsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room3 at getRoomsTestRoom... ");
		WasabiRoomDTO room3 = null;
		try {
			room3 = roomService().create("room3", getRoomsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(room3, "room3");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms1 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms1) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for room1... ");
		aclService().create(room1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms2 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms2) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		displayACLEntry(room3, "room3");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms3 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms3) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

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
