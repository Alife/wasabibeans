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
package de.wasabibeans.framework.server.core.test.versioning;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

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
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Run(RunModeType.IN_CONTAINER)
public class SimpleJCRVersioningTest extends Arquillian {

	private static final String ROOM = "room", ROOM1 = "room1", ROOM2 = "room2";
	private static final String CONTAINER1 = "container1", CONTAINER2 = "container2";
	private static final String DOCUMENT1 = "document1", DOCUMENT2 = "document2";

	private String roomId, room1Id, room2Id, room3Id;
	private String container1Id, container2Id;
	private String document1Id, document2Id;

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
		JcrConnector jcr = JcrConnector.getJCRConnector();

		TestHelperLocal testHelper = (TestHelperLocal) jndi.lookup("test/TestHelper/local");
		testHelper.initDatabase();
		testHelper.initRepository();

		Session s = jcr.getJCRSession();
		VersionManager versionManager = s.getWorkspace().getVersionManager();

		Node room = s.getRootNode().addNode(ROOM, WasabiNodeType.ROOM);
		room.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		roomId = room.getIdentifier();

		Node room1 = room.getNode(WasabiNodeProperty.ROOMS).addNode(ROOM1, WasabiNodeType.ROOM);
		room1.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		room1Id = room1.getIdentifier();
		Node room2 = room.getNode(WasabiNodeProperty.ROOMS).addNode(ROOM2, WasabiNodeType.ROOM);
		room2.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		room2Id = room2.getIdentifier();

		Node container1 = room1.getNode(WasabiNodeProperty.CONTAINERS).addNode(CONTAINER1, WasabiNodeType.CONTAINER);
		container1.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		container1Id = container1.getIdentifier();
		Node container2 = room1.getNode(WasabiNodeProperty.CONTAINERS).addNode(CONTAINER2, WasabiNodeType.CONTAINER);
		container2.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		container2Id = container2.getIdentifier();

		Node document1 = container1.getNode(WasabiNodeProperty.DOCUMENTS).addNode(DOCUMENT1, WasabiNodeType.DOCUMENT);
		document1.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		document1Id = document1.getIdentifier();
		Node document2 = container1.getNode(WasabiNodeProperty.DOCUMENTS).addNode(DOCUMENT2, WasabiNodeType.DOCUMENT);
		document2.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 1);
		document2Id = document2.getIdentifier();

		s.save();
		
		versionManager.checkout(room.getPath());
		versionManager.checkout(room1.getPath());
		versionManager.checkout(room1.getPath());
		versionManager.checkout(container1.getPath());
		versionManager.checkout(container2.getPath());
		versionManager.checkout(document1.getPath());
		versionManager.checkout(document2.getPath());
		
		s.logout();
	}

	private void printOutVersionHistory(Node node) throws Exception {
		VersionHistory versionHistory = node.getSession().getWorkspace().getVersionManager().getVersionHistory(
				node.getPath());
		System.out.println("=== Version History of " + node.getName() + " ===");
		for (VersionIterator vi = versionHistory.getAllVersions(); vi.hasNext();) {
			Version aVersion = vi.nextVersion();
			System.out.println(aVersion.getName());
		}
	}
	
	private void printOutVersionHistory(String path, Session s) throws Exception {
		VersionHistory versionHistory = s.getWorkspace().getVersionManager().getVersionHistory(
				path);
		System.out.println("=== Version History of " + path + " ===");
		for (VersionIterator vi = versionHistory.getAllVersions(); vi.hasNext();) {
			Version aVersion = vi.nextVersion();
			System.out.println(aVersion.getName());
		}
	}

	private void printOutTestTree(Node node, String prefix) throws Exception {
		if (prefix.equals("")) {
			System.out.println("=== Test Tree ===");
		}
		prefix += "--";
		System.out.println(prefix + node.getName() + ": " + node.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong());
		for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
			Node aNode = ni.nextNode();
			try {
			System.out.println(prefix + aNode.getName() + ": " + aNode.getProperty(WasabiNodeProperty.VERSION_COMMENT).getString());
			} catch (PathNotFoundException e) {}
			for (NodeIterator ni2 = aNode.getNodes(); ni2.hasNext();) {
				printOutTestTree(ni2.nextNode(), prefix);
			}
		}
	}

	private void printOutFullTree(Node node, String prefix) throws Exception {
		if (prefix.equals("")) {
			System.out.println("=== Tree of a Version ===");
		}
		prefix += "--";
		System.out.println(prefix + node.getName() + ": " + node.getPrimaryNodeType().getName());
		for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
			printOutFullTree(ni.nextNode(), prefix);
		}
	}

	// -------------------------------------------------------------------------------------------------------------

	@Test
	public void test() throws Throwable {
		setUpBeforeEachMethod();
		JcrConnector jcr = JcrConnector.getJCRConnector();
		Session s = jcr.getJCRSession();
		try {
			VersionManager versionManager = s.getWorkspace().getVersionManager();
			Node room = s.getNodeByIdentifier(roomId);

			room.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 2);
			s.save();
			Version version = versionManager.checkin(room.getPath());
			versionManager.getVersionHistory(room.getPath()).addVersionLabel(version.getName(), "hu", false);
			versionManager.checkout(room.getPath());
			
			room.setProperty(WasabiNodeProperty.OPT_LOCK_ID, 3);
			s.save();
			version = versionManager.checkin(room.getPath());
			versionManager.checkout(room.getPath());
			
			printOutFullTree(version, "");

		} finally {
			s.logout();
		}
	}

}