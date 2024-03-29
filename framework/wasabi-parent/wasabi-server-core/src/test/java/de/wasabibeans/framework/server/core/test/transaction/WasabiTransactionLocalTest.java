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
import java.util.concurrent.Callable;

import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;

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
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.IN_CONTAINER)
public class WasabiTransactionLocalTest extends Arquillian {

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
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	/** BASIC CLASSES NEEDED FOR THE TRANSACTION TESTS **/
	class UserTestThread extends Thread {
		private TestCallable userAction;
		private Vector<Throwable> throwables;

		public UserTestThread() {

		}

		public void setUserAction(TestCallable userAction) {
			this.userAction = userAction;
		}

		public void setThrowables(Vector<Throwable> throwables) {
			this.throwables = throwables;
		}

		@Override
		public void run() {
			try {
				System.out.println(userAction.getUsername() + " has client-thread-id " + this.getId());
				InitialContext jndi = new InitialContext();
				TestHelperLocal testhelper = (TestHelperLocal) jndi.lookup("test/TestHelper/local");
				jndi.close();
				testhelper.call(userAction);
				System.out.println(userAction.getUsername() + " has committed");
			} catch (Throwable t) {
				throwables.add(t);
			}
		}
	}

	public Vector<Throwable> executeUserThreads(UserTestThread user1, UserTestThread user2, TestCallable userAction1,
			TestCallable userAction2) throws Throwable {
		Vector<Throwable> throwables = new Vector<Throwable>();
		user1.setThrowables(throwables);
		user2.setThrowables(throwables);

		user1.setUserAction(userAction1);
		user2.setUserAction(userAction2);

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

	abstract class TestCallable implements Callable<Object> {

		protected String username;
		protected Thread otherUser;

		public TestCallable(String username, Thread otherUser) {
			this.username = username;
			this.otherUser = otherUser;
		}

		public String getUsername() {
			return this.username;
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
		public Object call() throws Exception {
			System.out.println(username + " has stateless-bean-thread-id " + Thread.currentThread().getId());
			return null;
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** DIRTY READ -------------------------------------------------------------------
	// Is it possible to read data of other transactions that do not have committed yet?
	// Jackrabbit: NOT POSSIBLE
	class DirtyReadPreventedCallable extends TestCallable {

		public DirtyReadPreventedCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==DIRTY READ==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
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

				return null;
			} finally {
				loCon.disconnect();
			}
		}

	}

	@Test
	public void dirtyReadPreventedTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			loWaCon.disconnect();

			UserTestThread user1 = new UserTestThread();
			UserTestThread user2 = new UserTestThread();
			TestCallable userAction1 = new DirtyReadPreventedCallable(USER1, user2);
			TestCallable userAction2 = new DirtyReadPreventedCallable(USER2, user1);

			Vector<Throwable> throwables = executeUserThreads(user1, user2, userAction1, userAction2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			loWaCon.defaultConnectAndLogin();
			userService = (UserServiceLocal) loWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
		} finally {
			loWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Non-Repeatable Read -------------------------------------------------------------------
	// An non-repeatable read occurs when a transaction reads data from the persistent store, but gets a different
	// result if it tries to read the same data again within the same transaction.
	// Jackrabbit: POSSIBLE
	class NonRepeatableReadCallable extends TestCallable {

		public NonRepeatableReadCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==NON REPEATABLE READ==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user1 reads, tx user2 writes, tx user2 commits, tx user1 reads
				if (username.equals(USER1)) {
					System.out.println(username + " reads");
					AssertJUnit.assertEquals(USER3, userService.getDisplayName(user3).getValue());

					notifyOther();
					waitForCommitOfOther();

					System.out.println(username + " reads again");
					userService.getDisplayName(user3).getValue();
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				} else {
					waitForMyTurn();
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				}
				return null;
			} finally {
				loCon.disconnect();
			}
		}

	}

	@Test
	public void nonRepeatableReadTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			loWaCon.disconnect();

			UserTestThread user1 = new UserTestThread();
			UserTestThread user2 = new UserTestThread();
			NonRepeatableReadCallable userAction1 = new NonRepeatableReadCallable(USER1, user2);
			NonRepeatableReadCallable userAction2 = new NonRepeatableReadCallable(USER2, user1);

			Vector<Throwable> throwables = executeUserThreads(user1, user2, userAction1, userAction2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			loWaCon.defaultConnectAndLogin();
			userService = (UserServiceLocal) loWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
		} finally {
			loWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Lost Update -------------------------------------------------------------------
	// Two transactions make data changes based upon a previously read state of the data. The problem is that while
	// making the data change one transaction is not aware of the data changes the other transaction makes.
	// Jackrabbit: POSSIBLE, though cases like tx1-write, tx2-write, tx1-commit, tx2-commit lead to a rollback of tx2
	class LostUpdateRollbackCallable extends TestCallable {

		public LostUpdateRollbackCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==LOST UPDATE ROLLBACK==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
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
					userService.create(USER4, USER4);
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());

					notifyOther();
					waitForCommitOfOther();
				}
				return null;
			} finally {
				loCon.disconnect();
			}
		}

	}

	@Test
	public void lostUpdateRollbackTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, USER1);
			userService.create(USER2, USER2);
			WasabiUserDTO user3 = userService.create(USER3, USER3);

			loWaCon.disconnect();

			UserTestThread user1 = new UserTestThread();
			UserTestThread user2 = new UserTestThread();
			LostUpdateRollbackCallable userAction1 = new LostUpdateRollbackCallable(USER1, user2);
			LostUpdateRollbackCallable userAction2 = new LostUpdateRollbackCallable(USER2, user1);

			boolean rolledBack = false;
			for (Throwable t : executeUserThreads(user1, user2, userAction1, userAction2)) {
				if (t instanceof EJBTransactionRolledbackException) {
					rolledBack = true;
				} else {
					throw t;
				}
			}
			AssertJUnit.assertTrue(rolledBack);

			System.out.println("Both user transactions have committed.");

			loWaCon.defaultConnectAndLogin();
			userService = (UserServiceLocal) loWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
			AssertJUnit.assertNull(userService.getUserByName(USER4));
			userService.create(USER4, USER4); // must work if user exists neither in the repository nor in the database
		} finally {
			loWaCon.disconnect();
		}
	}

	// ---------------------------------------------------------------------------------------------------

	class LostUpdateRollbackDueToOptLockIdCallable extends TestCallable {

		public LostUpdateRollbackDueToOptLockIdCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==LOST UPDATE ROLLBACK DUE TO OPTLOCKID==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
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
				return null;
			} finally {
				loCon.disconnect();
			}
		}
	}

	@Test
	public void lostUpdateRollbackDueToOptLockIdTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, "user1");
			userService.create(USER2, "user2");
			WasabiUserDTO user3 = userService.create(USER3, "user3");

			loWaCon.disconnect();

			UserTestThread user1 = new UserTestThread();
			UserTestThread user2 = new UserTestThread();
			LostUpdateRollbackDueToOptLockIdCallable userAction1 = new LostUpdateRollbackDueToOptLockIdCallable(USER1,
					user2);
			LostUpdateRollbackDueToOptLockIdCallable userAction2 = new LostUpdateRollbackDueToOptLockIdCallable(USER2,
					user1);

			boolean problemRecognized = false;
			for (Throwable t : executeUserThreads(user1, user2, userAction1, userAction2)) {
				if (t instanceof ConcurrentModificationException) {
					problemRecognized = true;
				} else {
					throw t;
				}
			}
			AssertJUnit.assertTrue(problemRecognized);
			System.out.println("Both user transactions have committed.");

			loWaCon.defaultConnectAndLogin();
			userService = (UserServiceLocal) loWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());

			AssertJUnit.assertNull(userService.getUserByName(USER4));
			userService.create(USER4, USER4); // must work if user exists neither in the repository nor in the database
		} finally {
			loWaCon.disconnect();
		}
	}

	// -------------------------------------------------------------------------------------------------

	class LostUpdateNotPreventedCallable extends TestCallable {

		public LostUpdateNotPreventedCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==LOST UPDATE NOT PREVENTED==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
				WasabiUserDTO user3 = userService.getUserByName(USER3);

				// tx user2 reads, tx user1 writes, tx user1 commits, tx user2 writes, tx user2 commits
				if (username.equals(USER1)) {
					waitForMyTurn();

					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER1, null);
					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
				} else {
					System.out.println(username + " reads");
					userService.getDisplayName(user3).getValue();
					notifyOther();
					waitForCommitOfOther();

					AssertJUnit.assertEquals(USER1, userService.getDisplayName(user3).getValue());
					System.out.println(username + " writes");
					userService.setDisplayName(user3, USER2, null);
					AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
				}

				return null;
			} finally {
				loCon.disconnect();
			}
		}

	}

	@Test
	public void lostUpdateNotPreventedTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, "user1");
			userService.create(USER2, "user2");
			WasabiUserDTO user3 = userService.create(USER3, "user3");

			loWaCon.disconnect();

			UserTestThread user1 = new UserTestThread();
			UserTestThread user2 = new UserTestThread();
			LostUpdateNotPreventedCallable userAction1 = new LostUpdateNotPreventedCallable(USER1, user2);
			LostUpdateNotPreventedCallable userAction2 = new LostUpdateNotPreventedCallable(USER2, user1);

			Vector<Throwable> throwables = executeUserThreads(user1, user2, userAction1, userAction2);
			if (!throwables.isEmpty()) {
				throw throwables.get(0);
			}
			System.out.println("Both user transactions have committed.");

			loWaCon.defaultConnectAndLogin();
			userService = (UserServiceLocal) loWaCon.lookup("UserService");
			AssertJUnit.assertEquals(USER2, userService.getDisplayName(user3).getValue());
		} finally {
			loWaCon.disconnect();
		}
	}

	// --------------------------------------------------------------------------------------------

	// ** Rollback -------------------------------------------------------------------
	// Successfully create a user within a transaction which rolls back afterwards -> the user must neither exist in the
	// database nor in the repository
	class UserCreateRollbackCallable extends TestCallable {

		public UserCreateRollbackCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			LocalWasabiConnector loCon = new LocalWasabiConnector();
			try {
				System.out.println("==USER CREATE ROLLBACK==");
				super.call();
				// authentication
				System.out.println(username + " authenticates");
				loCon.connect();
				loCon.login(username, username);

				UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");

				userService.create(USER2, USER2);
				userService.create(USER2, USER2); // provoke exception and failure of transaction

				return null;
			} finally {
				loCon.disconnect();
			}
		}

	}

	@Test
	public void userCreateRollbackTest() throws Throwable {
		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
		try {
			loWaCon.connect();

			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			loWaCon.defaultLogin();
			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");

			userService.create(USER1, USER1);

			loWaCon.disconnect();

			Vector<Throwable> throwables = new Vector<Throwable>();
			UserTestThread user1 = new UserTestThread();
			user1.setThrowables(throwables);
			TestCallable userAction1 = new UserCreateRollbackCallable(USER1, null);
			user1.setUserAction(userAction1);

			user1.start();
			user1.join();

			boolean rolledBack = false;
			for (Throwable t : throwables) {
				if (t instanceof ObjectAlreadyExistsException) {
					rolledBack = true;
				} else {
					throw t;
				}
			}
			AssertJUnit.assertTrue(rolledBack);

			loWaCon.defaultConnectAndLogin();
			AssertJUnit.assertNull(userService.getUserByName(USER2));
			userService.create(USER2, USER2); // would fail if previous rollback did not happen both in the database and
			// the repository
		} finally {
			loWaCon.disconnect();
		}
	}
	// --------------------------------------------------------------------------------------------
}
