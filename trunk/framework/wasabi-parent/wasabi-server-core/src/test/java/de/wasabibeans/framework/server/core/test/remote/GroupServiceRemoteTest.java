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

package de.wasabibeans.framework.server.core.test.remote;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class GroupServiceRemoteTest extends WasabiRemoteTest {

	private WasabiGroupDTO group1_1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();
		group1_1 = testhelper.initGroupServiceTest();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1DisplayNameTest() throws Exception {
		String displayName = groupService().getDisplayName(group1_1).getValue();
		AssertJUnit.assertEquals("group1_1", displayName);
	}

	@Test
	public void get1GroupByNameTest() throws Exception {
		WasabiGroupDTO test = groupService().getGroupByName("group1_1");
		AssertJUnit.assertEquals(group1_1, test);

		try {
			groupService().getGroupByName(null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertNull(groupService().getGroupByName("doesNotExist"));
	}

	@Test
	public void get1ParentGroupTest() throws Exception {
		WasabiGroupDTO group1 = groupService().getParentGroup(group1_1).getValue();
		AssertJUnit.assertEquals("group1", groupService().getName(group1).getValue());

		AssertJUnit.assertNull(groupService().getParentGroup(group1).getValue());
	}

	@Test
	public void get1SubGroupByNameTest() throws Exception {
		WasabiGroupDTO group1_1_2 = groupService().getSubGroupByName(group1_1, "group1_1_2");
		AssertJUnit.assertEquals("group1_1_2", groupService().getName(group1_1_2).getValue());

		try {
			groupService().getSubGroupByName(group1_1, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertNull(groupService().getSubGroupByName(group1_1, "doesNotExist"));
	}

	@Test
	public void get1SubGroupsTest() throws Exception {
		Vector<WasabiGroupDTO> subgroups = groupService().getSubGroups(group1_1);
		AssertJUnit.assertEquals(2, subgroups.size());
		int hitcount = 0;
		for (WasabiGroupDTO subgroup : subgroups) {
			if (groupService().getName(subgroup).getValue().equals("group1_1_1")
					|| groupService().getName(subgroup).getValue().equals("group1_1_2")) {
				hitcount++;
			}
		}
		AssertJUnit.assertEquals(2, hitcount);
	}

	@Test
	public void get1TopLevelGroupsTest() throws Exception {
		Vector<WasabiGroupDTO> topgroups = groupService().getTopLevelGroups();
		AssertJUnit.assertEquals(3, topgroups.size());
		int hitcount = 0;
		for (WasabiGroupDTO subgroup : topgroups) {
			if (groupService().getName(subgroup).getValue().equals("group1")
					|| groupService().getName(subgroup).getValue().equals(WasabiConstants.WASABI_GROUP_NAME)
					|| groupService().getName(subgroup).getValue().equals(WasabiConstants.ADMINS_GROUP_NAME)) {
				hitcount++;
			}
		}
		AssertJUnit.assertEquals(3, hitcount);
	}

	@Test
	public void get1MembersTest() throws Exception {
		Vector<WasabiUserDTO> members = groupService().getMembers(group1_1);
		AssertJUnit.assertEquals(2, members.size());
		int hitcount = 0;
		for (WasabiUserDTO member : members) {
			if (userService().getName(member).getValue().equals("user1")
					|| userService().getName(member).getValue().equals("user2")) {
				hitcount++;
			}
		}
		AssertJUnit.assertEquals(2, hitcount);
	}

	@Test
	public void get1MemberByNameTest() throws Exception {
		WasabiUserDTO user2 = groupService().getMemberByName(group1_1, "user2");
		AssertJUnit.assertEquals("user2", userService().getName(user2).getValue());

		try {
			groupService().getMemberByName(group1_1, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertNull(groupService().getMemberByName(group1_1, "doesNotExist"));
	}

	@Test
	public void get1AllGroupsTest() throws Exception {
		Vector<WasabiGroupDTO> allgroups = groupService().getAllGroups();
		// wasabi, admins, group1, group1_1, group1_2, group1_1_1, group1_1_2
		AssertJUnit.assertEquals(7, allgroups.size());
		AssertJUnit.assertTrue(allgroups.contains(group1_1));
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void createTest() throws Exception {
		WasabiGroupDTO group2 = groupService().create("group2", null);
		AssertJUnit.assertEquals("group2", groupService().getDisplayName(group2).getValue());
		Vector<WasabiGroupDTO> topgroups = groupService().getTopLevelGroups();
		AssertJUnit.assertTrue(topgroups.contains(group2));

		WasabiGroupDTO group1_1_3 = groupService().create("group1_1_3", group1_1);
		AssertJUnit.assertEquals("group1_1_3", groupService().getDisplayName(group1_1_3).getValue());
		Vector<WasabiGroupDTO> subgroups = groupService().getSubGroups(group1_1);
		AssertJUnit.assertTrue(subgroups.contains(group1_1_3));

		try {
			groupService().create(null, group1_1);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			groupService().create("group1_1_1", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void addMemberTest() throws Exception {
		WasabiUserDTO user3 = userService().create("user3", "user3");
		groupService().addMember(group1_1, user3);
		Vector<WasabiUserDTO> members = groupService().getMembers(group1_1);
		AssertJUnit.assertEquals(3, members.size());
		AssertJUnit.assertTrue(members.contains(user3));

		WasabiGroupDTO group2 = groupService().create("group2", null);
		groupService().addMember(group2, user3);
		Vector<WasabiGroupDTO> memberships = userService().getMemberships(user3);
		AssertJUnit.assertEquals(3, memberships.size());
		AssertJUnit.assertTrue(memberships.contains(group2));

		groupService().addMember(group2, user3);
		AssertJUnit.assertEquals(1, groupService().getMembers(group2).size());
		AssertJUnit.assertEquals(3, userService().getMemberships(user3).size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void isDirectMemberTest() throws Exception {
		WasabiUserDTO user1 = userService().getUserByName("user1");
		AssertJUnit.assertTrue(groupService().isDirectMember(group1_1, user1));

		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		AssertJUnit.assertFalse(groupService().isDirectMember(group1, user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		WasabiGroupDTO group1_1_2 = groupService().getGroupByName("group1_1_2");
		groupService().move(group1_1_2, group1, null);
		Vector<WasabiGroupDTO> subgroups1_1 = groupService().getSubGroups(group1_1);
		Vector<WasabiGroupDTO> subgroups1 = groupService().getSubGroups(group1);
		AssertJUnit.assertEquals(1, subgroups1_1.size());
		AssertJUnit.assertEquals(3, subgroups1.size());
		AssertJUnit.assertFalse(subgroups1_1.contains(group1_1_2));
		AssertJUnit.assertTrue(subgroups1.contains(group1_1_2));

		groupService().move(group1_1_2, null, null);
		subgroups1 = groupService().getSubGroups(group1);
		Vector<WasabiGroupDTO> topgroups = groupService().getTopLevelGroups();
		AssertJUnit.assertEquals(2, subgroups1.size());
		AssertJUnit.assertEquals(4, topgroups.size());
		AssertJUnit.assertFalse(subgroups1.contains(group1_1_2));
		AssertJUnit.assertTrue(topgroups.contains(group1_1_2));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeMemberTest() throws Exception {
		WasabiUserDTO user1 = userService().getUserByName("user1");
		groupService().removeMember(group1_1, user1);
		AssertJUnit.assertEquals(1, groupService().getMembers(group1_1).size());
		AssertJUnit.assertEquals(1, userService().getMemberships(user1).size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		try {
			groupService().rename(group1_1, "group1_1_1", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(groupService().getGroupByName("group1_1"));
			AssertJUnit.assertEquals(2, groupService().getSubGroups(group1).size());
		}

		try {
			groupService().rename(group1_1, null, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		groupService().rename(group1_1, "group1-1", null);
		AssertJUnit.assertEquals("group1-1", groupService().getName(group1_1).getValue());
		AssertJUnit.assertNotNull(groupService().getGroupByName("group1-1"));
		AssertJUnit.assertEquals(2, groupService().getSubGroups(group1).size());
		AssertJUnit.assertNull(groupService().getGroupByName("group1_1"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setDisplayNameTest() throws Exception {
		try {
			groupService().setDisplayName(group1_1, null, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		WasabiGroupDTO group1_2 = groupService().getGroupByName("group1_2");
		groupService().setDisplayName(group1_1, "name", null);
		groupService().setDisplayName(group1_2, "name", null);
		AssertJUnit.assertEquals("name", groupService().getDisplayName(group1_1).getValue());
		AssertJUnit.assertEquals("name", groupService().getDisplayName(group1_2).getValue());
	}

	@Test(dependsOnMethods = { ".*add.*" })
	public void getAllMembersTest() throws Exception {
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		WasabiGroupDTO group1_2 = groupService().getGroupByName("group1_1");
		WasabiGroupDTO group1_1_1 = groupService().getGroupByName("group1_1_1");
		WasabiGroupDTO group1_1_2 = groupService().getGroupByName("group1_1_2");

		WasabiUserDTO userG1_2 = userService().create("userG1_2", "userG1_2");
		WasabiUserDTO userG1_1_1 = userService().create("userG1_1_1", "userG1_1_1");
		WasabiUserDTO userG1_1_2 = userService().create("userG1_1_2", "userG1_1_2");

		groupService().addMember(group1_2, userG1_2);
		groupService().addMember(group1_1_1, userG1_1_1);
		groupService().addMember(group1_1_2, userG1_1_2);

		Vector<WasabiUserDTO> allMembers = groupService().getAllMembers(group1);
		AssertJUnit.assertEquals(5, allMembers.size());
		AssertJUnit.assertTrue(allMembers.contains(userService().getUserByName("user1")));
		AssertJUnit.assertTrue(allMembers.contains(userG1_1_1));
	}

	@Test(dependsOnMethods = { ".*add.*" })
	public void isMemberTest() throws Exception {
		WasabiUserDTO user3 = userService().create("user3", "user3");
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		WasabiGroupDTO group1_1_2 = groupService().getGroupByName("group1_1_2");
		groupService().addMember(group1_1_2, user3);
		AssertJUnit.assertTrue(groupService().isMember(group1, user3));

		WasabiGroupDTO group2 = groupService().create("group2", null);
		WasabiUserDTO user4 = userService().create("user4", "user4");
		groupService().addMember(group2, user4);
		AssertJUnit.assertFalse(groupService().isMember(group1, user4));
	}

	@Test(dependsOnMethods = { ".*add.*" })
	public void removeTest() throws Exception {
		WasabiUserDTO user1 = userService().getUserByName("user1");
		WasabiUserDTO user3 = userService().create("user3", "user3");
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		WasabiGroupDTO group1_1_1 = groupService().getGroupByName("group1_1_1");
		groupService().addMember(group1_1_1, user3);
		groupService().remove(group1_1);
		Vector<WasabiGroupDTO> subgroups1 = groupService().getSubGroups(group1);
		AssertJUnit.assertEquals(1, subgroups1.size());
		AssertJUnit.assertFalse(subgroups1.contains(subgroups1));
		Vector<WasabiGroupDTO> memberships1 = userService().getMemberships(user1);
		AssertJUnit.assertEquals(1, memberships1.size());
		Vector<WasabiGroupDTO> memberships3 = userService().getMemberships(user3);
		AssertJUnit.assertEquals(1, memberships3.size());
	}

	@Test(dependsOnMethods = { ".*set.*" })
	public void getGroupsByDisplayNameTest() throws Exception {
		WasabiGroupDTO group1 = groupService().getGroupByName("group1");
		groupService().setDisplayName(group1, "group1_1", null);
		Vector<WasabiGroupDTO> groups = groupService().getGroupsByDisplayName("group1_1");
		AssertJUnit.assertTrue(groups.contains(group1) && groups.contains(group1_1));
		AssertJUnit.assertEquals(2, groups.size());

		try {
			groupService().getGroupsByDisplayName(null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertTrue(groupService().getGroupsByDisplayName("doesNotExist").isEmpty());
	}
}
