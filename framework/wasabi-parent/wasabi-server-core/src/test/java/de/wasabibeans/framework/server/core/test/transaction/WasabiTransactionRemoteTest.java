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

import java.util.Vector;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
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
public class WasabiTransactionRemoteTest extends Arquillian {

	private static final String USER1 = "user1", USER2 = "user2", USER3 = "user3";

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
		protected Vector<Throwable> throwables;

		public UserThread(String username, Vector<Throwable> throwables) {
			super();
			this.username = username;
			this.throwables = throwables;
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

	// ** DIRTY READ -------------------------------------------------------------------
	// Is it possible to read data of other transactions that do not have committed yet?
	// Jackrabbit: NOT POSSIBLE
	class DirtyReadPreventedThread extends UserThread {

		public DirtyReadPreventedThread(String username, Vector<Throwable> throwables) {
			super(username, throwables);
		}

		@Override
		public void run() {
			try {
				System.out.println("==DIRTY READ==");
				// authentication
				System.out.println(username + " authenticates");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 writes, tx user2 reads, tx user1 and tx user2 commit
				if (username.equals(USER1)) {
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));

					notifyOther();
					waitForCommitOfOther();
				} else {
					waitForMyTurn();
					System.out.println(username + " reads");
					AssertJUnit.assertEquals(USER3, userService.getDisplayName(user3));
				}

				utx.commit();
				reCon.disconnect();
			} catch (Throwable t) {
				throwables.add(t);
			}
		}

	}

	@Test
	public void dirtyReadPreventedTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		userService.create(USER1, USER1);
		userService.create(USER2, USER2);
		WasabiUserDTO user3 = userService.create(USER3, USER3);

		reWaCon.disconnect();

		Vector<Throwable> throwables = new Vector<Throwable>();
		UserThread user1 = new DirtyReadPreventedThread(USER1, throwables);
		UserThread user2 = new DirtyReadPreventedThread(USER2, throwables);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!throwables.isEmpty()) {
			throw throwables.get(0);
		}
		System.out.println("Both user transactions have committed.");

		reWaCon.defaultConnectAndLogin();
		userService = (UserServiceRemote) reWaCon.lookup("UserService");
		AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));
		reWaCon.disconnect();
	}

	// --------------------------------------------------------------------------------------------

	// ** Non-Repeatable Read -------------------------------------------------------------------
	// An non-repeatable read occurs when a transaction reads data from the persistent store, but gets a different
	// result if it tries to read the same data again within the same transaction.
	// Jackrabbit: POSSIBLE
	class NonRepeatableReadThread extends UserThread {

		public NonRepeatableReadThread(String username, Vector<Throwable> throwables) {
			super(username, throwables);
		}

		@Override
		public void run() {
			try {
				System.out.println("==NON REPEATABLE READ==");
				// authentication
				System.out.println(username + " authenticates");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 reads, tx user2 writes, tx user2 commits, tx user1 reads
				if (username.equals(USER1)) {
					System.out.println(username + " reads");
					userService.getDisplayName(user3);
					AssertJUnit.assertEquals(USER3, userService.getDisplayName(user3));

					notifyOther();
					waitForCommitOfOther();

					System.out.println(username + " reads again");
					userService.getDisplayName(user3);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));
				} else {
					waitForMyTurn();
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));
				}

				utx.commit();
				reCon.disconnect();
			} catch (Throwable t) {
				throwables.add(t);
			}
		}

	}

	@Test
	public void nonRepeatableReadTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		userService.create(USER1, USER1);
		userService.create(USER2, USER2);
		WasabiUserDTO user3 = userService.create(USER3, USER3);

		reWaCon.disconnect();

		Vector<Throwable> throwables = new Vector<Throwable>();
		UserThread user1 = new NonRepeatableReadThread(USER1, throwables);
		UserThread user2 = new NonRepeatableReadThread(USER2, throwables);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!throwables.isEmpty()) {
			throw throwables.get(0);
		}
		System.out.println("Both user transactions have committed.");

		reWaCon.defaultConnectAndLogin();
		userService = (UserServiceRemote) reWaCon.lookup("UserService");
		AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));
		reWaCon.disconnect();
	}

	// --------------------------------------------------------------------------------------------

	// ** Lost Update -------------------------------------------------------------------
	// Two transactions make data changes based upon a previously read state of the data. The problem is that while
	// making the data change one transaction is not aware of the data changes the other transaction makes.
	// Jackrabbit: POSSIBLE, though cases like tx1-write, tx2-write, tx1-commit, tx2-commit lead to a rollback of tx2
	class LostUpdateRollbackThread extends UserThread {

		public LostUpdateRollbackThread(String username, Vector<Throwable> throwables) {
			super(username, throwables);
		}

		@Override
		public void run() {
			try {
				System.out.println("==LOST UPDATE ROLLBACK==");
				// authentication
				System.out.println(username + " authenticates");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 writes, tx user2 writes, tx user1 commits, tx user2 commits
				if (username.equals(USER1)) {
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));
					notifyOther();
					waitForMyTurn();
				} else {
					waitForMyTurn();
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));

					notifyOther();
					waitForCommitOfOther();
				}

				utx.commit();
				reCon.disconnect();
			} catch (Throwable t) {
				throwables.add(t);
			}
		}

	}

	@Test
	public void lostUpdateRollbackTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		userService.create(USER1, USER1);
		userService.create(USER2, USER2);
		WasabiUserDTO user3 = userService.create(USER3, USER3);

		reWaCon.disconnect();

		Vector<Throwable> throwables = new Vector<Throwable>();
		UserThread user1 = new LostUpdateRollbackThread(USER1, throwables);
		UserThread user2 = new LostUpdateRollbackThread(USER2, throwables);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();

		boolean rolledBack = false;
		for (Throwable t : throwables) {
			if (t instanceof RollbackException) {
				rolledBack = true;
			} else {
				throw t;
			}
		}
		AssertJUnit.assertTrue(rolledBack);

		System.out.println("Both user transactions have committed.");

		reWaCon.defaultConnectAndLogin();
		userService = (UserServiceRemote) reWaCon.lookup("UserService");
		AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));
		reWaCon.disconnect();
	}

	class LostUpdateNotPreventedThread extends UserThread {

		public LostUpdateNotPreventedThread(String username, Vector<Throwable> throwables) {
			super(username, throwables);
		}

		@Override
		public void run() {
			try {
				System.out.println("==LOST UPDATE NOT PREVENTED==");
				// authentication
				System.out.println(username + " authenticates");
				RemoteWasabiConnector reCon = new RemoteWasabiConnector();
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user2 reads, tx user1 writes, tx user1 commits, tx user2 writes, tx user2 commits
				if (username.equals(USER1)) {
					waitForMyTurn();
					
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));
				} else {
					System.out.println(username + " reads");
					userService.getDisplayName(user3);
					notifyOther();
					waitForCommitOfOther();
					
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3));
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));
				}

				utx.commit();
				reCon.disconnect();
			} catch (Throwable t) {
				throwables.add(t);
			}
		}

	}

	@Test
	public void lostUpdateNotPreventedTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		userService.create(USER1, "user1");
		userService.create(USER2, "user2");
		WasabiUserDTO user3 = userService.create(USER3, "user3");

		reWaCon.disconnect();

		Vector<Throwable> throwables = new Vector<Throwable>();
		UserThread user1 = new LostUpdateNotPreventedThread(USER1, throwables);
		UserThread user2 = new LostUpdateNotPreventedThread(USER2, throwables);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!throwables.isEmpty()) {
			throw throwables.get(0);
		}
		System.out.println("Both user transactions have committed.");

		reWaCon.defaultConnectAndLogin();
		userService = (UserServiceRemote) reWaCon.lookup("UserService");
		AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3));
		reWaCon.disconnect();
	}

	// --------------------------------------------------------------------------------------------

	// ** Rollback -------------------------------------------------------------------
	// Successfully create a user within a transaction which rolls back afterwards -> the user must neither exist in the
	// database nor in the repository
	@Test
	public void userCreateRollbackTest() throws Exception {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

		UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");

		try {
			utx.begin();

			userService.create(USER1, USER1);
			userService.create(null, null); // provoke exception and failure of transaction

			utx.commit();
		} catch (EJBTransactionRolledbackException rb) {
			utx.rollback();
		}

		AssertJUnit.assertNull(userService.getUserByName(USER1));

		userService.create(USER1, USER1); // would fail if previous rollback did not happen both in the database and the
											// repository

		reWaCon.disconnect();
	}
	// --------------------------------------------------------------------------------------------
}
