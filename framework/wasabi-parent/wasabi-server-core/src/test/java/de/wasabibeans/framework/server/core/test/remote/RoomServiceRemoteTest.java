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
import javax.jcr.RepositoryException;
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

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // auth
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(RoomService.class.getPackage()).addPackage(RoomServiceLocal.class.getPackage()).addPackage(
						RoomServiceRemote.class.getPackage());

		return testArchive;
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws NamingException, LoginException {
		reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();
	}

	@AfterClass
	public void tearDownAfterAllMethos() throws LoginException, NamingException {
		reWaCon.disconnect();
	}

	@BeforeMethod
	public void setUpForEachMethod() throws NamingException, LoginException, RepositoryException {
		WasabiManagerRemote waMan = (WasabiManagerRemote) reWaCon.lookup("WasabiManager/remote");
		waMan.initWorkspace("default");
	}

	@Test
	public void test() throws LoginException, NamingException {
		// *** room service lookup
		RoomServiceRemote roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
		AssertJUnit.assertNotNull(roomService);

		// *** getRootRoom
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		AssertJUnit.assertNotNull(rootRoom);

		// *** getRootHome
		WasabiRoomDTO rootHome = roomService.getRootHome();
		AssertJUnit.assertNotNull(rootHome);

		// *** create
		WasabiRoomDTO newRoom = roomService.create("newRoom", rootRoom);
		AssertJUnit.assertEquals("newRoom", roomService.getName(newRoom));

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
			roomService.create("newRoom", rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof ObjectAlreadyExistsException;
		}

		// *** getEnvironment
		WasabiRoomDTO environment = roomService.getEnvironment(newRoom);
		AssertJUnit.assertEquals(rootRoom, environment);

		// *** getRoomByName
		roomService.create("newRoom2", rootRoom);
		WasabiRoomDTO newRoom2 = roomService.getRoomByName(rootRoom, "newRoom2");
		AssertJUnit.assertEquals("newRoom2", roomService.getName(newRoom2));

		try {
			roomService.getRoomByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		AssertJUnit.assertNull(roomService.getRoomByName(rootRoom, "doesNotExist"));

		// *** getRooms
		Vector<WasabiRoomDTO> rooms = roomService.getRooms(rootRoom);
		AssertJUnit.assertTrue(rooms.contains(newRoom));
		AssertJUnit.assertTrue(rooms.contains(newRoom2));

		// *** rename
		try {
			roomService.rename(newRoom2, "newRoom");
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof ObjectAlreadyExistsException;
			AssertJUnit.assertNotNull(roomService.getRoomByName(rootRoom, "newRoom2"));
			AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
		}

		roomService.rename(newRoom2, "room2");
		AssertJUnit.assertEquals("room2", roomService.getName(newRoom2));
		AssertJUnit.assertNotNull(roomService.getRoomByName(rootRoom, "room2"));
		AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
		AssertJUnit.assertNull(roomService.getRoomByName(rootRoom, "newRoom2"));

		// *** move
		try {
			roomService.create("newRoom", rootHome);
			roomService.move(newRoom, rootHome);
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof ObjectAlreadyExistsException;
			AssertJUnit.assertNotNull(roomService.getRoomByName(rootRoom, "newRoom"));
			AssertJUnit.assertEquals(3, roomService.getRooms(rootRoom).size());
			AssertJUnit.assertEquals(1, roomService.getRooms(rootHome).size());
		}

		roomService.move(newRoom2, rootHome);
		AssertJUnit.assertNotNull(roomService.getRoomByName(rootHome, "room2"));
		AssertJUnit.assertNull(roomService.getRoomByName(rootRoom, "room2"));
		AssertJUnit.assertEquals(2, roomService.getRooms(rootHome).size());
		AssertJUnit.assertEquals(2, roomService.getRooms(rootRoom).size());

		// *** remove
		roomService.remove(newRoom2);
		AssertJUnit.assertNull(roomService.getRoomByName(rootHome, "room2"));
		AssertJUnit.assertEquals(1, roomService.getRooms(rootHome).size());
	}
}
