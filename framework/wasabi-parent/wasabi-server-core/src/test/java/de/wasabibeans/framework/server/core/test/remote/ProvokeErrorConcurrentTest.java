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
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class ProvokeErrorConcurrentTest extends Arquillian {

	private static final String USER1 = "user1", USER2 = "user2";

	private final static Object activeThreadLock = new Object();

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
				.addPackage(TestHelper.class.getPackage()); // testhelper

		return testArchive;
	}

	abstract class UserThread extends Thread {
		protected Thread otherUser;
		protected String username;
		protected Vector<Exception> exceptions;

		public UserThread(String username, Vector<Exception> exceptions) {
			super();
			this.username = username;
			this.exceptions = exceptions;
		}

		public void setOtherUser(Thread otherUser) {
			this.otherUser = otherUser;
		}

		protected void waitForMyTurn() throws InterruptedException {
			synchronized (activeThreadLock) {
				activeThreadLock.wait();
			}
		}

		protected void notifyOther() throws Exception {
			synchronized (activeThreadLock) {
				activeThreadLock.notify();
			}
		}

		protected void waitForCommitOfOther() throws InterruptedException {
			otherUser.join();
		}

		@Override
		public abstract void run();
	}

	// --------------------------------------------------------------------------------------------

	class ProvokeError extends UserThread {

		public ProvokeError(String username, Vector<Exception> exceptions) {
			super(username, exceptions);
		}

		@Override
		public void run() {
			try {
				// authentication
				System.out.println(username + " authenticates");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				RoomServiceRemote roomService = (RoomServiceRemote) reCon.lookup("RoomService");
				WasabiRoomDTO rootRoom = roomService.getRootRoom();

				for (int i = 0; i < 20; i++) {
					System.out.println(username + " creates");
					roomService.create(username + "-" + i, rootRoom);
				}

				reCon.disconnect();
			} catch (Exception e) {
				exceptions.add(e);
			}
		}

	}

	@Test
	public void provokeErrorTest() throws Exception {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initRepository();
		testhelper.initDatabase();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		userService.create(USER1, USER1);
		userService.create(USER2, USER2);

		reWaCon.disconnect();

		Vector<Exception> exceptions = new Vector<Exception>();
		UserThread user1 = new ProvokeError(USER1, exceptions);
		UserThread user2 = new ProvokeError(USER2, exceptions);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!exceptions.isEmpty()) {
			throw exceptions.get(0);
		}
		System.out.println("Both user threads are done.");
	}
}
