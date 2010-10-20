package de.wasabibeans.framework.server.core.test.versioning;

import java.util.Vector;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class VersioningServiceRemoteTest extends WasabiRemoteTest {

	private static final String ROOM = "room", ROOM1 = "room1", ROOM2 = "room2";
	private static final String CONTAINER1 = "container1", CONTAINER2 = "container2";
	private static final String DOCUMENT1 = "document1", DOCUMENT2 = "document2";

	private WasabiRoomDTO room, room1, room2;
	private WasabiContainerDTO container1;
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
		container1 = containerService().create(CONTAINER1, room1);
		containerService().create(CONTAINER2, room1);
		document1 = documentService().create(DOCUMENT1, container1);
		documentService().setContent(document1, DOCUMENT1, null);
		document2 = documentService().create(DOCUMENT2, container1);
		documentService().setContent(document2, DOCUMENT2, null);
		reWaCon.logout();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	//@Test
	// tests whether the versioning methods throw appropriate exceptions when the attempt is made to version s.th. else
	// than rooms, containers or documents
	public void illegalParameterTest() throws Exception {
		WasabiUserDTO user = userService().getUserByName("user");
		try {
			versioningService().getVersions(user);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			versioningService().createVersion(user, "test");
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			versioningService().restoreVersion(user, "test");
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}
	}

	@Test
	// simple versioning test on a document; the subtree of a document contains no further versionable wasabi-objects
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

//	@Test
	// tests that there are no problems when creating several versions of a document
	public void versioningADocumentMultipleTimesTest() throws Exception {
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
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

//	@Test
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

		versions = versioningService().getVersions(container1);
		AssertJUnit.assertEquals(1, versions.size());
		version = versions.get(0);
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

		documentService().remove(document2, null);
		AssertJUnit.assertNull(documentService().getDocumentByName(container1, DOCUMENT2));

		// restore must recreate deleted wasabi-objects in the subtree
		versioningService().restoreVersion(room, theVersion.getLabel());
		AssertJUnit.assertNotNull(documentService().getDocumentByName(container1, DOCUMENT2));
	}

//	@Test
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

		versions = versioningService().getVersions(container1);
		AssertJUnit.assertEquals(3, versions.size());
		version = versions.get(0);
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

//	@Test
	// tests whether objects like links, attributes, and tags that do not have their own version history are versioned
	// with their 'parent-objects'
	public void versioningAppendingObjects() throws Exception {
		tagService().addTag(room, "tag");
		WasabiLinkDTO link = linkService().create("link", room, container1);
		WasabiAttributeDTO attribute = attributeService().create("attribute", "attribute", document1);
		// store this state as a version
		versioningService().createVersion(room, "test");
		WasabiVersionDTO version = versioningService().getVersions(room).get(0);
		// make a few changes and confirm them
		tagService().addTag(room, "anotherTag");
		linkService().setDestination(link, document1, null);
		attributeService().setValue(attribute, "changedAttribute", null);
		AssertJUnit.assertEquals(2, tagService().getTags(room).size());
		AssertJUnit.assertEquals(document1, linkService().getDestination(link).getValue());
		AssertJUnit.assertEquals("changedAttribute", attributeService().getValue(String.class, attribute).getValue());
		// restore version and make assertions
		// tag of room
		versioningService().restoreVersion(room, version.getLabel());
		Vector<String> tags = tagService().getTags(room);
		AssertJUnit.assertEquals(1, tags.size());
		AssertJUnit.assertEquals("tag", tags.get(0));
		// link of container1
		Vector<WasabiLinkDTO> links = linkService().getLinks(container1);
		AssertJUnit.assertEquals(1, links.size());
		AssertJUnit.assertEquals(room, linkService().getDestination(links.get(0)).getValue());
		// attribute of document1
		Vector<WasabiAttributeDTO> attributes = attributeService().getAttributes(document1);
		AssertJUnit.assertEquals(1, attributes.size());
		AssertJUnit.assertEquals("attribute", attributeService().getValue(String.class, attributes.get(0)).getValue());
	}

	//@Test
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
		AssertJUnit.assertTrue(versioningService().getVersions(container1).isEmpty());
		AssertJUnit.assertTrue(versioningService().getVersions(room2).isEmpty());
		AssertJUnit.assertTrue(versioningService().getVersions(document1).isEmpty());
	}
}
