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

package de.wasabibeans.framework.server.core.test.performance;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class SimpleRightsPerformanceTest extends WasabiRemoteTest {

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

	@Test
	public void createRooms1() throws WasabiException {
		int numberOfRooms = 100;

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		long startTime = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			roomService().create(String.valueOf(i), usersHome);
			// System.out.println("create room " + i);
		}
		long endTime = java.lang.System.currentTimeMillis();

		long startTimeRead = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			WasabiRoomDTO room = roomService().getRoomByName(usersHome, String.valueOf(i));
			roomService().getName(room);
		}
		long endTimeRead = java.lang.System.currentTimeMillis();

		System.out.println("Write pass 1: " + (endTime - startTime));
		System.out.println("Read pass 1: " + (endTimeRead - startTimeRead));
	}

	@Test
	public void createRooms2() throws WasabiException {
		int numberOfRooms = 100;

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		long startTime = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			roomService().create(String.valueOf(i), usersHome);
			// System.out.println("create room " + i);
		}
		long endTime = java.lang.System.currentTimeMillis();

		long startTimeRead = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			WasabiRoomDTO room = roomService().getRoomByName(usersHome, String.valueOf(i));
			roomService().getName(room);
		}
		long endTimeRead = java.lang.System.currentTimeMillis();

		System.out.println("Write pass 2: " + (endTime - startTime));
		System.out.println("Read pass 2: " + (endTimeRead - startTimeRead));
	}

	@Test
	public void createRooms3() throws WasabiException {
		int numberOfRooms = 100;

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		long startTime = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			roomService().create(String.valueOf(i), usersHome);
			// System.out.println("create room " + i);
		}
		long endTime = java.lang.System.currentTimeMillis();

		long startTimeRead = java.lang.System.currentTimeMillis();
		for (long i = startTime; i < (startTime + numberOfRooms); i++) {
			WasabiRoomDTO room = roomService().getRoomByName(usersHome, String.valueOf(i));
			roomService().getName(room);
		}
		long endTimeRead = java.lang.System.currentTimeMillis();

		System.out.println("Write pass 3: " + (endTime - startTime));
		System.out.println("Read pass 3: " + (endTimeRead - startTimeRead));
	}
}