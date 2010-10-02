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

import java.util.HashMap;
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

import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class WasabiTransactionRemoteTest extends Arquillian {

	private static final String USER1 = "user1", USER2 = "user2", USER3 = "user3", USER4 = "user4";

	private static HashMap<String, Boolean> tickets;

	static {
		tickets = new HashMap<String, Boolean>();
		tickets.put(USER1, false);
		tickets.put(USER2, false);
	}

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
				.addPackage(WasabiUserACL.class.getPackage()) // authorization
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(WasabiException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(Locker.class.getPackage()) // locking
				.addPackage(WasabiEventType.class.getPackage()) // event
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(WasabiAOP.class.getPackage()) // AOP
				.addPackage(Filter.class.getPackage()) // pipes.filter
				.addPackage(FilterField.class.getPackage()) // pipes.filter.annotation
				.addPackage(DocumentSource.class.getPackage()) // pipes.filter.impl
				.addPackage(AuthTokenDelegate.class.getPackage()) // pipes.auth
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

		public UserThread(String username) {
			super();
			this.username = username;
		}

		public void setThrowables(Vector<Throwable> throwables) {
			this.throwables = throwables;
		}

		public void setOtherUser(Thread otherUser) {
			this.otherUser = otherUser;
		}

		protected void waitForMyTurn() throws InterruptedException {
			synchronized (tickets) {
				while (!tickets.get(username)) {
					tickets.wait();
				}
				tickets.put(username, false);
			}
		}

		protected void notifyOther() throws Exception {
			String otherUser;
			if (username.equals(USER1)) {
				otherUser = USER2;
			} else {
				otherUser = USER1;
			}
			synchronized (tickets) {
				tickets.put(otherUser, true);
				tickets.notify();
			}
		}

		protected void waitForCommitOfOther() throws InterruptedException {
			otherUser.join();
		}

		@Override
		public abstract void run();
	}

	public Vector<Throwable> executeUserThreads(UserThread user1, UserThread user2) throws Throwable {
		Vector<Throwable> throwables = new Vector<Throwable>();
		user1.setThrowables(throwables);
		user2.setThrowables(throwables);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join(5000);
		user2.join(5000);
		if (user1.isAlive() || user2.isAlive()) {
			user1.interrupt();
			user2.interrupt();
			AssertJUnit.fail();
		}
		return throwables;
	}

	// --------------------------------------------------------------------------------------------

	// ** DIRTY READ -------------------------------------------------------------------
	// Is it possible to read data of other transactions that do not have committed yet?
	// Jackrabbit: NOT POSSIBLE
	class DirtyReadPreventedThread extends UserThread {

		public DirtyReadPreventedThread(String username) {
			super(username);
		}

		@Override
		public void run() {
			RemoteWasabiConnector reCon = new RemoteWasabiConnector();
			try {
				System.out.println("==DIRTY READ==");
				// authentication
				System.out.println(username + " authenticates");
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 writes, tx user2 reads, tx user1 and tx user2 commit
				if (username.equals(USER1)) {
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1, null);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());

					notifyOther();
					waitForCommitOfOther();
				} else {
					waitForMyTurn();
					System.out.println(username + " reads");
					AssertJUnit.assertEquals(USER3, userService.getDisplayName(user3).getValue());
				}

				utx.commit();
			} catch (Throwable t) {
				throwables.add(t);
			} finally {
				reCon.disconnect();
			}
		}

	}

	@Test
	public void dirtyReadPreventedTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			reWaCon.disconnect();

			UserThread user1 = new DirtyReadPreventedThread(USER1);
			UserThread user2 = new DirtyReadPreventedThread(USER2);

			Vector<Throwable> throwables = executeUserThreads(user1, user2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			reWaCon.defaultConnectAndLogin();
			userService = (UserServiceRemote) reWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
		} finally {
			reWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Non-Repeatable Read -------------------------------------------------------------------
	// An non-repeatable read occurs when a transaction reads data from the persistent store, but gets a different
	// result if it tries to read the same data again within the same transaction.
	// Jackrabbit: POSSIBLE
	class NonRepeatableReadThread extends UserThread {

		public NonRepeatableReadThread(String username) {
			super(username);
		}

		@Override
		public void run() {
			RemoteWasabiConnector reCon = new RemoteWasabiConnector();
			try {
				System.out.println("==NON REPEATABLE READ==");
				// authentication
				System.out.println(username + " authenticates");
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
					AssertJUnit.assertEquals(USER3, userService.getDisplayName(user3).getValue());

					notifyOther();
					waitForCommitOfOther();

					System.out.println(username + " reads again");
					userService.getDisplayName(user3);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				} else {
					waitForMyTurn();
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				}

				utx.commit();
			} catch (Throwable t) {
				throwables.add(t);
			} finally {
				reCon.disconnect();
			}
		}

	}

	@Test
	public void nonRepeatableReadTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			reWaCon.disconnect();

			UserThread user1 = new NonRepeatableReadThread(USER1);
			UserThread user2 = new NonRepeatableReadThread(USER2);

			Vector<Throwable> throwables = executeUserThreads(user1, user2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			reWaCon.defaultConnectAndLogin();
			userService = (UserServiceRemote) reWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
		} finally {
			reWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Lost Update -------------------------------------------------------------------
	// Two transactions make data changes based upon a previously read state of the data. The problem is that while
	// making the data change one transaction is not aware of the data changes the other transaction makes.
	// Jackrabbit: POSSIBLE, though cases like tx1-write, tx2-write, tx1-commit, tx2-commit lead to a rollback of tx2
	class LostUpdateRollbackThread extends UserThread {

		public LostUpdateRollbackThread(String username) {
			super(username);
		}

		@Override
		public void run() {
			RemoteWasabiConnector reCon = new RemoteWasabiConnector();
			try {
				System.out.println("==LOST UPDATE ROLLBACK==");
				// authentication
				System.out.println(username + " authenticates");
				reCon.connect();
				reCon.login(username, username);

				UserTransaction utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 writes, tx user2 writes, tx user1 commits, tx user2 commits
				if (username.equals(USER1)) {
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1, null);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
					notifyOther();
					waitForMyTurn();
				} else {
					waitForMyTurn();
					// create another user in order to test later on whether transaction really rolls back
					System.out.println(username + " makes user4");
					userService.create(USER4, USER4);
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());

					notifyOther();
					waitForCommitOfOther();
				}

				utx.commit();
			} catch (Throwable t) {
				t.printStackTrace();
				throwables.add(t);
			} finally {
				reCon.disconnect();
			}
		}

	}

	@Test
	public void lostUpdateRollbackTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			reWaCon.disconnect();

			UserThread user1 = new LostUpdateRollbackThread(USER1);
			UserThread user2 = new LostUpdateRollbackThread(USER2);

			boolean rolledBack = false;
			for (Throwable t : executeUserThreads(user1, user2)) {
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
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
			AssertJUnit.assertNull(userService.getUserByName(USER4));
			userService.create(USER4, USER4); // must work if user exists neither in the repository nor in the database
		} finally {
			reWaCon.disconnect();
		}
	}

	// -------------------------------------------------------------------------------------------------

	class LostUpdateRollbackDueToOptLockIdThread extends UserThread {

		public LostUpdateRollbackDueToOptLockIdThread(String username) {
			super(username);
		}

		@Override
		public void run() {
			RemoteWasabiConnector reCon = new RemoteWasabiConnector();
			UserTransaction utx = null;
			try {
				System.out.println("==LOST UPDATE ROLLBACK DUE TO OPTLOCKID==");
				// authentication
				System.out.println(username + " authenticates");
				reCon.connect();
				reCon.login(username, username);

				utx = (UserTransaction) reCon.lookupGeneral("UserTransaction");
				utx.begin();

				UserServiceRemote userService = (UserServiceRemote) reCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user2 reads, tx user1 writes, tx user1 commits, tx user2 writes, tx user2 commits
				if (username.equals(USER1)) {
					waitForMyTurn();

					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1, null);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
				} else {
					System.out.println(username + " reads");
					WasabiValueDTO displayNameDTO = userService.getDisplayName(user3);
					// create another user in order to test later on whether transaction really rolls back
					userService.create(USER4, USER4);
					notifyOther();
					waitForCommitOfOther();

					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, displayNameDTO.getOptLockId());
				}

				utx.commit();
			} catch (Throwable t) {
				try {
					utx.rollback();
				} catch (Exception e) {
					throwables.add(e);
				}
				throwables.add(t);
			} finally {
				reCon.disconnect();
			}
		}
	}

	@Test
	public void lostUpdateRollbackDueToOptLockIdTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			reWaCon.disconnect();

			UserThread user1 = new LostUpdateRollbackDueToOptLockIdThread(USER1);
			UserThread user2 = new LostUpdateRollbackDueToOptLockIdThread(USER2);

			boolean problemRecognized = false;
			for (Throwable t : executeUserThreads(user1, user2)) {
				if (t instanceof ConcurrentModificationException) {
					problemRecognized = true;
				} else {
					throw t;
				}
			}
			AssertJUnit.assertTrue(problemRecognized);

			System.out.println("Both user transactions have committed.");

			reWaCon.defaultConnectAndLogin();
			userService = (UserServiceRemote) reWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());

			AssertJUnit.assertNull(userService.getUserByName(USER4));
			userService.create(USER4, USER4); // must work if user exists neither in the repository nor in the database
		} finally {
			reWaCon.disconnect();
		}
	}

	// -------------------------------------------------------------------------------------------

	class LostUpdateNotPreventedThread extends UserThread {

		public LostUpdateNotPreventedThread(String username) {
			super(username);
		}

		@Override
		public void run() {
			RemoteWasabiConnector reCon = new RemoteWasabiConnector();
			try {
				System.out.println("==LOST UPDATE NOT PREVENTED==");
				// authentication
				System.out.println(username + " authenticates");
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
					userService.setDisplayName(user3, USER1, null);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
				} else {
					System.out.println(username + " reads");
					userService.getDisplayName(user3);
					notifyOther();
					waitForCommitOfOther();

					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				}

				utx.commit();
			} catch (Throwable t) {
				throwables.add(t);
			} finally {
				reCon.disconnect();
			}
		}

	}

	@Test
	public void lostUpdateNotPreventedTest() throws Throwable {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			userService.create(USER1, "user1");
			userService.create(USER2, "user2");
			WasabiUserDTO user3 = userService.create(USER3, "user3");

			reWaCon.disconnect();

			UserThread user1 = new LostUpdateNotPreventedThread(USER1);
			UserThread user2 = new LostUpdateNotPreventedThread(USER2);

			Vector<Throwable> throwables = executeUserThreads(user1, user2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			reWaCon.defaultConnectAndLogin();
			userService = (UserServiceRemote) reWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
		} finally {
			reWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Rollback -------------------------------------------------------------------
	// Successfully create a user within a transaction which rolls back afterwards -> the user must neither exist in the
	// database nor in the repository
	@Test
	public void userCreateRollbackTest() throws Exception {
		RemoteWasabiConnector reWaCon = new RemoteWasabiConnector();
		try {
			reWaCon.connect();

			TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			reWaCon.defaultLogin();
			UserServiceRemote userService = (UserServiceRemote) reWaCon.lookup("UserService");

			UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");

			try {
				utx.begin();

				userService.create(USER1, USER1);
				userService.create(null, null); // provoke exception and failure of transaction

				utx.commit();
			} catch (EJBTransactionRolledbackException rb) {
				utx.rollback();
			} catch (WasabiException we) {
				utx.rollback();
			}

			AssertJUnit.assertNull(userService.getUserByName(USER1));

			userService.create(USER1, USER1); // would fail if previous rollback did not happen both in the database and
			// the repository

		} finally {
			reWaCon.disconnect();
		}
	}
	// --------------------------------------------------------------------------------------------
}
