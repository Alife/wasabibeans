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

package de.wasabibeans.framework.server.core.test.jcr;

import java.util.Vector;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
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
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.JCRTestBeanLocal;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.IN_CONTAINER)
public class SimpleJCRSessionLocalTest extends Arquillian {

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
				.addPackage(DebugInterceptor.class.getPackage()) // debug
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	@Test
	public void subsequentWrites() throws Exception {
		LocalWasabiConnector loCon = new LocalWasabiConnector();
		loCon.defaultConnectAndLogin();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initRepository();
		testhelper.initDatabase();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		userService.create(USER1, USER1);
		userService.create(USER2, USER2);
		loCon.disconnect();

		/**
		 * The following test-case works, IF step 1 is done without being encapsulated in a transaction (just remove the
		 * utx-lines in step 1 and add the 's1.logout()')
		 */
		InitialContext jndiContext = new InitialContext();
		Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
		UserTransaction utx;
		String id;
		
		// Step 1: user 1 acquires session and writes, then logs out
		utx = (UserTransaction) jndiContext.lookup("UserTransaction");
		utx.begin();
		Session s1 = rep.login(new SimpleCredentials(USER1, USER1.toCharArray()));
		Node nodeBys1 = s1.getRootNode().addNode("aNode");
		nodeBys1.setProperty("aProperty", USER1);
		s1.save();
		id = nodeBys1.getIdentifier();
		// s1.logout();
		utx.commit();

		// Step 2: user 2 acquires session, alters value written by user 1, then logs out
		utx = (UserTransaction) jndiContext.lookup("UserTransaction");
		utx.begin();
		Session s2 = rep.login(new SimpleCredentials(USER2, USER2.toCharArray()));
		s2.getNodeByIdentifier(id).setProperty("aProperty", USER2);
		s2.save();
		utx.commit();

		// Step 3: user 1 acquires session again, reads the property altered by user 2... but does not read the correct
		// value
		utx = (UserTransaction) jndiContext.lookup("UserTransaction");
		utx.begin();
		Session s = rep.login(new SimpleCredentials(USER1, USER1.toCharArray()));
		AssertJUnit.assertEquals(USER2, s.getNodeByIdentifier(id).getProperty("aProperty").getString());
		utx.commit();
	}

	// @Test
	public void startAndShutdown() throws Exception {
		InitialContext jndiContext = new InitialContext();
		Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");

		// start repository by requesting a session
		Session s = rep.login(new SimpleCredentials("tester", "tester".toCharArray()));
		System.out.println(s.toString());

		// try to shutdown repository by logging out the only existing session (does not work due to JCA -> JCA is
		// pooling)
		s.logout();
	}

	// @Test
	public void startAndShutdownSessionBean() throws Exception {
		InitialContext jndiContext = new InitialContext();
		JCRTestBeanLocal jcrTest = (JCRTestBeanLocal) jndiContext.lookup("test/JCRTestBean/local");
		try {
			jcrTest.sessionLoginLogout("tester");
		} catch (Exception e) {
			// ...
		}

		System.out.println(Thread.currentThread().getId());
		Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");

		// test the outcome of the container managed transaction
		Session s = rep.login(new SimpleCredentials("tester", "tester".toCharArray()));
		NodeIterator ni = s.getRootNode().getNodes();
		while (ni.hasNext()) {
			System.out.println(ni.nextNode().getName());
		}
		s.logout();
	}

	// --------------------- Multi-User-Tests ---------------------------------------------------------
	private static final String USER1 = "user1", USER2 = "user2";

	private final static Object activeThreadLock = new Object();

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

	class JCRSessionTester extends UserThread {

		public JCRSessionTester(String username, Vector<Exception> exceptions) {
			super(username, exceptions);
		}

		@Override
		public void run() {
			try {
				System.out.println(username + " has thread-id: " + Thread.currentThread().getId());
				InitialContext jndiContext = new InitialContext();
				JCRTestBeanLocal jcrTest = (JCRTestBeanLocal) jndiContext.lookup("test/JCRTestBean/local");

				if (username.equals(USER1)) {
					System.out.println(username + " calls sessionLoginLogout");
					try {
						jcrTest.sessionLoginLogout(username);
					} catch (Exception e) {
						// ...
					}

					notifyOther();
					waitForMyTurn();

					// test the outcome of the container managed transaction
					System.out.println(username + " prints out the subnodes of root");
					Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
					Session s = rep.login(new SimpleCredentials(username, username.toCharArray()));
					NodeIterator ni = s.getRootNode().getNodes();
					while (ni.hasNext()) {
						System.out.println(ni.nextNode().getName());
					}
					s.logout();

					notifyOther();
				} else {
					waitForMyTurn();

					System.out.println(username + " calls sessionLoginLogout");
					try {
						jcrTest.sessionLoginLogout(username);
					} catch (Exception e) {
						// ...
					}

					notifyOther();
					waitForMyTurn();

					// test the outcome of the container managed transaction
					System.out.println(username + " prints out the subnodes of root");
					Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
					Session s = rep.login(new SimpleCredentials(username, username.toCharArray()));
					NodeIterator ni = s.getRootNode().getNodes();
					while (ni.hasNext()) {
						System.out.println(ni.nextNode().getName());
					}
					s.logout();
				}
			} catch (Exception e) {
				exceptions.add(e);
			}
		}
	}

	// @Test
	public void multiUserSessionTest() throws Exception {
		Vector<Exception> exceptions = new Vector<Exception>();
		UserThread user1 = new JCRSessionTester(USER1, exceptions);
		UserThread user2 = new JCRSessionTester(USER2, exceptions);
		user1.setOtherUser(user2);
		user2.setOtherUser(user1);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!exceptions.isEmpty()) {
			throw exceptions.get(0);
		}
		System.out.println("Both user actions are done.");
	}

	// --------------------- Parallel Transaction Test -----------------------------------------
	class ParallelTransactionUserThread extends Thread {
		private TestCallable userAction;
		private Vector<Exception> exceptions;

		public ParallelTransactionUserThread(Vector<Exception> exceptions) {
			this.exceptions = exceptions;
		}

		public void setUserAction(TestCallable userAction) {
			this.userAction = userAction;
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
			} catch (Exception e) {
				exceptions.add(e);
			}
		}
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
		public Object call() throws Exception {
			System.out.println(username + " has stateless-bean-thread-id " + Thread.currentThread().getId());
			return null;
		}
	}

	class ParallelTransactionCallable extends TestCallable {

		public ParallelTransactionCallable(String username, Thread otherUser) {
			super(username, otherUser);
		}

		@Override
		public Object call() throws Exception {
			InitialContext jndiContext = new InitialContext();

			if (username.equals(USER1)) {
				Thread.sleep(1000);
				System.out.println(username + " retrieves session");
				Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
				Session s = rep.login(new SimpleCredentials(username, username.toCharArray()));

				notifyOther();
				waitForMyTurn();
			} else {
				waitForMyTurn();
				System.out.println(username + " retrieves session");
				Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
				Session s = rep.login(new SimpleCredentials(username, username.toCharArray()));
				notifyOther();
			}
			return null;
		}
	}

	// @Test
	public void parallelTransactionTest() throws Exception {
		Vector<Exception> exceptions = new Vector<Exception>();
		ParallelTransactionUserThread user1 = new ParallelTransactionUserThread(exceptions);
		ParallelTransactionUserThread user2 = new ParallelTransactionUserThread(exceptions);
		TestCallable userAction1 = new ParallelTransactionCallable(USER1, user2);
		TestCallable userAction2 = new ParallelTransactionCallable(USER2, user1);
		user1.setUserAction(userAction1);
		user2.setUserAction(userAction2);

		user1.start();
		user2.start();

		user1.join();
		user2.join();
		if (!exceptions.isEmpty()) {
			throw exceptions.get(0);
		}
		System.out.println("Both user transactions have committed.");
	}

}
