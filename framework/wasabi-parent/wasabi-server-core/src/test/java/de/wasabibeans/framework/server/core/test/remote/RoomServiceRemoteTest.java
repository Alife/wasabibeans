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
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class RoomServiceRemoteTest extends WasabiRemoteTest {

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
	public void getRootRoomTest() throws WasabiException {
		WasabiRoomDTO rootRoomActual = roomService().getRootRoom();
		AssertJUnit.assertEquals(rootRoom, rootRoomActual);
	}

	@Test
	public void getRootHomeTest() throws WasabiException {
		WasabiRoomDTO rootHome = roomService().getRootHome();
		AssertJUnit.assertNotNull(rootHome);
		AssertJUnit.assertEquals(WasabiConstants.HOME_ROOM_NAME, roomService().getName(rootHome).getValue());
	}

	@Test
	public void getEnvironmentTest() throws Exception {
		WasabiRoomDTO environment = roomService().getEnvironment(room1).getValue();
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test
	public void getRoomByNameTest() throws WasabiException {
		WasabiRoomDTO test = roomService().getRoomByName(rootRoom, "room1");
		AssertJUnit.assertEquals(room1, test);

		try {
			roomService().getRoomByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertNull(roomService().getRoomByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void getRoomsTest() throws WasabiException {
		Vector<WasabiRoomDTO> rooms = roomService().getRooms(rootRoom);
		AssertJUnit.assertTrue(rooms.contains(room1));
		AssertJUnit.assertEquals(2, rooms.size());
	}

	@Test(dependsOnMethods = { ".*get.*" })
	public void createTest() throws WasabiException {
		WasabiRoomDTO newRoom = roomService().create("room2", rootRoom);
		AssertJUnit.assertNotNull(newRoom);
		AssertJUnit.assertEquals(newRoom, roomService().getRoomByName(rootRoom, "room2"));

		try {
			roomService().create(null, rootRoom);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			roomService().create("test", null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
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
			roomService().rename(room2, "room1", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(roomService().getRoomByName(rootRoom, "room2"));
			AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		}

		roomService().rename(room2, "room_2", null);
		AssertJUnit.assertEquals("room_2", roomService().getName(room2).getValue());
		AssertJUnit.assertNotNull(roomService().getRoomByName(rootRoom, "room_2"));
		AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		AssertJUnit.assertNull(roomService().getRoomByName(rootRoom, "room2"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws WasabiException {
		WasabiRoomDTO sub = roomService().create("room2", room1);
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		try {
			roomService().move(sub, rootRoom, null);
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiRoomDTO> roomsOfRoom1 = roomService().getRooms(room1);
			AssertJUnit.assertTrue(roomsOfRoom1.contains(sub));
			AssertJUnit.assertEquals(1, roomsOfRoom1.size());
			AssertJUnit.assertEquals(3, roomService().getRooms(rootRoom).size());
		}

		roomService().move(sub, room2, null);
		Vector<WasabiRoomDTO> roomsOfRoom1 = roomService().getRooms(room1);
		AssertJUnit.assertFalse(roomsOfRoom1.contains(sub));
		AssertJUnit.assertEquals(0, roomsOfRoom1.size());
		Vector<WasabiRoomDTO> roomsOfRoom2 = roomService().getRooms(room2);
		AssertJUnit.assertTrue(roomsOfRoom2.contains(sub));
		AssertJUnit.assertEquals(1, roomsOfRoom2.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws WasabiException {
		WasabiRoomDTO room2 = roomService().create("room2", rootRoom);

		roomService().remove(room2);
		Vector<WasabiRoomDTO> rooms = roomService().getRooms(rootRoom);
		AssertJUnit.assertFalse(rooms.contains(room2));
		AssertJUnit.assertEquals(2, rooms.size());
	}
}
