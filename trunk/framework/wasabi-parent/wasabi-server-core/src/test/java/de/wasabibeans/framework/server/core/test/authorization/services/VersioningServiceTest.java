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
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class VersioningServiceTest extends WasabiRemoteTest {

	@Test
	public void createVersionTest() throws WasabiException {
		System.out.println("=== createVersionTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating createVersionTestRoom at usersHome... ");
		WasabiRoomDTO createVersionTestRoom = null;
		try {
			createVersionTestRoom = roomService().create("createVersionTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for createVersionTestRoom... ");
		aclService().create(createVersionTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for createVersionTestRoom... ");
		aclService().deactivateInheritance(createVersionTestRoom);
		System.out.println("done.");

		aclService().remove(
				createVersionTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Creating version at createVersionTestRoom... ");
		try {
			versioningService().createVersion(createVersionTestRoom, "wtf");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for createVersionTestRoom... ");
		aclService().create(createVersionTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Creating version at createVersionTestRoom... ");
		try {
			versioningService().createVersion(createVersionTestRoom, "wtf");
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
	public void getVersionsTest() throws WasabiException {
		System.out.println("=== getVersionsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getVersionsTestRoom at usersHome... ");
		WasabiRoomDTO getVersionsTestRoom = null;
		try {
			getVersionsTestRoom = roomService().create("getVersionsTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getVersionsTestRoom... ");
		aclService().create(getVersionsTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getVersionsTestRoom... ");
		aclService().deactivateInheritance(getVersionsTestRoom);
		System.out.println("done.");

		aclService().remove(
				getVersionsTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Getting versions of getVersionsTestRoom... ");
		try {
			versioningService().getVersions(getVersionsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getVersionsTestRoom... ");
		aclService().create(getVersionsTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting versions of getVersionsTestRoom... ");
		try {
			versioningService().getVersions(getVersionsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void restoreVersionTest() throws WasabiException {
		System.out.println("=== restoreVersionTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating restoreVersionTestRoom at usersHome... ");
		WasabiRoomDTO restoreVersionTestRoom = null;
		try {
			restoreVersionTestRoom = roomService().create("restoreVersionTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for restoreVersionTestRoom... ");
		aclService().create(restoreVersionTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for restoreVersionTestRoom... ");
		aclService().deactivateInheritance(restoreVersionTestRoom);
		System.out.println("done.");

		aclService().remove(
				restoreVersionTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Restoring version at restoreVersionTestRoom... ");
		try {
			versioningService().restoreVersion(restoreVersionTestRoom, "1.1.1.1.1");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for restoreVersionTestRoom... ");
		aclService().create(restoreVersionTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Restoring version at restoreVersionTestRoom... ");
		try {
			versioningService().restoreVersion(restoreVersionTestRoom, "1.1.1.1.1");
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
