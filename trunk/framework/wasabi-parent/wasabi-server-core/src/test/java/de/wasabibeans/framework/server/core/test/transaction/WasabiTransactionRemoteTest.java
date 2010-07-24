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

import java.util.concurrent.Executors;

import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.omg.CosTransactions.Status;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class WasabiTransactionRemoteTest extends Arquillian {

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
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(RoomServiceImpl.class.getPackage()); // internal

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
				System.out.println(username + " meldet sich beim Server an");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookup("UserTransaction");
				utx.begin();
				System.out.println(username + " hat Transaction " + utx.toString());
				
				System.out.println(username + " liest");
				RoomServiceRemote roomService = (RoomServiceRemote) reCon.lookup("RoomService/remote");
				WasabiRoomDTO rootRoom = roomService.getRootRoom();
				WasabiRoomDTO testRoom = roomService.getRoomByName(rootRoom, "Test");

				if (t != null) {
					System.out.println(username + " wartet");
					t.join();
				} else {
					System.out.println(username + " schläft");
					Thread.sleep(1000);
				}

				System.out.println(username + " schreibt (aktueller Name des Testraums: " + roomService.getName(testRoom) + ")");
				System.out.println("Status der laufenen Transaktion (aktiv = " + Status._StatusActive + "): " + utx.getStatus());
				roomService.rename(testRoom, username);
				
				utx.commit();
				
				reCon.disconnect();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void lostUpdateTest() throws Exception {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper/remote");
		WasabiRoomDTO rootRoom = testhelper.initWorkspace("default");
		testhelper.initDatabase();
		
		RoomServiceRemote roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService/remote");
		
		roomService.create("Test", rootRoom);
		userService.create("user1", "user1");
		userService.create("user2", "user2");
		
		reWaCon.disconnect();
		
		Thread t1 = new TestThread(null, "user1");
		Thread t2 = new TestThread(t1, "user2");

		t1.start();
		t2.start();

		t2.join();
		System.out.println("Beide fertig");
		
		reWaCon.defaultConnectAndLogin();
		roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
		for (WasabiRoomDTO aRoom : roomService.getRooms(rootRoom)) {
			System.out.println(roomService.getName(aRoom));
		}
		reWaCon.disconnect();
	}

}
