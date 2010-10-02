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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
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
public class AnalyseWriteConflictsTest extends Arquillian {

	private static final boolean TX_ON = true;

	private static final String TESTNODENAME = "testNode";
	private static final String TESTPARENTNAME = "testParent";
	private static final String VERSIONLABEL = "versionLabel";
	private static String TESTNODE;
	private static String TESTPARENT;
	private static final String TESTPROPERTY = "testproperty";
	private static final String TESTCHILD = "testchild";
	private static final byte SET_DIFF_PROPERTY = 0, SET_SAME_PROPERTY = 1, ADD_DIFF_CHILD = 2, ADD_SAME_CHILD = 3,
			REMOVE_DIFF_CHILD = 4, REMOVE_SAME_CHILD = 5, MOVE = 6, MOVE_DIFF_DEST = 7, REMOVE = 8, REMOVE_PARENT = 9,
			VERSION = 10, RESTORE = 11, VERSION_DIFF = 12, RESTORE_DIFF = 13;

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

	@Test
	public void diffProperty() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_DIFF_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, SET_DIFF_PROPERTY);
			executeAndPrintResult("EditVsEditDiff", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void sameProperty() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_SAME_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, SET_SAME_PROPERTY);
			executeAndPrintResult("EditVsEditSame", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addDiff() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_DIFF_CHILD);
			UserThread user2 = new TestUserVS(USER2, ADD_DIFF_CHILD);
			executeAndPrintResult("AddchildVsAddchildDiff", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addSame() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, ADD_SAME_CHILD);
			executeAndPrintResult("AddchildVsAddchildSame", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeDiff() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addNode(USER1, NodeType.NT_FOLDER);
			session.getNodeByIdentifier(TESTNODE).addNode(USER2, NodeType.NT_FOLDER);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE_DIFF_CHILD);
			UserThread user2 = new TestUserVS(USER2, REMOVE_DIFF_CHILD);
			executeAndPrintResult("RemoveVsRemoveDiff", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeSame() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addNode(TESTCHILD, NodeType.NT_FOLDER);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, REMOVE_SAME_CHILD);
			executeAndPrintResult("RemoveVsRemoveSame", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void move() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME, NodeType.NT_FOLDER).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsMoveSameDestination", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveDiffDest() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME, NodeType.NT_FOLDER).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME, NodeType.NT_FOLDER).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE_DIFF_DEST);
			UserThread user2 = new TestUserVS(USER2, MOVE_DIFF_DEST);
			executeAndPrintResult("MoveVsMoveDiffDestination", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsEdit() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_SAME_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsEdit", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void editVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, SET_SAME_PROPERTY);
			executeAndPrintResult("EditVsMove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void editVsremove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE);
			UserThread user2 = new TestUserVS(USER2, SET_SAME_PROPERTY);
			executeAndPrintResult("EditVsRemove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeVsedit() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_SAME_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, REMOVE);
			executeAndPrintResult("RemoveVsEdit", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, ADD_SAME_CHILD);
			executeAndPrintResult("AddchildVsMove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsadd() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsAddchild", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeVsadd() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, REMOVE);
			executeAndPrintResult("RemoveVsAddchild", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addVsremove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE);
			UserThread user2 = new TestUserVS(USER2, ADD_SAME_CHILD);
			executeAndPrintResult("AddchildVsRemove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, REMOVE);
			executeAndPrintResult("RemoveVsMmove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsremove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsRemove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeParentVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, REMOVE_PARENT);
			executeAndPrintResult("RemoveParentVsMove", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsremoveParent() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE_PARENT);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsRemoveParent", false, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsedit() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_SAME_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, VERSION);
			executeAndPrintResult("VersionVsEdit", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void editVsVersion() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION);
			UserThread user2 = new TestUserVS(USER2, SET_SAME_PROPERTY);
			executeAndPrintResult("EditVsVersion", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsremove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE);
			UserThread user2 = new TestUserVS(USER2, VERSION);
			executeAndPrintResult("VersionVsRemove", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeVsVersion() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION);
			UserThread user2 = new TestUserVS(USER2, REMOVE);
			executeAndPrintResult("RemoveVsVersion", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, VERSION);
			executeAndPrintResult("VersionVsMove", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsVersion() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsVersion", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsaddchild() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, VERSION);
			executeAndPrintResult("VersionVsAddchild", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addChildVsversion() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION);
			UserThread user2 = new TestUserVS(USER2, ADD_SAME_CHILD);
			executeAndPrintResult("AddchildVsVersion", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsedit() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, SET_SAME_PROPERTY);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsEdit", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void editVsRestore() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, SET_SAME_PROPERTY);
			executeAndPrintResult("EditVsRestore", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsremove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, REMOVE);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsRemove", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void removeVsRestore() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, REMOVE);
			executeAndPrintResult("RemoveVsRestore", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsmove() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, MOVE);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsMove", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void moveVsRestore() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			TESTPARENT = session.getRootNode().addNode(TESTPARENTNAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, MOVE);
			executeAndPrintResult("MoveVsRestore", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsaddChild() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, ADD_SAME_CHILD);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsAddchild", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void addChildVsRestore() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, ADD_SAME_CHILD);
			executeAndPrintResult("AddchildVsRestore", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsversionSame() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION);
			UserThread user2 = new TestUserVS(USER2, VERSION);
			executeAndPrintResult("VersionVsVersionSame", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsversionDiff() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION_DIFF);
			UserThread user2 = new TestUserVS(USER2, VERSION_DIFF);
			executeAndPrintResult("VersionVsVersionDiff", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void versionVsRestore() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, VERSION_DIFF);
			executeAndPrintResult("VersionVsRestore", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsVersion() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, VERSION_DIFF);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsVersion", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsrestoreSame() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), VERSIONLABEL,
					false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE);
			UserThread user2 = new TestUserVS(USER2, RESTORE);
			executeAndPrintResult("RestoreVsRestoreSame", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	@Test
	public void restoreVsrestoreDiff() throws Throwable {
		beforeTest();
		try {
			Session session = jcr.getJCRSession();
			TESTNODE = session.getRootNode().addNode(TESTNODENAME).getIdentifier();
			session.getNodeByIdentifier(TESTNODE).addMixin(NodeType.MIX_VERSIONABLE);
			session.save();
			Version version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), USER1, false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that the versions differ
			session.getNodeByIdentifier(TESTNODE).setProperty("1", "1");
			session.save();

			version = session.getWorkspace().getVersionManager().checkin(
					session.getNodeByIdentifier(TESTNODE).getPath());
			session.getWorkspace().getVersionManager().getVersionHistory(
					session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(), USER2, false);
			session.getWorkspace().getVersionManager().checkout(session.getNodeByIdentifier(TESTNODE).getPath());

			// edit so that restore matters
			session.getNodeByIdentifier(TESTNODE).setProperty("2", "2");
			session.save();
			jcr.cleanup(true);

			UserThread user1 = new TestUserVS(USER1, RESTORE_DIFF);
			UserThread user2 = new TestUserVS(USER2, RESTORE_DIFF);
			executeAndPrintResult("RestoreVsRestoreDiff", true, user1, user2);
		} finally {
			afterTest();
		}
	}

	// ---------------------------------------------------------------------------------------

	private LocalWasabiConnector loCon;
	private JcrConnector jcr;
	private JndiConnector jndi;

	private void beforeTest() throws Exception {
		loCon = new LocalWasabiConnector();
		loCon.connect();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();
		loCon.disconnect();

		TESTPARENT = null;
		jndi = JndiConnector.getJNDIConnector();
		jcr = JcrConnector.getJCRConnector(jndi);
	}

	private void afterTest() {
		jndi.close();
	}

	private void executeAndPrintResult(String testname, boolean withVersions, UserThread user1, UserThread user2)
			throws Throwable {
		try {
			System.out.println("------------------------ " + testname);

			// execute test
			for (Throwable t : executeUserThreads(user1, user2)) {
				t.printStackTrace();
			}

			// print result
			Session session = jcr.getJCRSession();
			System.out.println("---Children rootNode:");
			for (NodeIterator ni = session.getRootNode().getNodes(); ni.hasNext();) {
				System.out.println(ni.nextNode().getName());
			}
			System.out.println("---Properties testNode:");
			for (PropertyIterator pi = session.getNodeByIdentifier(TESTNODE).getProperties(); pi.hasNext();) {
				Property p = pi.nextProperty();
				String value = null;
				try {
					value = p.getString();
				} catch (Exception e) {
					// no string value
				}
				System.out.println(p.getName() + " : " + value);
			}
			System.out.println("---Children testNode:");
			for (NodeIterator ni = session.getNodeByIdentifier(TESTNODE).getNodes(); ni.hasNext();) {
				System.out.println(ni.nextNode().getName());
			}
			if (withVersions) {
				System.out.println("---Versions testNode:");
				String rootVersionName = session.getWorkspace().getVersionManager().getVersionHistory(
						session.getNodeByIdentifier(TESTNODE).getPath()).getRootVersion().getName();
				for (VersionIterator vi = session.getWorkspace().getVersionManager().getVersionHistory(
						session.getNodeByIdentifier(TESTNODE).getPath()).getAllVersions(); vi.hasNext();) {
					Version version = vi.nextVersion();
					if (!version.getName().equals(rootVersionName)) {
						System.out.println("------a version:");
						Node n = version.getFrozenNode();
						System.out.println("---------Properties:");
						for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
							Property p = pi.nextProperty();
							String value = null;
							try {
								value = p.getString();
							} catch (Exception e) {
								// no string value
							}
							System.out.println("---------" + p.getName() + " : " + value);
						}
						System.out.println("---------Children:");
						for (NodeIterator nit = n.getNodes(); nit.hasNext();) {
							System.out.println("---------" + nit.nextNode().getName());
						}
					}
				}
			}
			if (TESTPARENT != null) {
				System.out.println("---Children parentNode:");
				for (NodeIterator ni = session.getNodeByIdentifier(TESTPARENT).getNodes(); ni.hasNext();) {
					System.out.println(ni.nextNode().getName());
				}
			}
			System.out.println("------------------------ " + testname + " END");
		} finally {
			jcr.cleanup(true);
		}
	}

	class TestUserVS extends UserThread {
		private byte whatAction;

		public TestUserVS(String name, byte whatAction) {
			super(name);
			this.whatAction = whatAction;
		}

		@Override
		public void run() {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
			UserTransaction utx = null;
			try {
				Session session = null;
				if (TX_ON) {
					utx = (UserTransaction) jndi.lookup("UserTransaction");
					utx.begin();
				}
				session = jcr.getJCRSession();

				System.out.println(username + " " + ((JCASessionHandle) session).getXAResource().toString());
				if (username.equals(USER2)) {
					waitForMyTurn();
				}
				System.out.println(username + " does action");
				Version version = null;
				switch (whatAction) {
				case SET_DIFF_PROPERTY:
					session.getNodeByIdentifier(TESTNODE).setProperty(username, username);
					break;
				case SET_SAME_PROPERTY:
					session.getNodeByIdentifier(TESTNODE).setProperty(TESTPROPERTY, username);
					break;
				case ADD_DIFF_CHILD:
					session.getNodeByIdentifier(TESTNODE).addNode(username, NodeType.NT_FOLDER);
					break;
				case ADD_SAME_CHILD:
					session.getNodeByIdentifier(TESTNODE).addNode(TESTCHILD, NodeType.NT_FOLDER);
					break;
				case REMOVE_DIFF_CHILD:
					session.getNodeByIdentifier(TESTNODE).getNode(username).remove();
					break;
				case REMOVE_SAME_CHILD:
					session.getNodeByIdentifier(TESTNODE).getNode(TESTCHILD).remove();
					break;
				case MOVE:
					session.move(session.getNodeByIdentifier(TESTNODE).getPath(), session.getNodeByIdentifier(
							TESTPARENT).getPath()
							+ "/" + TESTNODENAME);
					break;
				case MOVE_DIFF_DEST:
					session.move(session.getNodeByIdentifier(TESTNODE).getPath(), session.getNodeByIdentifier(
							TESTPARENT).getPath()
							+ "/" + username);
					break;
				case REMOVE:
					session.getNodeByIdentifier(TESTNODE).remove();
					break;
				case REMOVE_PARENT:
					session.getNodeByIdentifier(TESTPARENT).remove();
					break;
				case VERSION:
					// edit so that versions are different
					session.getNodeByIdentifier(TESTNODE).setProperty(username, username);
					session.save();
					version = session.getWorkspace().getVersionManager().checkin(
							session.getNodeByIdentifier(TESTNODE).getPath());
					session.getWorkspace().getVersionManager().getVersionHistory(
							session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(),
							VERSIONLABEL, false);
					session.getWorkspace().getVersionManager()
							.checkout(session.getNodeByIdentifier(TESTNODE).getPath());
					break;
				case RESTORE:
					session.getWorkspace().getVersionManager().restoreByLabel(
							session.getNodeByIdentifier(TESTNODE).getPath(), VERSIONLABEL, true);
					session.getWorkspace().getVersionManager()
							.checkout(session.getNodeByIdentifier(TESTNODE).getPath());
					break;
				case VERSION_DIFF:
					// edit so that versions are different
					session.getNodeByIdentifier(TESTNODE).setProperty(username, username);
					session.save();
					version = session.getWorkspace().getVersionManager().checkin(
							session.getNodeByIdentifier(TESTNODE).getPath());
					session.getWorkspace().getVersionManager().getVersionHistory(
							session.getNodeByIdentifier(TESTNODE).getPath()).addVersionLabel(version.getName(),
							username, false);
					session.getWorkspace().getVersionManager()
							.checkout(session.getNodeByIdentifier(TESTNODE).getPath());
					break;
				case RESTORE_DIFF:
					session.getWorkspace().getVersionManager().restoreByLabel(
							session.getNodeByIdentifier(TESTNODE).getPath(), username, true);
					session.getWorkspace().getVersionManager()
							.checkout(session.getNodeByIdentifier(TESTNODE).getPath());
					break;

				default:
					break;
				}

				if (TX_ON) {
					// perform save before any commit
					System.out.println(username + " saves");
					session.save();
				}

				if (username.equals(USER1)) {
					notifyOther();
					waitForCommitOfOther();
				}

				if (TX_ON) {
					// finish transaction with commit
					System.out.println(username + " commits");
					jcr.cleanup(false);
					utx.commit();
				} else {
					// finish by saving
					System.out.println(username + " saves");
					session.save();
				}
			} catch (Throwable t) {
				if (TX_ON) {
					jcr.cleanup(false);
				}
				System.out.println(username + "throws exception");
				if (TX_ON && t instanceof RuntimeException) {
					try {
						utx.rollback();
					} catch (Exception e) {
						throwables.add(e);
					}
				}
				throwables.add(t);
			} finally {
				if (!TX_ON) {
					jcr.cleanup(true);
				}
				jndi.close();
			}

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
				Session session = jcr.getJCRSession();
				System.out.println(username + " " + ((JCASessionHandle) session).getXAResource().toString());
				if (username.equals(USER1)) {
					waitForMyTurn();
					session.getWorkspace().getLockManager().lock(session.getRootNode().getNode("testNode").getPath(),
							false, true, 0L, null);
				} else {
					UserTransaction utx = (UserTransaction) jndi.lookup("UserTransaction");
					utx.begin();
					notifyOther();
					waitForCommitOfOther();
					session.getRootNode().getNode("testNode").setProperty("bla", "bla");
					session.save();
					System.out.println("---------"
							+ session.getWorkspace().getLockManager().isLocked(
									session.getRootNode().getNode("testNode").getPath()));
					utx.commit();
				}
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
