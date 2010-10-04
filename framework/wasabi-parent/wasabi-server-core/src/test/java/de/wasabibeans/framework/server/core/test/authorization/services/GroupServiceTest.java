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
public class GroupServiceTest extends WasabiRemoteTest {

	@Test
	public void addMemberTest() throws WasabiException {
		System.out.println("=== addMemberTest() ===");

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

		displayACLEntry(testGroup, "testGroup");

		System.out.print("Adding user to testGroup...");
		try {
			groupService().addMember(testGroup, user);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT fobidacne as groupRight for testGroup... ");
		aclService().create(testGroup, wasabiGroup, WasabiPermission.INSERT, false);
		System.out.println("done.");

		displayACLEntry(testGroup, "testGroup");

		System.out.print("Adding user to testGroup...");
		try {
			groupService().addMember(testGroup, user);
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

		System.out.print("Creating group testGroup...");
		try {
			groupService().create("testGroup", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		try {
			groupService().create("testGroup", wasabiGroup);
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
	public void getAllGroupsTest() throws WasabiException {
		System.out.println("=== getAllGroupsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup1...");
		WasabiGroupDTO testGroup1 = null;
		try {
			testGroup1 = groupService().create("testGroup1", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating group testGroup2...");
		WasabiGroupDTO testGroup2 = null;
		try {
			testGroup2 = groupService().create("testGroup2", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting all Groups...");
		try {
			Vector<WasabiGroupDTO> allGroups = groupService().getAllGroups();
			for (WasabiGroupDTO wasabiGroupDTO : allGroups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup1... ");
		aclService().create(testGroup1, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting all Groups...");
		try {
			Vector<WasabiGroupDTO> allGroups = groupService().getAllGroups();
			for (WasabiGroupDTO wasabiGroupDTO : allGroups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getAllMembersTest() throws WasabiException {
		System.out.println("=== getAllMembersTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
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

		System.out.println("Getting all members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getAllMembers(testGroup);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting all members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getAllMembers(testGroup);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.READ, false);
		System.out.println("done.");

		System.out.println("Getting all members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getAllMembers(testGroup);
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

		System.out.print("Creating group testGroup1...");
		WasabiGroupDTO testGroup1 = null;
		try {
			testGroup1 = groupService().create("testGroup1", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting displayname of testGroup1...");
		try {
			groupService().getDisplayName(testGroup1);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup1... ");
		aclService().create(testGroup1, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting displayname of testGroup1...");
		try {
			groupService().getDisplayName(testGroup1);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getGroupByNameTest() throws WasabiException {
		System.out.println("=== getGroupByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup1...");
		WasabiGroupDTO testGroup1 = null;
		try {
			testGroup1 = groupService().create("testGroup1", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting testGroup1 by name...");
		try {
			groupService().getGroupByName("testGroup1");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup1... ");
		aclService().create(testGroup1, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting testGroup1 by name...");
		try {
			groupService().getGroupByName("testGroup1");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getGroupsByDisplayNameTest() throws WasabiException {
		System.out.println("=== getGroupsByDisplayNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup1...");
		WasabiGroupDTO testGroup1 = null;
		try {
			testGroup1 = groupService().create("testGroup1", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting testGroup1 by displayname...");
		try {
			Vector<WasabiGroupDTO> groups = groupService().getGroupsByDisplayName("testGroup1");
			for (WasabiGroupDTO wasabiGroupDTO : groups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup... ");
		aclService().create(testGroup1, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting testGroup1 by displayname...");
		try {
			Vector<WasabiGroupDTO> groups = groupService().getGroupsByDisplayName("testGroup1");
			for (WasabiGroupDTO wasabiGroupDTO : groups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getMemberByNameTest() throws WasabiException {
		System.out.println("=== getMemberByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
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

		System.out.print("Getting member user by name...");
		try {
			groupService().getMemberByName(testGroup, "user");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting member user by name...");
		try {
			groupService().getMemberByName(testGroup, "user");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.READ, false);
		System.out.println("done.");

		System.out.print("Getting member user by name...");
		try {
			groupService().getMemberByName(testGroup, "user");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getMembersTest() throws WasabiException {
		System.out.println("=== getMembersTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
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

		System.out.println("Getting  members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getMembers(testGroup);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting  members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getMembers(testGroup);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.READ, false);
		System.out.println("done.");

		System.out.println("Getting  members of testGroup...");
		try {
			Vector<WasabiUserDTO> users = groupService().getMembers(testGroup);
			for (WasabiUserDTO wasabiUserDTO : users)
				System.out.println(objectService().getName(wasabiUserDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getParentGroupTest() throws WasabiException {
		System.out.println("=== getParentGroupTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting parent group...");
		try {
			groupService().getParentGroup(testGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Getting parent group...");
		try {
			groupService().getParentGroup(testGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting parent group...");
		try {
			groupService().getParentGroup(testGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getSubGroupByNameTest() throws WasabiException {
		System.out.println("=== getSubGroupByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting sub group by name...");
		try {
			groupService().getSubGroupByName(wasabiGroup, "testGroup");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting sub group by name...");
		try {
			groupService().getSubGroupByName(wasabiGroup, "testGroup");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Getting sub group by name...");
		try {
			groupService().getSubGroupByName(wasabiGroup, "testGroup");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getSubGroupsTest() throws WasabiException {
		System.out.println("=== getSubGroupsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup1...");
		WasabiGroupDTO testGroup1 = null;
		try {
			testGroup1 = groupService().create("testGroup1", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating group testGroup2...");
		WasabiGroupDTO testGroup2 = null;
		try {
			testGroup2 = groupService().create("testGroup2", wasabiGroup);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting sub groups...");
		try {
			Vector<WasabiGroupDTO> subgroups = groupService().getSubGroups(wasabiGroup);
			for (WasabiGroupDTO wasabiGroupDTO : subgroups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup2... ");
		aclService().create(testGroup2, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting sub groups...");
		try {
			Vector<WasabiGroupDTO> subgroups = groupService().getSubGroups(wasabiGroup);
			for (WasabiGroupDTO wasabiGroupDTO : subgroups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for wasabi group... ");
		aclService().create(wasabiGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting sub groups...");
		try {
			Vector<WasabiGroupDTO> subgroups = groupService().getSubGroups(wasabiGroup);
			for (WasabiGroupDTO wasabiGroupDTO : subgroups)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getTopLevelGroupsTest() throws WasabiException {
		System.out.println("=== getTopLevelGroupsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.println("Getting top level groups...");
		try {
			Vector<WasabiGroupDTO> toplvl = groupService().getTopLevelGroups();
			for (WasabiGroupDTO wasabiGroupDTO : toplvl)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("Getting top level groups...");
		try {
			Vector<WasabiGroupDTO> toplvl = groupService().getTopLevelGroups();
			for (WasabiGroupDTO wasabiGroupDTO : toplvl)
				System.out.println(objectService().getName(wasabiGroupDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void isDirectMember() throws WasabiException {
		System.out.println("=== isDirectMember() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
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

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isDirectMember(testGroup, user));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isDirectMember(testGroup, user));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isDirectMember(testGroup, user));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void isMember() throws WasabiException {
		System.out.println("=== isMember() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as userRight for group wasabi... ");
		aclService().create(wasabiGroup, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating group testGroup...");
		WasabiGroupDTO testGroup = null;
		try {
			testGroup = groupService().create("testGroup", wasabiGroup);
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

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isMember(testGroup, user));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isMember(testGroup, user));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testGroup... ");
		aclService().create(testGroup, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Check if user is direct member of testGroup: ");
		try {
			System.out.println(groupService().isMember(testGroup, user));
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
