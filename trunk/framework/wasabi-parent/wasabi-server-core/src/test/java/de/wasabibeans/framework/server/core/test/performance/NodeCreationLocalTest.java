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

package de.wasabibeans.framework.server.core.test.performance;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
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
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Run(RunModeType.IN_CONTAINER)
public class NodeCreationLocalTest extends Arquillian {

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

	private LocalWasabiConnector loCon;
	private WasabiRoomDTO rootRoom;

	private void beforeTest() throws Exception {
		loCon = new LocalWasabiConnector();
		loCon.connect();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		loCon.disconnect();
	}

	//@Test
	/* this is the optimum reachable; no ejb, nodetype or whatsoever overhead */
	// results are: 1653, 1681, 1551, 1482 (without output), 1407 (without output), 1329 (without output), 1424 (without
	// output)
	public void directJCRTest() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			long start = System.currentTimeMillis();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				// System.out.println(i);
				rootNode.addNode("node" + i);
			}
			s.save();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jcr.cleanup(true);
		}
	}

	// @Test
	/* above with transaction */
	// results are: 1420, 1580, 1579
	public void directJCRTestTx() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i);
			}
			s.save();
			jcr.cleanup(false);
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// @Test
	/* above with save after every node creation */
	// results are: 22807, 23256, 23240
	public void directJCRTestTxMultipleSaves() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i);
				s.save();
			}
			jcr.cleanup(false);
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// @Test
	/* above without transaction */
	// results are: 133851
	public void directJCRTestMultipleSaves() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			long start = System.currentTimeMillis();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i);
				s.save();
			}
			jcr.cleanup(false);
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	// -------------------------------------------------------------------------------------------------

	//@Test
	/* introducing nodetype overhead; referenceable, lockable etc, all that comes at a cost */
	// results are: 12483, 12447, 13586, 13824, 11618 (without output), 13468 (without output), 13256 (without output)
	// results without autocreated elements: 2793, 1950, 2184, 1856, 1966
	public void directJCRNodeTypeTest() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			long start = System.currentTimeMillis();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				//System.out.println(i);
				rootNode.addNode("node" + i, WasabiNodeType.ROOM);
			}
			s.save();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	// @Test
	/* above with transaction */
	// results are: 13917, 13761
	public void directJCRNodeTypeTestTx() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i, WasabiNodeType.ROOM);
			}
			s.save();
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// @Test
	/* above with save after every node creation */
	// results are: 38316, 37084
	public void directJCRNodeTypeTestTxMultipleSaves() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i, WasabiNodeType.ROOM);
				s.save();
			}
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// @Test
	/* above without transaction */
	// results are: 151823, 151099
	public void directJCRNodeTypeTestMultipleSaves() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			long start = System.currentTimeMillis();
			Session s = jcr.getJCRSession();
			Node rootNode = s.getRootNode();
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				rootNode.addNode("node" + i, WasabiNodeType.ROOM);
				s.save();
			}
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	// --------------------------------------------------------------------------------------

	//@Test
	/*
	 * introducing overhead due to all the extra stuff happening in RoomServiceImpl.create() (like setting creation
	 * date)
	 */
	// results are: 17279, 17479, 16819, 17331, 17007
	// results without autocreated elements: 4555, 4338, 4478
	public void directJCRNodeTypeAndExtraAttributesTest() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			long start = System.currentTimeMillis();
			Session s = jcr.getJCRSession();
			Node rootRoomNode = s.getNodeByIdentifier(rootRoom.getId());
			for (int i = 0; i < 5000; i++) {
				//System.out.println(i);
				RoomServiceImpl.create("node" + i, rootRoomNode, s, false, WasabiConstants.ROOT_USER_NAME);
			}
			s.save();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	// @Test
	/*
	 * above with transaction
	 */
	// results are: 17164, 16695
	public void directJCRNodeTypeAndExtraAttributesTxTest() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootRoomNode = s.getNodeByIdentifier(rootRoom.getId());
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				RoomServiceImpl.create("node" + i, rootRoomNode, s, false, WasabiConstants.ROOT_USER_NAME);
			}
			s.save();
			jcr.cleanup(false);
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// @Test
	/*
	 * above with save after every node creation
	 */
	// results are: 48718, 50252
	public void directJCRNodeTypeAndExtraAttributesTxMultipleSavesTest() throws Throwable {
		beforeTest();
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
			long start = System.currentTimeMillis();
			utx.begin();
			Session s = jcr.getJCRSession();
			Node rootRoomNode = s.getNodeByIdentifier(rootRoom.getId());
			for (int i = 0; i < 5000; i++) {
				System.out.println(i);
				RoomServiceImpl.create("node" + i, rootRoomNode, s, true, WasabiConstants.ROOT_USER_NAME);
			}
			jcr.cleanup(false);
			utx.commit();
			System.out.println("------ " + (System.currentTimeMillis() - start));
		} finally {
			jndi.close();
		}
	}

	// --------------------------------------------------------------------------------------------

	@Test
	/* introducing ejb overhead + events */
	// results are: 59738, 59223, 58966, 60082
	// on save per tx mode: 27225, 27010, 28020, 27982
	// without autocreated, without events: 13262, 12544, 12638, 12637
	// without autocreated; with events: 15446, 16497
	public void localServiceTest() throws Throwable {
		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		long start = System.currentTimeMillis();
		utx.begin();
		for (int i = 0; i < 4700; i++) {
			//System.out.println(i);
			roomService.create("room" + i, rootRoom);
		}
		utx.commit();
		System.out.println("------ " + (System.currentTimeMillis() - start));
		loCon.disconnect();
	}

	// @Test
	/* tests what happens when not using a transaction as in the test above */
	// results are: 171092, 174674
	public void localServiceWithoutTxTest() throws Throwable {
		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 5000; i++) {
			System.out.println(i);
			roomService.create("room" + i, rootRoom);
		}
		System.out.println("------ " + (System.currentTimeMillis() - start));
		loCon.disconnect();
	}
}
