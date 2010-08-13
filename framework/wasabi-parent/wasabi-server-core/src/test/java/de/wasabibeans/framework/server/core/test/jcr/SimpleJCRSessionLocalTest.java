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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.apache.jackrabbit.jca.JCASessionHandle;
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
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
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
		loCon.connect();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();
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
		Session s1 = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
		Node nodeBys1 = s1.getRootNode().addNode("aNode");
		nodeBys1.setProperty("aProperty", "user1");
		s1.save();
		id = nodeBys1.getIdentifier();
		s1.logout();
		utx.commit();

		// Step 2: user 2 acquires session, alters value written by user 1, then logs out
		utx = (UserTransaction) jndiContext.lookup("UserTransaction");
		utx.begin();
		Session s2 = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
		AssertJUnit.assertEquals("user1", s2.getNodeByIdentifier(id).getProperty("aProperty").getString());
		s2.getNodeByIdentifier(id).setProperty("aProperty", "user2");
		s2.save();
		s2.logout();
		utx.commit();

		// Step 3: user 1 acquires session again, reads the property altered by user 2... but does not read the correct
		// value
		utx = (UserTransaction) jndiContext.lookup("UserTransaction");
		utx.begin();
		Session s = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
		AssertJUnit.assertEquals("user2", s.getNodeByIdentifier(id).getProperty("aProperty").getString());
		s.logout();
		utx.commit();
	}

	@Test
	public void concurrentTransactions() throws Throwable {
		LocalWasabiConnector loCon = new LocalWasabiConnector();
		loCon.connect();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();
		loCon.disconnect();
		final Object activeThreadLock = new Object();

		InitialContext jndiContext = new InitialContext();
		Repository rep = (Repository) jndiContext.lookup("java:/jcr/local");
		Session s = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
		System.out.println("1: " + ((JCASessionHandle) s).getXAResource().toString());
		Node node;
		try {
			node = s.getRootNode().getNode("aNode");
			node.remove();
		} catch (Exception e) {
			// ...
		}
		node = s.getRootNode().addNode("aNode");
		node.setProperty("aProperty", "1");
		s.save();
		s.logout();

		Thread user1 = new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
					InitialContext jndiContext = new InitialContext();
					Repository rep = (Repository) jndiContext.lookup("java:/jcr/local");
					UserTransaction utx = (UserTransaction) jndiContext.lookup("UserTransaction");
					utx.begin();
					Session s = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
					System.out.println("3: " + ((JCASessionHandle) s).getXAResource().toString());

					Node node = s.getRootNode().getNode("aNode");
					System.out.println(node.getProperty("aProperty").getString());

					synchronized (activeThreadLock) {
						activeThreadLock.notify();
						activeThreadLock.wait();
					}

					s = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
					System.out.println("4: " + ((JCASessionHandle) s).getXAResource().toString());
					node = s.getRootNode().getNode("aNode");
					System.out.println(node.getProperty("aProperty").getString());
					s.save();
					utx.commit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		Thread user2 = new Thread() {
			public void run() {
				try {
					InitialContext jndiContext = new InitialContext();
					Repository rep = (Repository) jndiContext.lookup("java:/jcr/local");
					UserTransaction utx = (UserTransaction) jndiContext.lookup("UserTransaction");
					utx.begin();
					Session s = rep.login(new SimpleCredentials(WasabiConstants.JCR_LOGIN, WasabiConstants.JCR_LOGIN.toCharArray()));
					System.out.println("2: " + ((JCASessionHandle) s).getXAResource().toString());
					Node node = s.getRootNode().getNode("aNode");

					synchronized (activeThreadLock) {
						activeThreadLock.wait();
					}

					node.setProperty("aProperty", "2");
					s.save();
					utx.commit();

					synchronized (activeThreadLock) {
						activeThreadLock.notify();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		user1.start();
		user2.start();
		user1.join();
		user2.join();
	}
}
