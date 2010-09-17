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

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class LockingRemoteTest extends WasabiRemoteTest {

	WasabiRoomDTO room;
	WasabiContainerDTO container;
	WasabiDocumentDTO document;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();

		reWaCon.defaultLogin();
		room = roomService().create("room", rootRoom);
		container = containerService().create("container", room);
		document = documentService().create("document", container);
		reWaCon.logout();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	// user acquires lock and then edits the object
	public void lockAndWrite() throws Exception {
		WasabiDocumentDTO userDocument = null;
		try {
			userDocument = (WasabiDocumentDTO) lockingService().acquireLock(document, false);
			documentService().setContent(userDocument, "hallo", null);
			AssertJUnit.assertEquals("hallo", documentService().getContent(userDocument).getValue());
		} finally {
			lockingService().releaseLock(userDocument);
		}
	}

	@Test
	// user acquires deep lock and then adds object
	public void deepLockAndWrite() throws Exception {
		WasabiContainerDTO userContainer = null;
		try {
			userContainer = (WasabiContainerDTO) lockingService().acquireLock(container, true);
			documentService().create("document2", userContainer);
			AssertJUnit.assertEquals(2, documentService().getDocuments(userContainer).size());
		} finally {
			lockingService().releaseLock(userContainer);
		}
	}

	@Test
	// user locks and subsequent write access without corresponding dto fails
	public void lockAndFailedAccess() throws Exception {
		WasabiDocumentDTO userDocument = null;
		try {
			// lock
			userDocument = (WasabiDocumentDTO) lockingService().acquireLock(document, false);

			// try to access with the dto that does not contain the lock token
			try {
				documentService().setContent(document, "hallo", null);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().releaseLock(userDocument);
		}
	}

	@Test
	// user locks deep and subsequent attempts to add child-objects by using the wrong dto fail
	public void deeplockAndFailedAccess() throws Exception {
		WasabiRoomDTO userRoom = null;
		try {
			// lock
			userRoom = (WasabiRoomDTO) lockingService().acquireLock(room, true);

			// try to add child nodes by using the wrong dto
			try {
				documentService().create("document2", container);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().releaseLock(userRoom);
		}
	}

	@Test
	// user locks, unlocks and writes/locks again
	public void lockAndUnlock() throws Exception {
		WasabiDocumentDTO userDocument = null;
		// lock and unlock
		try {
			userDocument = (WasabiDocumentDTO) lockingService().acquireLock(document, false);
		} finally {
			lockingService().releaseLock(userDocument);
		}

		// write and lock again
		documentService().setContent(document, "hallo", null);
		try {
			userDocument = (WasabiDocumentDTO) lockingService().acquireLock(document, false);
		} finally {
			lockingService().releaseLock(userDocument);
		}
	}

	@Test
	// user locks deep, unlocks and writes/locks deep again
	public void lockDeepAndUnlock() throws Exception {
		WasabiContainerDTO userContainer = null;
		// lock and unlock
		try {
			userContainer = (WasabiContainerDTO) lockingService().acquireLock(container, true);
		} finally {
			lockingService().releaseLock(userContainer);
		}

		// write and lock again
		documentService().create("document2", container);
		try {
			userContainer = (WasabiContainerDTO) lockingService().acquireLock(container, true);
		} finally {
			lockingService().releaseLock(userContainer);
		}
	}

	@Test
	// user acquires normal lock and then tries to create a version
	public void provokeLockingException() throws Exception {
		WasabiRoomDTO userRoom = null;
		try {
			userRoom = (WasabiRoomDTO) lockingService().acquireLock(room, false);
			try {
				versioningService().createVersion(userRoom, "test");
			} catch (LockingException e) {
				// passed
			}
		} finally {
			lockingService().releaseLock(userRoom);
		}

		// check that it actually works with a deep lock
		try {
			userRoom = (WasabiRoomDTO) lockingService().acquireLock(room, true);
			versioningService().createVersion(userRoom, "test");
			AssertJUnit.assertEquals(1, versioningService().getVersions(userRoom).size());
		} finally {
			lockingService().releaseLock(userRoom);
		}
	}

	@Test
	// user tries to acquire lock although he already has it -> should be no problem if 'isDeep' is the same
	public void redundantlyAcquireLock() throws Exception {
		WasabiRoomDTO userRoom = null;
		try {
			userRoom = (WasabiRoomDTO) lockingService().acquireLock(room, false);

			// problem
			try {
				lockingService().acquireLock(userRoom, true);
				AssertJUnit.fail();
			} catch (LockingException e) {
				// passed
			}

			// no problem
			WasabiRoomDTO userRoom2 = (WasabiRoomDTO) lockingService().acquireLock(userRoom, false);
			AssertJUnit.assertEquals(userRoom.getLockToken(), userRoom2.getLockToken());
		} finally {
			lockingService().releaseLock(userRoom);
		}

		// check that no lock is left -> then the following call causes no problems
		roomService().rename(room, "test", null);
	}

	@Test
	// tests what happens if DTOs with invalid lock-tokens are used
	public void invalidDTOs() throws Exception {
		// lock and unlock and keep the dto
		WasabiRoomDTO userRoom1 = null;
		try {
			userRoom1 = (WasabiRoomDTO) lockingService().acquireLock(room, true);
		} finally {
			lockingService().releaseLock(userRoom1);
		}

		// create a new lock and try to write using old dto
		WasabiRoomDTO userRoom2 = null;
		try {
			userRoom2 = (WasabiRoomDTO) lockingService().acquireLock(room, true);
			System.out.println(userRoom1.getLockToken());
			System.out.println(userRoom2.getLockToken());
			try {
				documentService().create("document2", userRoom1);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().releaseLock(userRoom2);
		}
		
		// no problem to use dto that contains old lock-token when no locks are set
		documentService().create("document2", userRoom1);
	}
	
	@Test
	// tests that locks remain active until explicitly removed
	public void lockRemainsActiveDuringMultipleWrites() throws Exception {
		WasabiDocumentDTO userDocument = null;
		try {
			// lock
			userDocument = (WasabiDocumentDTO) lockingService().acquireLock(document, false);
			// multiple writes
			documentService().setContent(userDocument, "hello1", null);
			documentService().setContent(userDocument, "hello2", null);
			documentService().setContent(userDocument, "hello3", null);

			// try to access with the dto that does not contain the lock token
			try {
				documentService().setContent(document, "hallo", null);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().releaseLock(userDocument);
		}
	}
}
