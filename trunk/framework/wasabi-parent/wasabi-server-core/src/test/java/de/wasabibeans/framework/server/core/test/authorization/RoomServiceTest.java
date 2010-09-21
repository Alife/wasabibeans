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

		System.out.print("Removing all VIEWs for getRoomsTestRoom und subrooms... ");
		aclService().remove(room1, user, WasabiPermission.VIEW);
		aclService().remove(getRoomsTestRoom, user, WasabiPermission.VIEW);
		System.out.println("done.");

		displayACLEntry(getRoomsTestRoom, "getRoomsTestRoom");
		displayACLEntry(room1, "room1");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms4 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms4) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW as group right for room2... ");
		aclService().create(room2, g7, WasabiPermission.VIEW, true);
		System.out.println("done.");

		displayACLEntry(room2, "room2");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms5 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms5) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW as group right for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, g2, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms6 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms6) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW with fobidddance as group right for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, g4, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms7 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms7) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW with fobidddance as group time right for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, g1, WasabiPermission.VIEW, false, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123425));
		System.out.println("done.");

		displayACLEntry(room2, "room2");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms8 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms8) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW with fobidddance as group time right for room2... ");
		aclService().create(room2, g3, WasabiPermission.VIEW, false, java.lang.System.currentTimeMillis(),
				(java.lang.System.currentTimeMillis() + 123425));
		System.out.println("done.");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms9 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms9) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.print("Setting VIEW as user right for getRoomsTestRoom... ");
		aclService().create(getRoomsTestRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Display names of Rooms with View... ");
		Vector<WasabiRoomDTO> rooms10 = roomService().getRooms(getRoomsTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms10) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.println("===========================");
	}

	@Test
	public void moveTest() throws WasabiException {
		System.out.println("=== moveTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating moveTestRoom1 at usersHome... ");
		WasabiRoomDTO moveTestRoom1 = null;
		try {
			moveTestRoom1 = roomService().create("moveTestRoom1", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating moveTestRoom2 at usersHome... ");
		WasabiRoomDTO moveTestRoom2 = null;
		try {
			moveTestRoom2 = roomService().create("moveTestRoom2", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for moveTestRoom1... ");
		aclService().deactivateInheritance(moveTestRoom1);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for moveTestRoom2... ");
		aclService().deactivateInheritance(moveTestRoom2);
		System.out.println("done.");

		displayACLEntry(moveTestRoom1, "moveTestRoom1");
		displayACLEntry(moveTestRoom2, "moveTestRoom2");

		System.out.print("Setting INSERT as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Creating moveRoom at moveTestRoom... ");
		WasabiRoomDTO moveRoom = null;
		try {
			moveRoom = roomService().create("moveRoom", moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom at moveRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", moveRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting COMMENT as userRight for moveRoom... ");
		aclService().create(moveRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		displayACLEntry(moveRoom, "moveRoom");
		displayACLEntry(someRoom, "someRoom");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			roomService().move(moveRoom, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		displayACLEntry(moveTestRoom1, "moveTestRoom1");
		displayACLEntry(moveTestRoom2, "moveTestRoom2");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			roomService().move(moveRoom, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			roomService().move(moveRoom, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(moveRoom, "moveRoom");
		displayACLEntry(someRoom, "someRoom");

		System.out.println("===========================");
	}

	@Test
	public void removeTest() throws WasabiException {
		System.out.println("=== removeTest() ===");
		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating removeTestRoom at usersHome... ");
		WasabiRoomDTO removeTestRoom = null;
		try {
			removeTestRoom = roomService().create("removeTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for removeTestRoom... ");
		aclService().deactivateInheritance(removeTestRoom);
		System.out.println("done.");

		System.out.print("Setting VIEW and INSERT as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, new int[] { WasabiPermission.INSERT, WasabiPermission.VIEW },
				new boolean[] { true, true });
		System.out.println("done.");

		displayACLEntry(removeTestRoom, "removeTestRoom");

		System.out.print("Creating room1 at removeTestRoom... ");
		WasabiRoomDTO room1 = null;
		try {
			room1 = roomService().create("room1", removeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room2 at removeTestRoom... ");
		WasabiRoomDTO room2 = null;
		try {
			room2 = roomService().create("room2", removeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating subroom1 at room2... ");
		WasabiRoomDTO subroom1 = null;
		try {
			subroom1 = roomService().create("subroom1", room2);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting WRITE with forbiddance as userRight for room2... ");
		aclService().create(room2, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Setting WRITE as userRight for subroom1... ");
		aclService().create(subroom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		displayACLEntry(removeTestRoom, "removeTestRoom");

		System.out.print("Removing removeTestRoom... ");
		try {
			roomService().remove(removeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(removeTestRoom, "removeTestRoom");

		System.out.println("Display names of rooms with View at usersHome... ");
		Vector<WasabiRoomDTO> rooms0 = roomService().getRooms(usersHome);
		for (WasabiRoomDTO wasabiRoomDTO : rooms0) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.println("Display names of rooms with View at removeTestRoom... ");
		Vector<WasabiRoomDTO> rooms1 = roomService().getRooms(removeTestRoom);
		for (WasabiRoomDTO wasabiRoomDTO : rooms1) {
			System.out.println(roomService().getName(wasabiRoomDTO).getValue());
		}
		System.out.println("done.");

		System.out.println("Display names of rooms with View at room2... ");
		Vector<WasabiRoomDTO> rooms2 = roomService().getRooms(room2);
		for (WasabiRoomDTO wasabiRoomDTO : rooms2) {
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
