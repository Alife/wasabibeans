///* 
// * Copyright (C) 2010 
// * Jonas Schulte, Dominik Klaholt, Jannis Sauer
// *
// *  This program is free software; you can redistribute it and/or modify
// *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
// *  the Free Software Foundation; either version 3 of the license, or
// *  (at your option) any later version.
// *
// *  This program is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
// *
// *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
// *  along with this program; if not, write to the Free Software
// *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// *
// *  Further information are online available at: http://www.wasabibeans.de
// */
//package de.wasabibeans.framework.server.core.test.locking;
//
//import java.util.HashMap;
//import java.util.Vector;
//
//import javax.jcr.Node;
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;
//import javax.jcr.lock.LockException;
//import javax.jcr.nodetype.NodeType;
//import javax.transaction.UserTransaction;
//
//import org.jboss.arquillian.api.Deployment;
//import org.jboss.arquillian.api.Run;
//import org.jboss.arquillian.api.RunModeType;
//import org.jboss.arquillian.testng.Arquillian;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
//import org.testng.AssertJUnit;
//import org.testng.annotations.Test;
//
//import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
//import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
//import de.wasabibeans.framework.server.core.bean.RoomService;
//import de.wasabibeans.framework.server.core.common.WasabiConstants;
//import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
//import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
//import de.wasabibeans.framework.server.core.common.WasabiNodeType;
//import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
//import de.wasabibeans.framework.server.core.event.WasabiEventType;
//import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
//import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
//import de.wasabibeans.framework.server.core.exception.WasabiException;
//import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
//import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
//import de.wasabibeans.framework.server.core.local.UserServiceLocal;
//import de.wasabibeans.framework.server.core.locking.Locker;
//import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
//import de.wasabibeans.framework.server.core.manager.WasabiManager;
//import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
//import de.wasabibeans.framework.server.core.pipes.filter.Filter;
//import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
//import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
//import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
//import de.wasabibeans.framework.server.core.test.testhelper.DummyDTO;
//import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
//import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
//import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
//import de.wasabibeans.framework.server.core.util.DebugInterceptor;
//import de.wasabibeans.framework.server.core.util.HashGenerator;
//import de.wasabibeans.framework.server.core.util.JcrConnector;
//import de.wasabibeans.framework.server.core.util.JndiConnector;
//
//@Run(RunModeType.IN_CONTAINER)
//public class LockingLocalTest extends Arquillian {
//
//	private static final String USER1 = "user1", USER2 = "user2";
//	private static final String DOC1 = "document1", CONTENT1 = "content1";
//
//	private static HashMap<String, Boolean> tickets;
//
//	static {
//		tickets = new HashMap<String, Boolean>();
//		tickets.put(USER1, false);
//		tickets.put(USER2, false);
//	}
//
//	private String docid;
//	private DummyDTO dto;
//
//	@Deployment
//	public static JavaArchive deploy() {
//		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
//		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
//				.addPackage(WasabiUserACL.class.getPackage()) // authorization
//				.addPackage(WasabiConstants.class.getPackage()) // common
//				.addPackage(WasabiException.class.getPackage()) // exception
//				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
//				.addPackage(HashGenerator.class.getPackage()) // util
//				.addPackage(Locker.class.getPackage()) // locking
//				.addPackage(WasabiEventType.class.getPackage()) // event
//				.addPackage(WasabiManager.class.getPackage()) // manager
//				.addPackage(Filter.class.getPackage()) // pipes.filter
//				.addPackage(FilterField.class.getPackage()) // pipes.filter.annotation
//				.addPackage(DocumentSource.class.getPackage()) // pipes.filter.impl
//				.addPackage(AuthTokenDelegate.class.getPackage()) // pipes.auth
//				.addPackage(RoomService.class.getPackage()) // bean impl
//				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
//				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
//				.addPackage(RoomServiceImpl.class.getPackage()) // internal
//				.addPackage(DebugInterceptor.class.getPackage()) // debug
//				.addPackage(TestHelper.class.getPackage()) // testhelper
//				.addClass(LocalWasabiConnector.class);
//
//		return testArchive;
//	}
//
//	// @BeforeMethod annotation does not seem to work for IN_CONTAINER tests in arquillian version 1.0.0.Alpha2
//	public void setUpBeforeEachMethod() throws Exception {
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//		try {
//			loWaCon.connect();
//
//			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
//			testhelper.initDatabase();
//			testhelper.initRepository();
//
//			loWaCon.defaultLogin();
//			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");
//			userService.create(USER1, USER1);
//			userService.create(USER2, USER2);
//
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getRootNode().addNode(DOC1, WasabiNodeType.DOCUMENT);
//			document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 0);
//			document.setProperty(WasabiNodeProperty.CONTENT, CONTENT1);
//			s.save();
//
//			docid = document.getIdentifier();
//			dto = new DummyDTO(docid);
//		} finally {
//			jcr.logout();
//			jndi.close();
//			loWaCon.disconnect();
//		}
//	}
//
//	abstract class UserThread extends Thread {
//		protected Thread otherUser;
//		protected String username;
//		protected Vector<Throwable> throwables;
//
//		public UserThread(String username) {
//			super();
//			this.username = username;
//		}
//
//		public void setThrowables(Vector<Throwable> throwables) {
//			this.throwables = throwables;
//		}
//
//		public void setOtherUser(Thread otherUser) {
//			this.otherUser = otherUser;
//		}
//
//		protected void waitForMyTurn() throws InterruptedException {
//			synchronized (tickets) {
//				while (!tickets.get(username)) {
//					tickets.wait();
//				}
//				tickets.put(username, false);
//			}
//		}
//
//		protected void notifyOther() throws Exception {
//			String otherUser;
//			if (username.equals(USER1)) {
//				otherUser = USER2;
//			} else {
//				otherUser = USER1;
//			}
//			synchronized (tickets) {
//				tickets.put(otherUser, true);
//				tickets.notify();
//			}
//		}
//
//		protected void waitForCommitOfOther() throws InterruptedException {
//			otherUser.join();
//		}
//
//		@Override
//		public abstract void run();
//	}
//
//	public Vector<Throwable> executeUserThreads(UserThread user1, UserThread user2) throws Throwable {
//		Vector<Throwable> throwables = new Vector<Throwable>();
//		user1.setThrowables(throwables);
//		user2.setThrowables(throwables);
//		user1.setOtherUser(user2);
//		user2.setOtherUser(user1);
//
//		user1.start();
//		user2.start();
//
//		user1.join(5000);
//		user2.join(5000);
//		if (user1.isAlive() || user2.isAlive()) {
//			user1.interrupt();
//			user2.interrupt();
//			AssertJUnit.fail();
//		}
//		return throwables;
//	}
//
//	// ------------------------------------------------------------------------------------------
//	// tests the case when one user tries to write an already locked node
//	class SetSomethingTest1 extends UserThread {
//
//		public SetSomethingTest1(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				document = s.getNodeByIdentifier(docid);
//				try {
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//					}
//
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//
//					if (username.equals(USER1)) {
//						notifyOther();
//						waitForMyTurn();
//					}
//
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (username.equals(USER2)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				t.printStackTrace();
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest1() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest1(USER1);
//		UserThread user2 = new SetSomethingTest1(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() instanceof LockException) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// ------------------------------------------------------------------------------------------
//	// tests the case when one user tries to lock a node which should already be locked because of a deep lock on an
//	// ancestor
//	class SetSomethingTest1_1 extends UserThread {
//
//		public SetSomethingTest1_1(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Session s = jcr.getJCRSessionNoTx();
//				Node document = s.getNodeByIdentifier(docid);
//				Node node1 = document.getParent().getParent();
//				DummyDTO node1DTO = new DummyDTO(node1.getIdentifier());
//				try {
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//						Locker.acquireLock(document, dto, false, s, locker);
//					} else {
//						Locker.acquireLock(node1, node1DTO, true, s, locker);
//					}
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//
//					if (username.equals(USER1)) {
//						notifyOther();
//						waitForMyTurn();
//					}
//
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(node1, node1DTO, s, locker);
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (username.equals(USER2)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest1_1() throws Throwable {
//		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			loWaCon.connect();
//
//			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
//			testhelper.initDatabase();
//			testhelper.initRepository();
//
//			loWaCon.defaultLogin();
//			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");
//			userService.create(USER1, USER1);
//			userService.create(USER2, USER2);
//
//			Session s = jcr.getJCRSessionNoTx();
//			Node node1 = s.getRootNode().addNode("node1");
//			node1.addMixin(NodeType.MIX_LOCKABLE);
//			Node node2 = node1.addNode("node2");
//			node2.addMixin(NodeType.MIX_LOCKABLE);
//
//			Node document = node2.addNode(DOC1, WasabiNodeType.DOCUMENT);
//			document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 0);
//			document.setProperty(WasabiNodeProperty.CONTENT, CONTENT1);
//			s.save();
//
//			docid = document.getIdentifier();
//			dto = new DummyDTO(docid);
//		} finally {
//			jcr.logout();
//			jndi.close();
//			loWaCon.disconnect();
//		}
//
//		UserThread user1 = new SetSomethingTest1_1(USER1);
//		UserThread user2 = new SetSomethingTest1_1(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() instanceof LockException) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		Session s = jcr.getJCRSessionNoTx();
//		try {
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -------------------------------------------------------------------------------------------------------
//	// tests whether two users can subsequently acquire the lock
//	class SetSomethingTest2 extends UserThread {
//
//		public SetSomethingTest2(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				document = s.getNodeByIdentifier(docid);
//				try {
//					if (username.equals(USER2)) {
//						waitForCommitOfOther();
//					}
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest2() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest2(USER1);
//		UserThread user2 = new SetSomethingTest2(USER2);
//
//		// assert that there are no errors -> shows that after one thread is done another one is able to acquire a lock
//		AssertJUnit.assertTrue(executeUserThreads(user1, user2).isEmpty());
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER2, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -----------------------------------------------------------------------------------------------------------------
//	// tests whether the lock gets released if the original lock holder encounters a problem
//	class SetSomethingTest3 extends UserThread {
//
//		public SetSomethingTest3(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				document = s.getNodeByIdentifier(docid);
//				try {
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//					}
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//
//					if (username.equals(USER1)) {
//						throw new Exception("Simulated problem");
//					}
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (username.equals(USER1)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest3() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest3(USER1);
//		UserThread user2 = new SetSomethingTest3(USER2);
//
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				throw t; // node could not be unlocked by user1
//			}
//		}
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER2, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -----------------------------------------------------------------------------------------------------------------
//	// tests the case when a write attempt should fail because of the optLockId check
//	class SetSomethingTest4 extends UserThread {
//
//		public SetSomethingTest4(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				document = s.getNodeByIdentifier(docid);
//				try {
//					if (username.equals(USER2)) {
//						Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//						waitForCommitOfOther();
//						Locker.acquireLock(document, dto, false, s, locker);
//						Locker.checkOptLockId(document, dto, optLockId);
//					} else {
//						Locker.acquireLock(document, dto, false, s, locker);
//					}
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest4() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest4(USER1);
//		UserThread user2 = new SetSomethingTest4(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() == null) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -----------------------------------------------------------------------------------------
//	// tests the case when one user tries to write an already locked node... with transactions
//	class SetSomethingTest1WithTransaction extends UserThread {
//
//		public SetSomethingTest1WithTransaction(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				UserTransaction utx = (UserTransaction) loWaCon.lookupGeneral("UserTransaction");
//				boolean error = false;
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				try {
//					utx.begin();
//					document = s.getNodeByIdentifier(docid);
//
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//					}
//
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//
//					if (username.equals(USER1)) {
//						notifyOther();
//						waitForMyTurn();
//					}
//
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					error = true;
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} catch (Throwable t) {
//					error = true;
//					throw t;
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (error) {
//						utx.rollback();
//					} else {
//						utx.commit();
//					}
//
//					if (username.equals(USER2)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest1WithTransaction() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest1WithTransaction(USER1);
//		UserThread user2 = new SetSomethingTest1WithTransaction(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() instanceof LockException) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// ------------------------------------------------------------------------------------------
//	// tests the case when one user tries to lock a node which should already be locked because of a deep lock on an
//	// ancestor... with transactions
//	class SetSomethingTest1_1WithTransaction extends UserThread {
//
//		public SetSomethingTest1_1WithTransaction(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				UserTransaction utx = (UserTransaction) loWaCon.lookupGeneral("UserTransaction");
//				boolean error = false;
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Session s = jcr.getJCRSessionNoTx();
//				Node document = s.getNodeByIdentifier(docid);
//				Node node1 = document.getParent().getParent();
//				DummyDTO node1DTO = new DummyDTO(node1.getIdentifier());
//				try {
//					utx.begin();
//
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//						Locker.acquireLock(document, dto, false, s, locker);
//					} else {
//						Locker.acquireLock(node1, node1DTO, true, s, locker);
//					}
//
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//
//					if (username.equals(USER1)) {
//						notifyOther();
//						waitForMyTurn();
//					}
//
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					error = true;
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} catch (Throwable t) {
//					error = true;
//					throw t;
//				} finally {
//					Locker.releaseLock(node1, node1DTO, s, locker);
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (error) {
//						utx.rollback();
//					} else {
//						utx.commit();
//					}
//
//					if (username.equals(USER2)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest1_1WithTransaction() throws Throwable {
//		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			loWaCon.connect();
//
//			TestHelperLocal testhelper = (TestHelperLocal) loWaCon.lookup("TestHelper");
//			testhelper.initDatabase();
//			testhelper.initRepository();
//
//			loWaCon.defaultLogin();
//			UserServiceLocal userService = (UserServiceLocal) loWaCon.lookup("UserService");
//			userService.create(USER1, USER1);
//			userService.create(USER2, USER2);
//
//			Session s = jcr.getJCRSessionNoTx();
//			Node node1 = s.getRootNode().addNode("node1");
//			node1.addMixin(NodeType.MIX_LOCKABLE);
//			Node node2 = node1.addNode("node2");
//			node2.addMixin(NodeType.MIX_LOCKABLE);
//
//			Node document = node2.addNode(DOC1, WasabiNodeType.DOCUMENT);
//			document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 0);
//			document.setProperty(WasabiNodeProperty.CONTENT, CONTENT1);
//			s.save();
//
//			docid = document.getIdentifier();
//			dto = new DummyDTO(docid);
//		} finally {
//			jcr.logout();
//			jndi.close();
//			loWaCon.disconnect();
//		}
//
//		UserThread user1 = new SetSomethingTest1_1WithTransaction(USER1);
//		UserThread user2 = new SetSomethingTest1_1WithTransaction(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() instanceof LockException) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// --------------------------------------------------------------------------------------------------------
//	// tests whether two users can subsequently acquire the lock... with transactions
//	class SetSomethingTest2WithTransaction extends UserThread {
//
//		public SetSomethingTest2WithTransaction(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				UserTransaction utx = (UserTransaction) loWaCon.lookupGeneral("UserTransaction");
//				boolean error = false;
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				try {
//					utx.begin();
//					document = s.getNodeByIdentifier(docid);
//
//					if (username.equals(USER2)) {
//						waitForCommitOfOther();
//					}
//
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					error = true;
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} catch (Throwable t) {
//					error = true;
//					throw t;
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (error) {
//						utx.rollback();
//					} else {
//						utx.commit();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest2WithTransaction() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest2WithTransaction(USER1);
//		UserThread user2 = new SetSomethingTest2WithTransaction(USER2);
//
//		// assert that there are no errors -> proves that after one thread is done another one is able to acquire a lock
//		AssertJUnit.assertTrue(executeUserThreads(user1, user2).isEmpty());
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER2, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// --------------------------------------------------------------------------------------------------------------------
//	// tests whether the lock gets released if the original lock holder encounters a problem... with transactions
//	class SetSomethingTest3WithTransaction extends UserThread {
//
//		public SetSomethingTest3WithTransaction(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				UserTransaction utx = (UserTransaction) loWaCon.lookupGeneral("UserTransaction");
//				boolean error = false;
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				try {
//					utx.begin();
//					document = s.getNodeByIdentifier(docid);
//
//					if (username.equals(USER2)) {
//						waitForMyTurn();
//					}
//
//					Locker.acquireLock(document, dto, false, s, locker);
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//
//					if (username.equals(USER1)) {
//						throw new Exception("Simulated problem");
//					}
//					s.save();
//
//				} catch (RepositoryException re) {
//					error = true;
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} catch (Throwable t) {
//					error = true;
//					throw t;
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//
//					if (error) {
//						utx.rollback();
//					} else {
//						utx.commit();
//					}
//
//					if (username.equals(USER1)) {
//						notifyOther();
//					}
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest3WithTransaction() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest3WithTransaction(USER1);
//		UserThread user2 = new SetSomethingTest3WithTransaction(USER2);
//
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				throw t; // node could not be unlocked by user1
//			}
//		}
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER2, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -----------------------------------------------------------------------------------------------------------------
//	// tests the case when a write attempt should fail because of the optLockId check... with transactions
//	class SetSomethingTest4WithTransaction extends UserThread {
//
//		public SetSomethingTest4WithTransaction(String username) {
//			super(username);
//		}
//
//		@Override
//		public void run() {
//			LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//			JndiConnector jndi = JndiConnector.getJNDIConnector();
//			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//			try {
//				loWaCon.connect();
//				loWaCon.login(username, username);
//
//				LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//				Node document = null;
//				Session s = jcr.getJCRSessionNoTx();
//				try {
//					document = s.getNodeByIdentifier(docid);
//
//					if (username.equals(USER2)) {
//						Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//						waitForCommitOfOther();
//						Locker.acquireLock(document, dto, false, s, locker);
//						Locker.checkOptLockId(document, dto, optLockId);
//					} else {
//						Locker.acquireLock(document, dto, false, s, locker);
//					}
//
//					document.setProperty(WasabiNodeProperty.CONTENT, username);
//					Long optLockId = document.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
//					document.setProperty(WasabiNodeProperty.OPT_LOCK_ID, ++optLockId);
//					s.save();
//
//				} catch (RepositoryException re) {
//					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
//				} finally {
//					Locker.releaseLock(document, dto, s, locker);
//				}
//			} catch (Throwable t) {
//				throwables.add(t);
//			} finally {
//				jcr.logout();
//				jndi.close();
//				loWaCon.disconnect();
//			}
//		}
//	}
//
//	@Test
//	public void setSomethingTest4WithTransaction() throws Throwable {
//		setUpBeforeEachMethod();
//		UserThread user1 = new SetSomethingTest4WithTransaction(USER1);
//		UserThread user2 = new SetSomethingTest4WithTransaction(USER2);
//
//		boolean problemRecognized = false;
//		for (Throwable t : executeUserThreads(user1, user2)) {
//			if (t instanceof ConcurrentModificationException) {
//				if (t.getCause() == null) {
//					problemRecognized = true;
//				}
//			} else {
//				throw t;
//			}
//		}
//		AssertJUnit.assertTrue(problemRecognized);
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			AssertJUnit.assertEquals(USER1, document.getProperty(WasabiNodeProperty.CONTENT).getString());
//		} finally {
//			jcr.logout();
//			jndi.close();
//		}
//	}
//
//	// -------------------------------------------------------------------------------------------------
//	// tests that no harm is done when the writeAccessRelease-method tries to unlock although no lock is acquired
//	@Test
//	public void unlockWithoutLock() throws Throwable {
//		setUpBeforeEachMethod();
//		LocalWasabiConnector loWaCon = new LocalWasabiConnector();
//		JndiConnector jndi = JndiConnector.getJNDIConnector();
//		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
//		try {
//			loWaCon.connect();
//			loWaCon.login(USER1, USER1);
//
//			LockingHelperLocal locker = (LockingHelperLocal) loWaCon.lookup("LockingHelper");
//
//			Session s = jcr.getJCRSessionNoTx();
//			Node document = s.getNodeByIdentifier(docid);
//			Locker.releaseLock(document, dto, s, locker);
//		} finally {
//			jcr.logout();
//			jndi.close();
//			loWaCon.disconnect();
//		}
//		// success
//	}
//}