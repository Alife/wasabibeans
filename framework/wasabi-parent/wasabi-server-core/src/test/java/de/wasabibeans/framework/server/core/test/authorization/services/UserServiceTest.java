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
public class UserServiceTest extends WasabiRemoteTest {

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
	public void getUserByNameWithEnvironmentTest() throws WasabiException {
		System.out.println("=== getUserByNameWithEnvironmentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating createTestRoom at getUserByNameWithEnvironmentTestRoom... ");
		WasabiRoomDTO getUserByNameWithEnvironmentTestRoom = null;
		try {
			getUserByNameWithEnvironmentTestRoom = roomService().create("getUserByNameWithEnvironmentTestRoom",
					usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getUserByNameWithEnvironmentTestRoom... ");
		aclService().create(getUserByNameWithEnvironmentTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getUserByNameWithEnvironmentTestRoom... ");
		aclService().create(getUserByNameWithEnvironmentTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getUserByNameWithEnvironmentTestRoom... ");
		aclService().deactivateInheritance(getUserByNameWithEnvironmentTestRoom);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUserByNameWithEnvironmentTestRoom by newUser...");
		try {
			userService().enter(newUser, getUserByNameWithEnvironmentTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting user newUser by name at room getUserByNameWithEnvironmentTestRoom...");
		try {
			userService().getUserByName(getUserByNameWithEnvironmentTestRoom, "newUser");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting user newUser by name at room getUserByNameWithEnvironmentTestRoom...");
		try {
			userService().getUserByName(getUserByNameWithEnvironmentTestRoom, "newUser");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getUsersTest() throws WasabiException {
		System.out.println("=== getUsersTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating createTestRoom at getUsersTestRoom... ");
		WasabiRoomDTO getUsersTestRoom = null;
		try {
			getUsersTestRoom = roomService().create("getUsersTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getUsersTestRoom... ");
		aclService().create(getUsersTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getUsersTestRoom... ");
		aclService().create(getUsersTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getUsersTestRoom... ");
		aclService().deactivateInheritance(getUsersTestRoom);
		System.out.println("done.");

		System.out.println("Creating user1...");
		WasabiUserDTO user1 = null;
		try {
			user1 = userService().create("user1", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Creating user2...");
		WasabiUserDTO user2 = null;
		try {
			user2 = userService().create("user2", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Creating user3...");
		WasabiUserDTO user3 = null;
		try {
			user3 = userService().create("user3", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUsersTestRoom by user1...");
		try {
			userService().enter(user1, getUsersTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUsersTestRoom by user2...");
		try {
			userService().enter(user2, getUsersTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUsersTestRoom by user3...");
		try {
			userService().enter(user3, getUsersTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting users with VIEW at room getUsersTestRoom...");
		try {
			Vector<WasabiUserDTO> users = userService().getUsers(getUsersTestRoom);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(userService().getDisplayName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user2... ");
		aclService().create(user2, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting users with VIEW at room getUsersTestRoom...");
		try {
			Vector<WasabiUserDTO> users = userService().getUsers(getUsersTestRoom);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(userService().getDisplayName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getStatusTest() throws WasabiException {
		System.out.println("=== getStatusTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting status of newUser...");
		try {
			userService().getStatus(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.READ, false);
		System.out.println("done.");

		System.out.print("Getting status of newUser...");
		try {
			userService().getStatus(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getUserByNameTest() throws WasabiException {
		System.out.println("=== getUserByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting newUser by name...");
		try {
			userService().getUserByName("newUser");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting newUser by name...");
		try {
			userService().getUserByName("newUser");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getStartRoomTest() throws WasabiException {
		System.out.println("=== getStartRoomTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting startRoom of user newUser...");
		try {
			userService().getStartRoom(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.READ, false);
		System.out.println("done.");

		System.out.print("Getting startRoom of newUser...");
		try {
			userService().getStartRoom(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getHomeRoomTest() throws WasabiException {
		System.out.println("=== getHomeRoomTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting homeRoom of user newUser...");
		try {
			userService().getHomeRoom(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting homeRoom of user newUser...");
		try {
			userService().getHomeRoom(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDisplayNameTest() throws WasabiException {
		System.out.println("=== getDisplayNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting display name of user newUser...");
		try {
			userService().getDisplayName(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting display name of user newUser...");
		try {
			userService().getDisplayName(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getMembershipsTest() throws WasabiException {
		System.out.println("=== getMembershipsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting get memberships of newUser...");
		try {
			userService().getMemberships(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting get memberships of newUser...");
		try {
			userService().getMemberships(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getPasswordTest() throws WasabiException {
		System.out.println("=== getPasswordTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting password of newUser...");
		try {
			userService().getPassword(newUser);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Getting password of newUser...");
		try {
			userService().getPassword(newUser);
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
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.println("Creating user newUser...");
		try {
			userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		try {
			userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getAllUsersTest() throws WasabiException {
		System.out.println("=== getAllUsersTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating user user1...");
		WasabiUserDTO user1 = null;
		try {
			user1 = userService().create("user1", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating user user2...");
		WasabiUserDTO user2 = null;
		try {
			user2 = userService().create("user2", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating user user3...");
		WasabiUserDTO user3 = null;
		try {
			user3 = userService().create("user3", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting all users...");
		try {
			Vector<WasabiUserDTO> users = userService().getAllUsers();
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing VIEW as userRight for user1... ");
		aclService().remove(user1, user, WasabiPermission.VIEW);
		System.out.println("done.");

		System.out.print("Removing VIEW as userRight for user3... ");
		aclService().remove(user3, user, WasabiPermission.VIEW);
		System.out.println("done.");

		System.out.println("Getting all users...");
		try {
			Vector<WasabiUserDTO> users = userService().getAllUsers();
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
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
