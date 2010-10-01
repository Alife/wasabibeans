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

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class UserServiceRemoteTest extends WasabiRemoteTest {

	private WasabiUserDTO user1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();
		user1 = testhelper.initUserServiceTest();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1AllUsersTest() throws Exception {
		Vector<WasabiUserDTO> users = userService().getAllUsers();
		AssertJUnit.assertTrue(users.contains(user1));
		AssertJUnit.assertEquals(5, users.size());
	}

	@Test
	public void get1DisplayNameTest() throws Exception {
		String displayName = userService().getDisplayName(user1).getValue();
		AssertJUnit.assertEquals("user1", displayName);
	}

	@Test
	public void get1HomeRoomTest() throws Exception {
		WasabiRoomDTO homeRoom = roomService().getRoomByName(roomService().getRootHome(), "user1");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService().getHomeRoom(user1).getValue());
	}

	@Test
	public void get1PasswordTest() throws Exception {
		String expectedPwd = HashGenerator.generateHash("user1", hashAlgorithms.SHA);
		AssertJUnit.assertEquals(expectedPwd, userService().getPassword(user1));
	}

	@Test
	public void get1StartRoomTest() throws Exception {
		WasabiRoomDTO startRoom = roomService().getRoomByName(roomService().getRootHome(), "user1");
		AssertJUnit.assertNotNull(startRoom);
		AssertJUnit.assertEquals(startRoom, userService().getStartRoom(user1).getValue());
	}

	@Test
	public void get1StatusTest() throws Exception {
		AssertJUnit.assertTrue((Boolean) userService().getStatus(user1).getValue());
	}

	@Test
	public void get1UserByNameTest() throws Exception {
		WasabiUserDTO test = userService().getUserByName("user1");
		AssertJUnit.assertEquals(user1, test);

		try {
			userService().getUserByName(null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertNull(userService().getUserByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1MembershipsTest() throws Exception {
		Vector<WasabiGroupDTO> memberships = userService().getMemberships(user1);
		AssertJUnit.assertEquals(1, memberships.size());
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
		AssertJUnit.assertTrue(memberships.contains(wasabiGroup));
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void get2UsersRoomTest() throws Exception {
		WasabiRoomDTO homeRoom = userService().getHomeRoom(user1).getValue();
		Vector<WasabiUserDTO> result = userService().getUsers(homeRoom);
		AssertJUnit.assertEquals(1, result.size());
		AssertJUnit.assertTrue(result.contains(user1));
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void get2UsersByNameRoomTest() throws Exception {
		WasabiRoomDTO homeRoom = userService().getHomeRoom(user1).getValue();
		try {
			userService().getUserByName(homeRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		// search not existing user
		AssertJUnit.assertNull(userService().getUserByName(homeRoom, "doesNotExist"));

		// search existing but not present user
		AssertJUnit.assertNull(userService().getUserByName(homeRoom, "user2"));

		// search existing and present user
		WasabiUserDTO test = userService().getUserByName(homeRoom, "user1");
		AssertJUnit.assertEquals(user1, test);
	}

	@Test(dependsOnMethods = { ".*get2.*" })
	public void createTest() throws WasabiException {
		WasabiUserDTO user3 = userService().create("user3", "pwd");
		AssertJUnit.assertEquals("user3", userService().getDisplayName(user3).getValue());
		WasabiRoomDTO homeRoom = roomService().getRoomByName(roomService().getRootHome(), "user3");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService().getHomeRoom(user3).getValue());
		AssertJUnit.assertEquals(homeRoom, userService().getStartRoom(user3).getValue());
		AssertJUnit.assertEquals(HashGenerator.generateHash("pwd", hashAlgorithms.SHA), userService()
				.getPassword(user3));
		AssertJUnit.assertTrue((Boolean) userService().getStatus(user3).getValue());
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
		AssertJUnit.assertTrue(groupService().isDirectMember(wasabiGroup, user3));
		AssertJUnit.assertTrue(userService().getUsers(homeRoom).contains(user3));

		try {
			userService().create(null, "pwd");
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			userService().create("name", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			userService().create("user3", "pwd");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		try {
			userService().rename(user1, "user2", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(userService().getUserByName("user1"));
			AssertJUnit.assertEquals(5, userService().getAllUsers().size());
		}

		userService().rename(user1, "user_2", null);
		AssertJUnit.assertEquals("user_2", userService().getName(user1).getValue());
		AssertJUnit.assertNotNull(userService().getUserByName("user_2"));
		AssertJUnit.assertEquals(5, userService().getAllUsers().size());
		AssertJUnit.assertNull(userService().getUserByName("user1"));
		AssertJUnit.assertEquals(HashGenerator.generateHash("user1", hashAlgorithms.SHA), userService().getPassword(
				user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setDisplayNameTest() throws Exception {
		try {
			userService().setDisplayName(user1, null, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().setDisplayName(user1, "name", null);
		userService().setDisplayName(user2, "name", null);
		AssertJUnit.assertEquals("name", userService().getDisplayName(user1).getValue());
		AssertJUnit.assertEquals("name", userService().getDisplayName(user2).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setPasswordTest() throws Exception {
		try {
			userService().setPassword(user1, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		userService().setPassword(user1, "newPwd");
		AssertJUnit.assertEquals(HashGenerator.generateHash("newPwd", hashAlgorithms.SHA), userService().getPassword(
				user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setStartRoomTest() throws Exception {
		try {
			userService().setStartRoom(user1, null, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		WasabiRoomDTO newRoom = roomService().create("newRoom", rootRoom);
		userService().setStartRoom(user1, newRoom, null);
		AssertJUnit.assertEquals(newRoom, userService().getStartRoom(user1).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setStatusTest() throws Exception {
		userService().setStatus(user1, false, null);
		AssertJUnit.assertFalse((Boolean) userService().getStatus(user1).getValue());
		// TODO any other consequences to test??
	}

	@Test(dependsOnMethods = { "createTest" })
	public void enterTest() throws Exception {
		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().enter(user1, rootRoom);
		userService().enter(user2, rootRoom);
		Vector<WasabiUserDTO> result = userService().getUsers(rootRoom);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertTrue(result.contains(user1) && result.contains(user2));
	}

	@Test(dependsOnMethods = { "enterTest" })
	public void leaveTest() throws Exception {
		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().enter(user1, rootRoom);
		userService().enter(user2, rootRoom);
		userService().leave(user1, rootRoom);
		Vector<WasabiUserDTO> result = userService().getUsers(rootRoom);
		AssertJUnit.assertEquals(1, result.size());
		AssertJUnit.assertTrue(result.contains(user2));
	}

	@Test(dependsOnMethods = { "enterTest" })
	public void removeTest() throws Exception {
		userService().enter(user1, rootRoom);

		userService().remove(user1, null);
		// check that user is removed
		Vector<WasabiUserDTO> users = userService().getAllUsers();
		AssertJUnit.assertFalse(users.contains(user1));
		AssertJUnit.assertEquals(4, users.size());
		// check that the user's home-room is removed
		AssertJUnit.assertNull(roomService().getRoomByName(roomService().getRootHome(), "user1"));
		// check that the user not listed as present any more
		AssertJUnit.assertTrue(userService().getUsers(rootRoom).isEmpty());
		// check that the user is not listed as member any more
		WasabiGroupDTO wasabi = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
		AssertJUnit.assertTrue(!groupService().getMembers(wasabi).contains(user1));
		// check that the user's entries in the database have been removed by attempting to create a new user with
		// exactly the same attributes
		userService().create("user1", "user1");
	}

	@Test(dependsOnMethods = { "leaveTest" })
	public void getWhereaboutsTest() throws Exception {
		// create more rooms
		WasabiRoomDTO room1 = roomService().create("room1", rootRoom);
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		// enter more rooms (initially user is present in his home room)
		userService().enter(user1, room1);
		userService().enter(user1, room2);

		// check whereabouts
		Vector<WasabiRoomDTO> whereabouts = userService().getWhereabouts(user1);
		AssertJUnit.assertEquals(3, whereabouts.size());
		AssertJUnit.assertTrue(whereabouts.contains(room1) && whereabouts.contains(room2)
				&& whereabouts.contains(userService().getHomeRoom(user1).getValue()));

		// leave a room
		userService().leave(user1, room1);

		// check whereabouts again
		whereabouts = userService().getWhereabouts(user1);
		AssertJUnit.assertEquals(2, whereabouts.size());
		AssertJUnit.assertTrue(whereabouts.contains(room2)
				&& whereabouts.contains(userService().getHomeRoom(user1).getValue()));
	}

	@Test(dependsOnMethods = { ".*set.*" })
	public void getUsersByDisplayName() throws WasabiException {
		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().setDisplayName(user2, "user1", null);
		Vector<WasabiUserDTO> users = userService().getUsersByDisplayName("user1");
		AssertJUnit.assertTrue(users.contains(user1) && users.contains(user2));
		AssertJUnit.assertEquals(2, users.size());

		try {
			userService().getUsersByDisplayName(null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertTrue(userService().getUsersByDisplayName("doesNotExist").isEmpty());
	}
}
