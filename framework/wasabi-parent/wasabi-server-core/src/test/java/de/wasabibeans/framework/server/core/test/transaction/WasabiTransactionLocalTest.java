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
package de.wasabibeans.framework.server.core.test.transaction;

import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.IN_CONTAINER)
public class WasabiTransactionLocalTest extends Arquillian {

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
				.addPackage(WasabiUserACL.class.getPackage()) // authorization
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(RoomServiceImpl.class.getPackage()) // internal
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	class TestThread extends Thread {
		private Thread t;
		private String username;

		public TestThread(Thread t, String username) {
			super();
			this.t = t;
			this.username = username;
		}

		@Override
		public void run() {
			try {
				InitialContext jndi = new InitialContext();
				TestHelperLocal testhelper = (TestHelperLocal) jndi.lookup("test/TestHelper/local");
				
				Callable<Object> toDo = new LostUpdateCallable(t, username);
				testhelper.call(toDo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class LostUpdateCallable implements Callable<Object> {
		
		private Thread t;
		private String username;

		public LostUpdateCallable(Thread t, String username) {
			this.t = t;
			this.username = username;
		}

		@Override
		public Object call() throws Exception {
			System.out.println(username + " meldet sich beim Server an");
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			loCon.connect();
			loCon.login(username, username);

			System.out.println(username + " liest");
			RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
			WasabiRoomDTO rootRoom = roomService.getRootRoom();
			WasabiRoomDTO testRoom = roomService.getRoomByName(rootRoom, "Test");

			if (t != null) {
				System.out.println(username + " wartet");
				t.join();
			} else {
				System.out.println(username + " schläft");
				Thread.sleep(1000);
			}

			System.out.println(username + " schreibt (aktueller Name des Testraums: "
					+ roomService.getName(testRoom) + ")");
			roomService.rename(testRoom, username);

			loCon.disconnect();
			return null;
		}
		
	}

	@Test
	public void lostUpdateTest() throws Exception {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		loWaCon.defaultConnectAndLogin();

		TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
		WasabiRoomDTO rootRoom = testhelper.initWorkspace("default");
		testhelper.initDatabase();

		RoomServiceLocal roomService = (RoomServiceLocal) loWaCon.lookup("RoomService");
		UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

		roomService.create("Test", rootRoom);
		userService.create("user1", "user1");
		userService.create("user2", "user2");

		loWaCon.disconnect();

		Thread t1 = new TestThread(null, "user1");
		Thread t2 = new TestThread(t1, "user2");

		t1.start();
		t2.start();

		t2.join();
		System.out.println("Beide fertig");

		loWaCon.defaultConnectAndLogin();
		roomService = (RoomServiceLocal) loWaCon.lookup("RoomService");
		for (WasabiRoomDTO aRoom : roomService.getRooms(rootRoom)) {
			System.out.println(roomService.getName(aRoom));
		}
		loWaCon.disconnect();
	}
}
