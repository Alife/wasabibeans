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
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.auth.SqlLoginModule;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomService;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.manager.WasabiManagerRemote;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class RoomServiceRemoteTest extends Arquillian {

	private RemoteWasabiConnector reWaCon;
	private WasabiManagerRemote waMan;

	private RoomServiceRemote roomService;

	private WasabiRoomDTO rootRoom;

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // auth
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(RoomService.class.getPackage()) // services impl
				.addPackage(RoomServiceLocal.class.getPackage()) // services local
				.addPackage(RoomServiceRemote.class.getPackage()); // services remote

		return testArchive;
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws LoginException, NamingException {
		// connect and login
		reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		// lookup wasabi manager
		waMan = (WasabiManagerRemote) reWaCon.lookup("WasabiManager/remote");

		// lookup services
		roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
	}

	@AfterClass
	public void tearDownAfterAllMethods() throws LoginException, NamingException {
		// disconnect and logout
		reWaCon.disconnect();
	}

	@BeforeMethod
	public void setUpBeforeEachMethod() throws LoginException, NamingException {
		// initialize jcr repository
		rootRoom = waMan.initWorkspace("default");
	}

	@Test
	public void createTest() throws WasabiException {
		WasabiRoomDTO newRoom = roomService.create("room", rootRoom);
		AssertJUnit.assertEquals("room", roomService.getName(newRoom));

		try {
			roomService.create(null, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			roomService.create("test", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			roomService.create("room", rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test
	public void getRootRoomTest() throws WasabiException {
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		AssertJUnit.assertNotNull(rootRoom);
	}

	@Test
	public void getRootHomeTest() throws WasabiException {
		WasabiRoomDTO rootHome = roomService.getRootHome();
		AssertJUnit.assertNotNull(rootHome);
	}

	@Test(dependsOnMethods = "createTest")
	public void getEnvironmentTest() throws WasabiException {
		WasabiRoomDTO newRoom = roomService.create("room", rootRoom);

		WasabiRoomDTO environment = roomService.getEnvironment(newRoom);
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test(dependsOnMethods = "createTest")
	public void getRoomByNameTest() throws WasabiException {
		roomService.create("room1", rootRoom);
		WasabiRoomDTO room2 = roomService.create("room2", rootRoom);

		WasabiRoomDTO test = roomService.getRoomByName(rootRoom, "room2");
		AssertJUnit.assertEquals(room2, test);

		try {
			roomService.getRoomByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		AssertJUnit.assertNull(roomService.getRoomByName(rootRoom, "doesNotExist"));
	}

	@Test(dependsOnMethods = "createTest")
	public void getRoomsTest() throws WasabiException {
		WasabiRoomDTO room1 = roomService.create("room1", rootRoom);
		WasabiRoomDTO room2 = roomService.create("room2", rootRoom);

		Vector<WasabiRoomDTO> rooms = roomService.getRooms(rootRoom);
		AssertJUnit.assertTrue(rooms.contains(room1));
		AssertJUnit.assertTrue(rooms.contains(room2));
		AssertJUnit.assertEquals(3, rooms.size());
	}

	@Test(dependsOnMethods = ".*get.*")
	public void renameTest() throws WasabiException {
		roomService.create("room1", rootRoom);
		WasabiRoomDTO room2 = roomService.create("room2", rootRoom);

		try {
			roomService.rename(room2, "room1");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(roomService.getRoomByName(rootRoom, "room2"));
			AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
		}

		roomService.rename(room2, "room_2");
		AssertJUnit.assertEquals("room_2", roomService.getName(room2));
		AssertJUnit.assertNotNull(roomService.getRoomByName(rootRoom, "room_2"));
		AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
		AssertJUnit.assertNull(roomService.getRoomByName(rootRoom, "room2"));
	}

	@Test(dependsOnMethods = ".*get.*")
	public void moveTest() throws WasabiException {
		WasabiRoomDTO room1 = roomService.create("room1", rootRoom);
		WasabiRoomDTO sub = roomService.create("room2", room1);
		WasabiRoomDTO room2 = roomService.create("room2", rootRoom);

		try {
			roomService.move(sub, rootRoom);
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiRoomDTO> roomsOfRoom1 = roomService.getRooms(room1);
			AssertJUnit.assertTrue(roomsOfRoom1.contains(sub));
			AssertJUnit.assertEquals(1, roomsOfRoom1.size());
			AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
		}

		roomService.move(sub, room2);
		Vector<WasabiRoomDTO> roomsOfRoom1 = roomService.getRooms(room1);
		AssertJUnit.assertFalse(roomsOfRoom1.contains(sub));
		AssertJUnit.assertEquals(0, roomsOfRoom1.size());
		Vector<WasabiRoomDTO> roomsOfRoom2 = roomService.getRooms(room2);
		AssertJUnit.assertTrue(roomsOfRoom2.contains(sub));
		AssertJUnit.assertEquals(1, roomsOfRoom2.size());
	}

	@Test(dependsOnMethods = ".*get.*")
	public void removeTest() throws WasabiException {
		roomService.create("room1", rootRoom);
		WasabiRoomDTO room2 = roomService.create("room2", rootRoom);

		roomService.remove(room2);
		Vector<WasabiRoomDTO> rooms = roomService.getRooms(rootRoom);
		AssertJUnit.assertFalse(rooms.contains(room2));
		AssertJUnit.assertEquals(2, rooms.size());
	}
}
