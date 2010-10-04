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

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class RoomServiceRemoteTest extends WasabiRemoteTest {

	private Long optLockId = -1L;
	private WasabiRoomDTO room1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		room1 = testhelper.initRoomServiceTest();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1RootRoomTest() throws WasabiException {
		WasabiRoomDTO rootRoomActual = roomService().getRootRoom();
		AssertJUnit.assertEquals(rootRoom, rootRoomActual);
	}

	@Test
	public void get1RootHomeTest() throws WasabiException {
		WasabiRoomDTO rootHome = roomService().getRootHome();
		AssertJUnit.assertNotNull(rootHome);
		AssertJUnit.assertEquals(WasabiConstants.HOME_ROOM_NAME, roomService().getName(rootHome).getValue());
	}

	@Test
	public void get1EnvironmentTest() throws Exception {
		WasabiRoomDTO environment = roomService().getEnvironment(room1).getValue();
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test
	public void get1RoomByNameTest() throws WasabiException {
		WasabiRoomDTO test = roomService().getRoomByName(rootRoom, "room1");
		AssertJUnit.assertEquals(room1, test);

		try {
			roomService().getRoomByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertNull(roomService().getRoomByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1RoomsTest() throws WasabiException {
		Vector<WasabiRoomDTO> rooms = roomService().getRooms(rootRoom);
		AssertJUnit.assertTrue(rooms.contains(room1));
		AssertJUnit.assertEquals(2, rooms.size());
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void createTest() throws WasabiException {
		WasabiRoomDTO newRoom = roomService().create("room2", rootRoom);
		AssertJUnit.assertNotNull(newRoom);
		AssertJUnit.assertEquals(newRoom, roomService().getRoomByName(rootRoom, "room2"));

		try {
			roomService().create(null, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			roomService().create("test", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			roomService().create("room2", rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws WasabiException {
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		try {
			roomService().rename(room2, "room1", optLockId);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(roomService().getRoomByName(rootRoom, "room2"));
			AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		}

		roomService().rename(room2, "room_2", optLockId);
		AssertJUnit.assertEquals("room_2", roomService().getName(room2).getValue());
		AssertJUnit.assertNotNull(roomService().getRoomByName(rootRoom, "room_2"));
		AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		AssertJUnit.assertNull(roomService().getRoomByName(rootRoom, "room2"));
		
		// test renaming a user's home-room
		WasabiRoomDTO homeRoom = userService().getHomeRoom(userService().getUserByName("user")).getValue();
		try {
			roomService().rename(homeRoom, "test", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws WasabiException {
		WasabiRoomDTO sub = roomService().create("room2", room1);
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		try {
			roomService().move(sub, rootRoom, optLockId);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiRoomDTO> roomsOfRoom1 = roomService().getRooms(room1);
			AssertJUnit.assertTrue(roomsOfRoom1.contains(sub));
			AssertJUnit.assertEquals(1, roomsOfRoom1.size());
			AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		}

		roomService().move(sub, room2, optLockId);
		Vector<WasabiRoomDTO> roomsOfRoom1 = roomService().getRooms(room1);
		AssertJUnit.assertFalse(roomsOfRoom1.contains(sub));
		AssertJUnit.assertEquals(0, roomsOfRoom1.size());
		Vector<WasabiRoomDTO> roomsOfRoom2 = roomService().getRooms(room2);
		AssertJUnit.assertTrue(roomsOfRoom2.contains(sub));
		AssertJUnit.assertEquals(1, roomsOfRoom2.size());
		
		// test moving a user's home-room
		WasabiRoomDTO homeRoom = userService().getHomeRoom(userService().getUserByName("user")).getValue();
		try {
			roomService().move(homeRoom, rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}
	}

	@Test//(dependsOnMethods = { "createTest" })
	public void removeTest() throws WasabiException {
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		roomService().remove(room2, optLockId);
		Vector<WasabiRoomDTO> rooms = roomService().getRooms(rootRoom);
		AssertJUnit.assertFalse(rooms.contains(room2));
		AssertJUnit.assertEquals(2, rooms.size());
		
		// test removing a user's home-room
		WasabiRoomDTO homeRoom = userService().getHomeRoom(userService().getUserByName("user")).getValue();
		try {
			roomService().remove(homeRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsTestDepth() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[5][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
				}
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiRoomDTO> result = roomService().getRooms(room, 3);
		AssertJUnit.assertEquals(20, result.size());

		// test for layer 0 only
		result = roomService().getRooms(room, 0);
		AssertJUnit.assertEquals(5, result.size());

		// test for all 5 layers
		result = roomService().getRooms(room, -1);
		AssertJUnit.assertEquals(25, result.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 rooms with 5 different timestamps (before, begin, in-between, end, after)
		WasabiRoomDTO[] rooms = new WasabiRoomDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			rooms[i] = roomService().create("room" + i, room);
			objectService().setCreatedOn(rooms[i], dates[i], optLockId);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiRoomDTO> result = roomService().getRoomsByCreationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(rooms[0]));
		AssertJUnit.assertFalse(result.contains(rooms[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByCreationDateTestDepth() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[5][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
				}
				objectService().setCreatedOn(rooms[d][t], dates[t], optLockId);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiRoomDTO> result = roomService().getRoomsByCreationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(rooms[d][0]));
			AssertJUnit.assertFalse(result.contains(rooms[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(rooms[4][t]));
		}

		// test for layer 0 only
		result = roomService().getRoomsByCreationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(rooms[0][0]));
		AssertJUnit.assertFalse(result.contains(rooms[0][4]));

		// test for all 5 layers
		result = roomService().getRoomsByCreationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(rooms[d][0]));
			AssertJUnit.assertFalse(result.contains(rooms[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByCreatorTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create rooms that should not be returned
		WasabiRoomDTO room1ThisUser = roomService().create("room1ThisUser", rootRoom);
		WasabiRoomDTO room2ThisUser = roomService().create("room2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another room to be found
		roomService().create("anotherRoomOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get rooms created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiRoomDTO> result = roomService().getRoomsByCreator(root);
		// home-room of user 'admin', home-room of user 'user', room1 via TestHelper, the room created in this method
		AssertJUnit.assertEquals(4, result.size());
		AssertJUnit.assertFalse(result.contains(room1ThisUser));
		AssertJUnit.assertFalse(result.contains(room2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByCreatorTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a room that should not be returned (wrong modifier)
		WasabiRoomDTO room1ThisUser = roomService().create("room1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another room that should not be returned (correct modifier, but wrong location)
		roomService().create("anotherRoomOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get rooms created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiRoomDTO> result = roomService().getRoomsByCreator(root, rootRoom);
		AssertJUnit.assertEquals(1, result.size());
		AssertJUnit.assertFalse(result.contains(room1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByModificationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 rooms with 5 different timestamps (before, begin, in-between, end, after)
		WasabiRoomDTO[] rooms = new WasabiRoomDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			rooms[i] = roomService().create("room" + i, room);
			objectService().setModifiedOn(rooms[i], dates[i], optLockId);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiRoomDTO> result = roomService().getRoomsByModificationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(rooms[0]));
		AssertJUnit.assertFalse(result.contains(rooms[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByModificationDateTestDepth() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[5][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
				}
				objectService().setModifiedOn(rooms[d][t], dates[t], optLockId);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiRoomDTO> result = roomService().getRoomsByModificationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(rooms[d][0]));
			AssertJUnit.assertFalse(result.contains(rooms[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(rooms[4][t]));
		}

		// test for layer 0 only
		result = roomService().getRoomsByModificationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(rooms[0][0]));
		AssertJUnit.assertFalse(result.contains(rooms[0][4]));

		// test for all 5 layers
		result = roomService().getRoomsByModificationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(rooms[d][0]));
			AssertJUnit.assertFalse(result.contains(rooms[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByModifierTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create rooms that should not be returned
		WasabiRoomDTO room1ThisUser = roomService().create("room1ThisUser", rootRoom);
		WasabiRoomDTO room2ThisUser = roomService().create("room2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another room to be found
		roomService().create("anotherRoomOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get rooms modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiRoomDTO> result = roomService().getRoomsByModifier(root);
		// home-room of user 'admin', home-room of user 'user', room1 via TestHelper, the room created in this method
		AssertJUnit.assertEquals(4, result.size());
		AssertJUnit.assertFalse(result.contains(room1ThisUser));
		AssertJUnit.assertFalse(result.contains(room2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getRoomsByModifierTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a room that should not be returned (wrong modifier)
		WasabiRoomDTO room1ThisUser = roomService().create("room1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another room that should not be returned (correct modifier, but wrong location)
		roomService().create("anotherRoomOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get rooms modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiRoomDTO> result = roomService().getRoomsByModifier(root, rootRoom);
		AssertJUnit.assertEquals(1, result.size());
		AssertJUnit.assertFalse(result.contains(room1ThisUser));
	}
}
