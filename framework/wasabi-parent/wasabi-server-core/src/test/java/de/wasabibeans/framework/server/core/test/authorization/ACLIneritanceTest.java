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
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ACLIneritanceTest extends WasabiRemoteTest {

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
	public void inheritanceTest2() throws WasabiException {
		System.out.println("=== inheritanceTest2() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating inheritanceTestRoom at usersHome... ");
		WasabiRoomDTO inheritanceTestRoom = null;
		try {
			inheritanceTestRoom = roomService().create("inheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");

		System.out.print("Creating subRoom at inheritanceTestRoom... ");
		WasabiRoomDTO subRoom = null;
		try {
			subRoom = roomService().create("subRoom", inheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(subRoom, "subRoom");

		System.out.print("Setting COMMENT as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");
		displayACLEntry(subRoom, "subRoom");

		System.out.print("Setting EXECUTE and WRITE as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, new int[] { WasabiPermission.EXECUTE, WasabiPermission.WRITE },
				new boolean[] { true, true });
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");
		displayACLEntry(subRoom, "subRoom");

		System.out.print("Removing COMMENT as userRight for inheritanceTestRoom... ");
		aclService().remove(inheritanceTestRoom, user, WasabiPermission.COMMENT);
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");
		displayACLEntry(subRoom, "subRoom");

		System.out.print("Removing EXECUTE and WRITE as userRight for inheritanceTestRoom... ");
		aclService().remove(inheritanceTestRoom, user, new int[] { WasabiPermission.EXECUTE, WasabiPermission.WRITE });
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");
		displayACLEntry(subRoom, "subRoom");

		System.out.println("===========================");
	}

	@Test
	public void inheritanceTest() throws WasabiException {
		System.out.println("=== inheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating inheritanceTestRoom at usersHome... ");
		WasabiRoomDTO inheritanceTestRoom = null;
		try {
			inheritanceTestRoom = roomService().create("inheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for inheritanceTestRoom... ");
		aclService().deactivateInheritance(inheritanceTestRoom);
		System.out.println("done.");

		aclService().remove(
				inheritanceTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.INSERT, WasabiPermission.WRITE });

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");

		System.out.print("Setting INSERT as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");

		System.out.print("Creating subRoom1 at inheritanceTestRoom... ");
		WasabiRoomDTO subRoom1 = null;
		try {
			subRoom1 = roomService().create("subRoom1", inheritanceTestRoom);
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

		System.out.print("Setting INSERT as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.print("Activating inheritance for inheritanceTestRoom... ");
		aclService().activateInheritance(inheritanceTestRoom);
		System.out.println("done.");

		displayACLEntry(inheritanceTestRoom, "inheritanceTestRoom");
		displayACLEntry(subRoom1, "subRoom1");
		displayACLEntry(subSubRoom1, "subSubRoom1");

		System.out.println("===========================");
	}

	@Test
	public void inheritanceTest3() throws WasabiException {
		System.out.println("=== inheritanceTest3() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating inheritanceTestRoom at usersHome... ");
		WasabiRoomDTO inheritanceTestRoom = null;
		try {
			inheritanceTestRoom = roomService().create("inheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for inheritanceTestRoom... ");
		aclService().deactivateInheritance(inheritanceTestRoom);
		System.out.println("done.");

		aclService().remove(
				inheritanceTestRoom,
				user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating room1 at inheritanceTestRoom... ");
		WasabiRoomDTO room1 = null;
		try {
			room1 = roomService().create("room1", inheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room2 at room1... ");
		WasabiRoomDTO room2 = null;
		try {
			room2 = roomService().create("room2", room1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting COMMENT forbiddance as userRight for inheritanceTestRoom... ");
		aclService().create(inheritanceTestRoom, user, WasabiPermission.COMMENT, false);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for room1... ");
		aclService().create(room1, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");
		
		displayACLEntry(room2, "room2");

		System.out.print("Using hasPermission: ");
		try {
			System.out.println(authorizationService().hasPermission(room2, user, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	private void displayACLEntry(WasabiObjectDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Vector<WasabiACLEntryDTO> ACLEntries = new Vector<WasabiACLEntryDTO>();
		ACLEntries = aclService().getAclEntries(room);

		System.out.println("---- ACL entries for object (" + name + ") " + objectService().getUUID(room) + " ----");

		for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntries) {
			System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
					+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id=" + wasabiACLEntryDTO.getParentId()
					+ ",view=" + wasabiACLEntryDTO.getView() + ",read=" + wasabiACLEntryDTO.getRead() + ",insert="
					+ wasabiACLEntryDTO.getInsert() + ",execute=" + wasabiACLEntryDTO.getExecute() + ",write="
					+ wasabiACLEntryDTO.getWrite() + ",comment=" + wasabiACLEntryDTO.getComment() + ",grant="
					+ wasabiACLEntryDTO.getGrant() + ",start_time=" + wasabiACLEntryDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryDTO.getEndTime() + ",inheritance=" + wasabiACLEntryDTO.getInheritance()
					+ ",inheritance_id=" + wasabiACLEntryDTO.getInheritanceId());
		}
	}
}
