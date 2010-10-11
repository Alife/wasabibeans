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

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
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
		WasabiDocumentDTO lockDocument = null;
		try {
			lockDocument = lockingService().lock(document, false);
			documentService().setContent(lockDocument, "hallo", null);
			AssertJUnit.assertEquals("hallo", documentService().getContent(lockDocument).getValue());
		} finally {
			lockingService().unlock(lockDocument);
		}
	}

	@Test
	// user acquires deep lock and then adds object
	public void deepLockAndWrite() throws Exception {
		WasabiContainerDTO lockContainer = null;
		try {
			lockContainer = lockingService().lock(container, true);
			documentService().create("document2", lockContainer);
			AssertJUnit.assertEquals(2, documentService().getDocuments(lockContainer).size());
		} finally {
			lockingService().unlock(lockContainer);
		}
	}

	@Test
	// user acquires deep lock and then adds object on a deeper level
	public void deepLockAndWrite2() throws Exception {
		WasabiRoomDTO lockRoom = null;
		try {
			lockRoom = lockingService().lock(room, true);
			WasabiContainerDTO lockContainer = containerService().getContainerByName(lockRoom, "container");
			documentService().create("document2", lockContainer);
			AssertJUnit.assertEquals(2, documentService().getDocuments(lockContainer).size());
		} finally {
			lockingService().unlock(lockRoom);
		}
	}

	@Test
	// user locks and subsequent write access without corresponding dto fails
	public void lockAndFailedAccess() throws Exception {
		WasabiDocumentDTO lockDocument = null;
		try {
			// lock
			lockDocument = lockingService().lock(document, false);

			// try to access with the dto that does not contain the lock token
			try {
				documentService().setContent(document, "hallo", null);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().unlock(lockDocument);
		}
	}

	@Test
	// user locks deep and subsequent attempts to add child-objects by using the wrong dto fail
	public void deeplockAndFailedAccess() throws Exception {
		WasabiRoomDTO lockRoom = null;
		try {
			// lock
			lockRoom = lockingService().lock(room, true);

			// try to add child nodes by using the wrong dto
			try {
				documentService().create("document2", container);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().unlock(lockRoom);
		}
	}

	@Test
	// user locks, unlocks and writes/locks again
	public void lockAndUnlock() throws Exception {
		WasabiDocumentDTO lockDocument = null;
		// lock and unlock
		try {
			lockDocument = lockingService().lock(document, false);
		} finally {
			lockingService().unlock(lockDocument);
		}

		// write and lock again
		documentService().setContent(document, "hallo", null);
		try {
			lockDocument = lockingService().lock(document, false);
		} finally {
			lockingService().unlock(lockDocument);
		}
	}

	@Test
	// user locks deep, unlocks and writes/locks deep again
	public void lockDeepAndUnlock() throws Exception {
		WasabiContainerDTO lockContainer = null;
		// lock and unlock
		try {
			lockContainer = lockingService().lock(container, true);
		} finally {
			lockingService().unlock(lockContainer);
		}

		// write and lock again
		documentService().create("document2", container);
		try {
			lockContainer = lockingService().lock(container, true);
		} finally {
			lockingService().unlock(lockContainer);
		}
	}

	@Test
	// user tries to acquire lock although he already has it -> LockingException
	public void redundantlylock() throws Exception {
		WasabiRoomDTO lockRoom = null;
		try {
			lockRoom = lockingService().lock(room, false);

			try {
				lockingService().lock(lockRoom, false);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}

			try {
				lockingService().lock(lockRoom, true);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().unlock(lockRoom);
		}

		// check that no lock is left -> then the following call causes no problems
		roomService().rename(room, "test", null);
	}

	@Test
	// tests that calling unlock() although there is no lock causes no harm
	public void invalidunlock() throws Exception {
		try {
			// throws IllegalArgumentException as 'room' does not contain a lock-token
			lockingService().unlock(room);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		// lock and unlock
		WasabiDocumentDTO lockDocument = null;
		try {
			lockDocument = lockingService().lock(document, false);
		} finally {
			lockingService().unlock(lockDocument);
		}
		// try to release non-existing lock
		lockingService().unlock(lockDocument);
	}

	@Test
	// tests that locks remain active until explicitly removed
	public void lockRemainsActiveDuringMultipleWrites() throws Exception {
		WasabiDocumentDTO lockDocument = null;
		try {
			// lock
			lockDocument = lockingService().lock(document, false);
			// multiple writes
			documentService().setContent(lockDocument, "hello1", null);
			documentService().setContent(lockDocument, "hello2", null);
			documentService().setContent(lockDocument, "hello3", null);

			// try to access with the dto that does not contain the lock token
			try {
				documentService().setContent(document, "hallo", null);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}
		} finally {
			lockingService().unlock(lockDocument);
		}
	}

	@Test
	// tests a service that uses multiple locks at once
	public void multipleLocksAtOnce() throws Exception {
		WasabiGroupDTO group = groupService().create("group", null);
		WasabiUserDTO member = userService().create("member", "member");

		WasabiGroupDTO lockGroup = null;
		WasabiUserDTO lockMember = null;
		try {
			// lock both the group and the member
			lockGroup = lockingService().lock(group, true);
			lockMember = lockingService().lock(member, true);

			try {
				groupService().addMember(group, member);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}

			try {
				groupService().addMember(lockGroup, member);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}

			try {
				groupService().addMember(group, lockMember);
				AssertJUnit.fail();
			} catch (ConcurrentModificationException e) {
				// passed
			}

			groupService().addMember(lockGroup, lockMember);
			AssertJUnit.assertTrue(groupService().getMembers(lockGroup).contains(lockMember));
		} finally {
			lockingService().unlock(lockGroup);
			lockingService().unlock(lockMember);
		}
	}

	@Test
	// tests that a service fails when called with the wrong optLockId
	public void wrongOptLockUsed() throws Exception {
		try {
			// should fail because actual optLockId is 0
			documentService().setContent(document, "hallo", 1L);
			AssertJUnit.fail();
		} catch (ConcurrentModificationException e) {
			// passed
		}

		// no problem
		documentService().setContent(document, "hallo", 0L);
	}

}
