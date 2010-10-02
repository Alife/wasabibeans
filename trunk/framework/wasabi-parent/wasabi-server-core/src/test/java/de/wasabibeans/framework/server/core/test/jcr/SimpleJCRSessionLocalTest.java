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

import java.util.HashMap;
import java.util.Vector;

import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.jca.JCASessionHandle;
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
import de.wasabibeans.framework.server.core.event.WasabiEventType;
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
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Run(RunModeType.IN_CONTAINER)
public class SimpleJCRSessionLocalTest extends Arquillian {

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
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	@Test
	public void test() throws Throwable {
		LocalWasabiConnector loCon = new LocalWasabiConnector();
		loCon.connect();

		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();

		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		Session s = jcr.getJCRSession();
		try {
			UserThread user1 = new TestUser(USER1);
			UserThread user2 = new TestUser(USER2);
			executeUserThreads(user1, user2);

		} finally {
			jcr.cleanup(true);
			jndi.close();
			loCon.disconnect();
		}
	}

	class TestUser extends UserThread {
		public TestUser(String name) {
			super(name);
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			try {
				Session s = jcr.getJCRSession();
				JCASessionHandle handle = (JCASessionHandle) s;
				System.out.println(username + " " + handle.getXAResource().toString());
				VersionManager vm = s.getWorkspace().getVersionManager();
				System.out.println(username + " " + vm.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				jcr.cleanup(true);
				jndi.close();
			}

		}
	}

	// -----------------------------------------------------------------

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

	private static final String USER1 = "user1", USER2 = "user2";
	private static HashMap<String, Boolean> tickets;

	static {
		tickets = new HashMap<String, Boolean>();
		tickets.put(USER1, false);
		tickets.put(USER2, false);
	}

}
