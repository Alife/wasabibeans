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
package de.wasabibeans.framework.server.core.test.locking;

import java.util.HashMap;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.transaction.TransactionManager;
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
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.DummyDTO;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Run(RunModeType.IN_CONTAINER)
public class LockingLocalTest extends Arquillian {

	private static final String USER1 = "user1", USER2 = "user2";
	private static final String DOC1 = "document1";

	private static HashMap<String, Boolean> tickets;

	static {
		tickets = new HashMap<String, Boolean>();
		tickets.put(USER1, false);
		tickets.put(USER2, false);
	}

	private DummyDTO dto;

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
				.addPackage(DebugInterceptor.class.getPackage()) // debug
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	// @BeforeMethod annotation does not seem to work for IN_CONTAINER tests in arquillian version 1.0.0.Alpha2
	public void setUpBeforeEachMethod() throws Exception {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			TestHelperLocal testhelper = (TestHelperLocal) jndi.lookupLocal("test/TestHelper");
			testhelper.initDatabase();
			testhelper.initRepository();

			Session s = jcr.getJCRSession();
			Node document = s.getRootNode().addNode(DOC1, WasabiNodeType.DOCUMENT);
			s.save();

			dto = new DummyDTO(document.getIdentifier());
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
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

	// ------------------------------------------------------------------------------------------
	// tests the case when one user tries to lock an already locked node
	class AlreadyLocked extends UserThread {

		public AlreadyLocked(String username) {
			super(username);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				utx = (UserTransaction) jndi.lookup("UserTransaction");
				utx.begin();

				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("test/LockingHelper");
				TransactionManager tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
				Session s = jcr.getJCRSession();

				if (username.equals(USER2)) {
					waitForMyTurn();
				}

				String lockToken = Locker.acquireServiceCallLock(dto, -1L, locker, tm);
				Locker.recognizeLockToken(s, lockToken);

				if (username.equals(USER1)) {
					notifyOther();
					waitForCommitOfOther();
				}

				jcr.cleanup(false);
				utx.commit();
			} catch (Throwable t) {
				jcr.cleanup(false);
				System.out.println(username + "throws exception");
				if (t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				jndi.close();
			}
		}
	}

	@Test
	public void AlreadyLockedTest() throws Throwable {
		setUpBeforeEachMethod();
		UserThread user1 = new AlreadyLocked(USER1);
		UserThread user2 = new AlreadyLocked(USER2);

		Vector<Throwable> throwables = executeUserThreads(user1, user2);
		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		AssertJUnit.assertEquals(1, throwables.size());
		AssertJUnit.assertTrue(throwables.get(0) instanceof ConcurrentModificationException);
	}

	// -------------------------------------------------------------------------------------------------------
	// tests whether two users can subsequently acquire the lock
	class SubSequentAcquire extends UserThread {

		public SubSequentAcquire(String username) {
			super(username);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				utx = (UserTransaction) jndi.lookup("UserTransaction");
				utx.begin();

				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("test/LockingHelper");
				TransactionManager tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
				Session s = jcr.getJCRSession();

				if (username.equals(USER2)) {
					waitForCommitOfOther();
				}

				String lockToken = Locker.acquireServiceCallLock(dto, -1L, locker, tm);
				Locker.recognizeLockToken(s, lockToken);

				jcr.cleanup(false);
				utx.commit();
			} catch (Throwable t) {
				jcr.cleanup(false);
				System.out.println(username + "throws exception");
				if (t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				jndi.close();
			}
		}
	}

	@Test
	public void SubSequentAcquireTest() throws Throwable {
		setUpBeforeEachMethod();
		UserThread user1 = new SubSequentAcquire(USER1);
		UserThread user2 = new SubSequentAcquire(USER2);

		// assert that there are no errors -> shows that after one transaction is done another one is able to acquire a
		// lock
		Vector<Throwable> throwables = executeUserThreads(user1, user2);
		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		AssertJUnit.assertTrue(throwables.isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// tests whether the lock gets released if the original lock holder encounters a problem
	class ReleasedOnRollback extends UserThread {

		public ReleasedOnRollback(String username) {
			super(username);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				utx = (UserTransaction) jndi.lookup("UserTransaction");
				utx.begin();

				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("test/LockingHelper");
				TransactionManager tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
				Session s = jcr.getJCRSession();

				if (username.equals(USER2)) {
					waitForCommitOfOther();
				}

				String lockToken = Locker.acquireServiceCallLock(dto, -1L, locker, tm);
				Locker.recognizeLockToken(s, lockToken);

				if (username.equals(USER1)) {
					throw new RuntimeException();
				}

				jcr.cleanup(false);
				utx.commit();
			} catch (Throwable t) {
				jcr.cleanup(false);
				System.out.println(username + "throws exception");
				if (t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				jndi.close();
			}
		}
	}

	@Test
	public void ReleasedOnRollbackTest() throws Throwable {
		setUpBeforeEachMethod();
		UserThread user1 = new ReleasedOnRollback(USER1);
		UserThread user2 = new ReleasedOnRollback(USER2);

		Vector<Throwable> throwables = executeUserThreads(user1, user2);
		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		AssertJUnit.assertEquals(1, throwables.size());
		AssertJUnit.assertTrue(throwables.get(0) instanceof RuntimeException);
	}

	// ------------------------------------------------------------------------------------------
	// tests the case when one user tries to write an already locked node
	class WriteAlreadyLocked extends UserThread {

		public WriteAlreadyLocked(String username) {
			super(username);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				utx = (UserTransaction) jndi.lookup("UserTransaction");
				utx.begin();

				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("test/LockingHelper");
				TransactionManager tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
				Session s = jcr.getJCRSession();

				if (username.equals(USER1)) {
					String lockToken = Locker.acquireServiceCallLock(dto, -1L, locker, tm);
					Locker.recognizeLockToken(s, lockToken);

					notifyOther();
					waitForCommitOfOther();
				} else {
					waitForMyTurn();

					// user2 does not lock, but just attempts to write -> should fail
					s.getNodeByIdentifier(dto.getId()).setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
					s.save();
				}

				jcr.cleanup(false);
				utx.commit();
			} catch (Throwable t) {
				jcr.cleanup(false);
				System.out.println(username + "throws exception");
				if (t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				jndi.close();
			}
		}
	}

	@Test
	public void WriteAlreadyLocked() throws Throwable {
		setUpBeforeEachMethod();
		UserThread user1 = new WriteAlreadyLocked(USER1);
		UserThread user2 = new WriteAlreadyLocked(USER2);

		Vector<Throwable> throwables = executeUserThreads(user1, user2);
		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		AssertJUnit.assertEquals(1, throwables.size());
		AssertJUnit.assertTrue(throwables.get(0) instanceof LockException);
	}

	// ------------------------------------------------------------------------------------------
	// IMPORTANT: shows that a delayed, non-locking transaction can 'circumvent' a lock of another transaction
	class DelayedTransactionProblem extends UserThread {

		public DelayedTransactionProblem(String username) {
			super(username);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				utx = (UserTransaction) jndi.lookup("UserTransaction");
				utx.begin();

				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("test/LockingHelper");
				TransactionManager tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
				Session s = jcr.getJCRSession();

				if (username.equals(USER1)) {
					// first let user2 save a change
					waitForMyTurn();

					// <<2>> now lock the node in question
					String lockToken = Locker.acquireServiceCallLock(dto, -1L, locker, tm);
					Locker.recognizeLockToken(s, lockToken);

					// the node is locked, let user2 try to commit his change
					notifyOther();
					waitForCommitOfOther();
				} else {
					// <<1>> user2 does not lock, but just writes
					s.getNodeByIdentifier(dto.getId()).setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
					s.save();

					// now the transaction of user2 gets delayed a bit
					notifyOther();
					waitForMyTurn();

					// <<3>> user2 commits his change although the other transaction has just locked the node
				}

				jcr.cleanup(false);
				utx.commit();
			} catch (Throwable t) {
				jcr.cleanup(false);
				System.out.println(username + "throws exception");
				if (t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				jndi.close();
			}
		}
	}

	@Test
	public void DelayedTransactionProblem() throws Throwable {
		setUpBeforeEachMethod();
		UserThread user1 = new DelayedTransactionProblem(USER1);
		UserThread user2 = new DelayedTransactionProblem(USER2);

		// show that no problems have occurred
		Vector<Throwable> throwables = executeUserThreads(user1, user2);
		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		AssertJUnit.assertTrue(throwables.isEmpty());

		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			Session s = jcr.getJCRSession();
			AssertJUnit.assertEquals(1, s.getNodeByIdentifier(dto.getId()).getProperty(WasabiNodeProperty.OPT_LOCK_ID)
					.getLong());
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}
}