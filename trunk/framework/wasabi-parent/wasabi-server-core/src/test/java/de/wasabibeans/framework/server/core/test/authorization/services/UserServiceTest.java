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
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class UserServiceTest extends WasabiRemoteTest {

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
	public void enterTest() throws WasabiException {
		System.out.println("=== enterTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating enterTestRoom at usersHome... ");
		WasabiRoomDTO enterTestRoom = null;
		try {
			enterTestRoom = roomService().create("enterTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for enterTestRoom... ");
		aclService().create(enterTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for enterTestRoom... ");
		aclService().deactivateInheritance(enterTestRoom);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering enterTestRoom...");
		try {
			userService().enter(newUser, enterTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting EXECUTE as userRight for enterTestRoom... ");
		aclService().create(enterTestRoom, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.print("Entering enterTestRoom...");
		try {
			userService().enter(newUser, enterTestRoom);
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

		System.out.print("Setting EXECUTE as userRight for getUserByNameWithEnvironmentTestRoom... ");
		aclService().create(getUserByNameWithEnvironmentTestRoom, user, WasabiPermission.EXECUTE, true);
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
	public void getUsersByDisplayNameTest() throws WasabiException {
		System.out.println("=== getUsersByDisplayNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating createTestRoom at getUsersByDisplayNameTestRoom... ");
		WasabiRoomDTO getUsersByDisplayNameTestRoom = null;
		try {
			getUsersByDisplayNameTestRoom = roomService().create("getUsersByDisplayNameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getUsersByDisplayNameTestRoom... ");
		aclService().create(getUsersByDisplayNameTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getUsersByDisplayNameTestRoom... ");
		aclService().create(getUsersByDisplayNameTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting EXECUTE as userRight for getUsersByDisplayNameTestRoom... ");
		aclService().create(getUsersByDisplayNameTestRoom, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getUsersByDisplayNameTestRoom... ");
		aclService().deactivateInheritance(getUsersByDisplayNameTestRoom);
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

		System.out.print("Entering getUsersByDisplayNameTestRoom by user1...");
		try {
			userService().enter(user1, getUsersByDisplayNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUsersByDisplayNameTestRoom by user2...");
		try {
			userService().enter(user2, getUsersByDisplayNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering getUsersByDisplayNameTestRoom by user3...");
		try {
			userService().enter(user3, getUsersByDisplayNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting users by displayName with VIEW at room getUsersByDisplayNameTestRoom...");
		try {
			Vector<WasabiUserDTO> users = userService().getUsersByDisplayName("user2");
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(userService().getDisplayName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user2... ");
		aclService().create(user2, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting users with VIEW at room getUsersByDisplayNameTestRoom...");
		try {
			Vector<WasabiUserDTO> users = userService().getUsersByDisplayName("user2");
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(userService().getDisplayName(wasabiUserDTO).getValue());
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

		System.out.print("Setting EXECUTE as userRight for getUsersTestRoom... ");
		aclService().create(getUsersTestRoom, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

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
	public void getWhereaboutsTest() throws WasabiException {
		System.out.println("=== getWhereaboutsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating enterRoom1 at usersHome... ");
		WasabiRoomDTO enterRoom1 = null;
		try {
			enterRoom1 = roomService().create("enterRoom1", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating enterRoom2 at usersHome... ");
		WasabiRoomDTO enterRoom2 = null;
		try {
			enterRoom2 = roomService().create("enterRoom2", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating enterRoom3 at usersHome... ");
		WasabiRoomDTO enterRoom3 = null;
		try {
			enterRoom3 = roomService().create("enterRoom3", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for enterRoom1... ");
		aclService().create(enterRoom1, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting GRANT as userRight for enterRoom2... ");
		aclService().create(enterRoom2, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting GRANT as userRight for enterRoom3... ");
		aclService().create(enterRoom3, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for enterRoom1... ");
		aclService().deactivateInheritance(enterRoom1);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for enterRoom2... ");
		aclService().deactivateInheritance(enterRoom2);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for enterRoom3... ");
		aclService().deactivateInheritance(enterRoom3);
		System.out.println("done.");

		System.out.print("Setting EXECUTE as userRight for enterRoom1... ");
		aclService().create(enterRoom1, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.print("Setting EXECUTE as userRight for enterRoom2... ");
		aclService().create(enterRoom2, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.print("Setting EXECUTE as userRight for enterRoom3... ");
		aclService().create(enterRoom3, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for enterRoom1... ");
		aclService().create(enterRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for enterRoom2... ");
		aclService().create(enterRoom2, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for enterRoom3... ");
		aclService().create(enterRoom3, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering enterRoom1...");
		try {
			userService().enter(newUser, enterRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering enterRoom2...");
		try {
			userService().enter(newUser, enterRoom2);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Entering enterRoom3...");
		try {
			userService().enter(newUser, enterRoom3);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(enterRoom1, "enterRoom1");
		displayACLEntry(enterRoom2, "enterRoom2");
		displayACLEntry(enterRoom3, "enterRoom3");

		System.out.println("Using getWhereabouts...");
		try {
			Vector<WasabiRoomDTO> rooms = userService().getWhereabouts(newUser);
			for (WasabiRoomDTO wasabiRoomDTO : rooms)
				System.out.println(objectService().getName(wasabiRoomDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(enterRoom1, "enterRoom1");
		displayACLEntry(enterRoom2, "enterRoom2");
		displayACLEntry(enterRoom3, "enterRoom3");

		System.out.print("Setting VIEW as userRight for enterRoom1... ");
		aclService().create(enterRoom1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for enterRoom3... ");
		aclService().create(enterRoom3, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		displayACLEntry(enterRoom1, "enterRoom1");
		displayACLEntry(enterRoom2, "enterRoom2");
		displayACLEntry(enterRoom3, "enterRoom3");

		System.out.println("Using getWhereabouts...");
		try {
			Vector<WasabiRoomDTO> rooms = userService().getWhereabouts(newUser);
			for (WasabiRoomDTO wasabiRoomDTO : rooms)
				System.out.println(objectService().getName(wasabiRoomDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Leaving enterRoom3...");
		try {
			userService().leave(newUser, enterRoom3);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Using getWhereabouts...");
		try {
			Vector<WasabiRoomDTO> rooms = userService().getWhereabouts(newUser);
			for (WasabiRoomDTO wasabiRoomDTO : rooms)
				System.out.println(objectService().getName(wasabiRoomDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void removeTest() throws WasabiException {
		System.out.println("=== removeTest() ===");

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

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Removing newUser...");
		try {
			userService().remove(newUser, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Removing newUser...");
		try {
			userService().remove(newUser, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void renameTest() throws WasabiException {
		System.out.println("=== renameTest() ===");

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

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Renaming newUser...");
		try {
			userService().rename(newUser, "newName", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Renaming newUser...");
		try {
			userService().rename(newUser, "newName", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setDisplayNameTest() throws WasabiException {
		System.out.println("=== setDisplayNameTest() ===");

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

		System.out.print("Setting displayName of newUser...");
		try {
			userService().setDisplayName(newUser, "blubb", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Setting displayName of newUser...");
		try {
			userService().setDisplayName(newUser, "blubb", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setPasswordTest() throws WasabiException {
		System.out.println("=== setPasswordTest() ===");

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

		System.out.print("Setting password of newUser...");
		try {
			userService().setPassword(newUser, "pwd");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Check if password is string pwd:");
		String pwHash = userService().getPassword(newUser);
		if (pwHash.equals(HashGenerator.generateHash("pwd", hashAlgorithms.SHA)))
			System.out.println("true.");
		else
			System.out.println("false.");

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Setting password of newUser...");
		try {
			userService().setPassword(newUser, "pwd");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setStartRoomTest() throws WasabiException {
		System.out.println("=== setStartRoomTest() ===");

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

		System.out.print("Setting startRoom of newUser...");
		try {
			userService().setStartRoom(newUser, usersHome, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Setting startRoom of newUser...");
		try {
			userService().setStartRoom(newUser, usersHome, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setStatusTest() throws WasabiException {
		System.out.println("=== setStatusTest() ===");

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

		System.out.print("Setting status of newUser...");
		try {
			userService().setStatus(newUser, false, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE forbiddance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Setting status of newUser...");
		try {
			userService().setStatus(newUser, true, null);
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
