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
		String displayName = userService().getDisplayName(user1);
		AssertJUnit.assertEquals("user1", displayName);
	}

	@Test
	public void get1HomeRoomTest() throws Exception {
		WasabiRoomDTO homeRoom = roomService().getRoomByName(roomService().getRootHome(), "user1");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService().getHomeRoom(user1));
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
		AssertJUnit.assertEquals(startRoom, userService().getStartRoom(user1));
	}

	@Test
	public void get1StatusTest() throws Exception {
		AssertJUnit.assertTrue(userService().getStatus(user1));
	}

	@Test
	public void get1UserByNameTest() throws Exception {
		WasabiUserDTO test = userService().getUserByName("user1");
		AssertJUnit.assertEquals(user1, test);

		try {
			userService().getUserByName(null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
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
	public void createTest() throws WasabiException {
		WasabiUserDTO user3 = userService().create("user3", "pwd");
		AssertJUnit.assertEquals("user3", userService().getDisplayName(user3));
		WasabiRoomDTO homeRoom = roomService().getRoomByName(roomService().getRootHome(), "user3");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService().getHomeRoom(user3));
		AssertJUnit.assertEquals(homeRoom, userService().getStartRoom(user3));
		AssertJUnit.assertEquals(HashGenerator.generateHash("pwd", hashAlgorithms.SHA), userService()
				.getPassword(user3));
		AssertJUnit.assertTrue(userService().getStatus(user3));
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
		AssertJUnit.assertTrue(groupService().isDirectMember(wasabiGroup, user3));

		try {
			userService().create(null, "pwd");
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			userService().create("name", null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			userService().create("user3", "pwd");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws Exception {
		// still todos in remove
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		try {
			userService().rename(user1, "user2");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(userService().getUserByName("user1"));
			AssertJUnit.assertEquals(5, userService().getAllUsers().size());
		}

		userService().rename(user1, "user_2");
		AssertJUnit.assertEquals("user_2", userService().getName(user1));
		AssertJUnit.assertNotNull(userService().getUserByName("user_2"));
		AssertJUnit.assertEquals(5, userService().getAllUsers().size());
		AssertJUnit.assertNull(userService().getUserByName("user1"));
		AssertJUnit.assertEquals(HashGenerator.generateHash("user1", hashAlgorithms.SHA), userService().getPassword(
				user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setDisplayNameTest() throws Exception {
		try {
			userService().setDisplayName(user1, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().setDisplayName(user1, "name");
		userService().setDisplayName(user2, "name");
		AssertJUnit.assertEquals("name", userService().getDisplayName(user1));
		AssertJUnit.assertEquals("name", userService().getDisplayName(user2));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setPasswordTest() throws Exception {
		try {
			userService().setPassword(user1, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		userService().setPassword(user1, "newPwd");
		AssertJUnit.assertEquals(HashGenerator.generateHash("newPwd", hashAlgorithms.SHA), userService().getPassword(
				user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setStartRoomTest() throws Exception {
		try {
			userService().setStartRoom(user1, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		WasabiRoomDTO newRoom = roomService().create("newRoom", rootRoom);
		userService().setStartRoom(user1, newRoom);
		AssertJUnit.assertEquals(newRoom, userService().getStartRoom(user1));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setStatusTest() throws Exception {
		userService().setStatus(user1, false);
		AssertJUnit.assertFalse(userService().getStatus(user1));
		//TODO any other consequences to test??
	}

	@Test(dependsOnMethods = { ".*set.*" })
	public void getUsersByDisplayName() throws WasabiException {
		WasabiUserDTO user2 = userService().getUserByName("user2");
		userService().setDisplayName(user2, "user1");
		Vector<WasabiUserDTO> users = userService().getUsersByDisplayName("user1");
		AssertJUnit.assertTrue(users.contains(user1) && users.contains(user2));
		AssertJUnit.assertEquals(2, users.size());

		try {
			userService().getUsersByDisplayName(null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed 
		}

		AssertJUnit.assertTrue(userService().getUsersByDisplayName("doesNotExist").isEmpty());
	}
}
