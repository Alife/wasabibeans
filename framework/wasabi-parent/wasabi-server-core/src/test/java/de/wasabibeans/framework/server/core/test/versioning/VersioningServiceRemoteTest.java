package de.wasabibeans.framework.server.core.test.versioning;

import java.util.Vector;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

// TODO include containers, links, attributes, and tags in tests
@Run(RunModeType.AS_CLIENT)
public class VersioningServiceRemoteTest extends WasabiRemoteTest {

	private static final String ROOM = "room", ROOM1 = "room1", ROOM2 = "room2";
	private static final String CONTAINER1 = "container1", CONTAINER2 = "container2";
	private static final String DOCUMENT1 = "document1", DOCUMENT2 = "document2";

	private WasabiRoomDTO room, room1, room2;
	private WasabiDocumentDTO document1, document2;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();

		reWaCon.defaultLogin();
		room = roomService().create(ROOM, rootRoom);
		room1 = roomService().create(ROOM1, room);
		room2 = roomService().create(ROOM2, room);
		document1 = documentService().create(DOCUMENT1, room1);
		documentService().setContent(document1, DOCUMENT1, null);
		document2 = documentService().create(DOCUMENT2, room1);
		documentService().setContent(document1, DOCUMENT1, null);
		reWaCon.logout();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	// tests whether the versioning methods throw appropriate exceptions when the attempt is made to version s.th. else
	// than rooms, containers or documents
	public void illegalParameterTest() throws Exception {
		WasabiUserDTO user = userService().getUserByName("user");
		try {
			versioningService().getVersions(user);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			versioningService().createVersion(user, "test");
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			versioningService().restoreVersion(user, "test");
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}
	}

	@Test
	// simple versioning test on a document; the subtree of a document contains no further versionable wasabi-object
	public void versioningADocumentTest() throws Exception {
		versioningService().createVersion(document1, "test");
		Vector<WasabiVersionDTO> versions = versioningService().getVersions(document1);
		AssertJUnit.assertEquals(1, versions.size());
		WasabiVersionDTO version = versions.get(0);
		AssertJUnit.assertEquals("test", version.getComment());

		documentService().setContent(document1, "newContent", null);
		AssertJUnit.assertEquals("newContent", documentService().getContent(document1).getValue());

		versioningService().restoreVersion(document1, version.getLabel());
		AssertJUnit.assertEquals(DOCUMENT1, documentService().getContent(document1).getValue());
	}

	@Test
	// tests that there are no problems when creating several versions of a document
	public void versioningADocumentMultipleTimesTest() throws Exception {
		versioningService().createVersion(document1, "test");
		documentService().setContent(document1, "test2", null);
		versioningService().createVersion(document1, "test2");
		documentService().setContent(document1, "test3", null);
		versioningService().createVersion(document1, "test3");

		Vector<WasabiVersionDTO> versions = versioningService().getVersions(document1);
		AssertJUnit.assertEquals(3, versions.size());
		WasabiVersionDTO version = versions.get(1);

		AssertJUnit.assertEquals("test3", documentService().getContent(document1).getValue());
		versioningService().restoreVersion(document1, version.getLabel());
		AssertJUnit.assertEquals("test2", documentService().getContent(document1).getValue());
	}

	@Test
	// more complex versioning test, as a room can have a subtree with further versionable wasabi-objects
	public void versioningARoomTest() throws Exception {
		versioningService().createVersion(room, "test");
		Vector<WasabiVersionDTO> versions = versioningService().getVersions(room);
		AssertJUnit.assertEquals(1, versions.size());
		WasabiVersionDTO theVersion = versions.get(0);
		AssertJUnit.assertEquals("test", theVersion.getComment());

		// for each versionable node in the subtree a version must have been created
		versions = versioningService().getVersions(room2);
		AssertJUnit.assertEquals(1, versions.size());
		WasabiVersionDTO version = versions.get(0);
		AssertJUnit.assertEquals("test", version.getComment());

		versions = versioningService().getVersions(document1);
		AssertJUnit.assertEquals(1, versions.size());
		version = versions.get(0);
		AssertJUnit.assertEquals("test", version.getComment());

		documentService().setContent(document1, "newContent", null);
		AssertJUnit.assertEquals("newContent", documentService().getContent(document1).getValue());

		// restore must affect entire subtree
		versioningService().restoreVersion(room, theVersion.getLabel());
		AssertJUnit.assertEquals(DOCUMENT1, documentService().getContent(document1).getValue());

		documentService().remove(document2);
		AssertJUnit.assertNull(documentService().getDocumentByName(room1, DOCUMENT2));

		// restore must recreate deleted wasabi-objects in the subtree
		versioningService().restoreVersion(room, theVersion.getLabel());
		AssertJUnit.assertNotNull(documentService().getDocumentByName(room1, DOCUMENT2));
	}

	@Test
	// tests that there are no problems when creating several versions of a room
	public void versioningARoomTestMultipleTimes() throws Exception {
		versioningService().createVersion(room, "test");
		documentService().setContent(document1, "test2", null);
		versioningService().createVersion(room, "test2");
		documentService().setContent(document1, "test3", null);
		versioningService().createVersion(room, "test3");

		// check whether versions are created in entire subtree
		Vector<WasabiVersionDTO> versions = versioningService().getVersions(document1);
		AssertJUnit.assertEquals(3, versions.size());
		WasabiVersionDTO theVersion = versions.get(1);

		versions = versioningService().getVersions(room2);
		AssertJUnit.assertEquals(3, versions.size());
		WasabiVersionDTO version = versions.get(0);
		AssertJUnit.assertEquals("test", version.getComment());

		versions = versioningService().getVersions(document1);
		AssertJUnit.assertEquals(3, versions.size());
		version = versions.get(0);
		AssertJUnit.assertEquals("test", version.getComment());

		// restore one of the versions
		AssertJUnit.assertEquals("test3", documentService().getContent(document1).getValue());
		versioningService().restoreVersion(room, theVersion.getLabel());
		AssertJUnit.assertEquals("test2", documentService().getContent(document1).getValue());
	}

	@Test
	// tests whether the creation of a version is rolled back when this creation takes place within a transaction that
	// fails
	public void versioningWithinTxAndRollback() throws Exception {
		UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
		
		// execute transaction
		try {
			utx.begin();

			versioningService().createVersion(room, "test");
			AssertJUnit.assertEquals(1, versioningService().getVersions(document1).size());
			userService().create(null, null); // provoke exception and failure of transaction

			utx.commit();
		} catch (EJBTransactionRolledbackException rb) {
			utx.rollback();
		}
		
		// check that the creation of the version has been rolled back
		AssertJUnit.assertTrue(versioningService().getVersions(room).isEmpty());
		AssertJUnit.assertTrue(versioningService().getVersions(room2).isEmpty());
		AssertJUnit.assertTrue(versioningService().getVersions(document1).isEmpty());
	}
}
