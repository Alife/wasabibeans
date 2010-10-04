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
public class LockingServiceTest extends WasabiRemoteTest {

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
	public void lockTest() throws WasabiException {
		System.out.println("=== lockTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating lockTestRoom at usersHome... ");
		WasabiRoomDTO lockTestRoom = null;
		try {
			lockTestRoom = roomService().create("lockTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for lockTestRoom... ");
		aclService().create(lockTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for lockTestRoom... ");
		aclService().deactivateInheritance(lockTestRoom);
		System.out.println("done.");

		System.out.print("Setting lock at lockTestRoom... ");
		try {
			lockingService().lock(lockTestRoom, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for lockTestRoom... ");
		aclService().create(lockTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting lock at lockTestRoom... ");
		try {
			lockingService().lock(lockTestRoom, true);
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

	@Test
	public void unlockTest() throws WasabiException {
		System.out.println("=== unlockTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating unlockTestRoom at usersHome... ");
		WasabiRoomDTO unlockTestRoom = null;
		try {
			unlockTestRoom = roomService().create("unlockTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for unlockTestRoom... ");
		aclService().create(unlockTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for unlockTestRoom... ");
		aclService().deactivateInheritance(unlockTestRoom);
		System.out.println("done.");

		System.out.print("Setting WRITE as userRight for unlockTestRoom... ");
		aclService().create(unlockTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting lock at unlockTestRoom... ");
		WasabiRoomDTO lockToken = null;
		try {
			lockToken = lockingService().lock(unlockTestRoom, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Unlocking unlockTestRoom... ");
		try {
			lockingService().unlock(lockToken);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting lock at unlockTestRoom... ");
		try {
			lockToken = lockingService().lock(unlockTestRoom, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing WRITE as userRight for unlockTestRoom... ");
		aclService().remove(unlockTestRoom, user, WasabiPermission.WRITE);
		System.out.println("done.");

		System.out.print("Unlocking unlockTestRoom... ");
		try {
			lockingService().unlock(lockToken);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}
}
