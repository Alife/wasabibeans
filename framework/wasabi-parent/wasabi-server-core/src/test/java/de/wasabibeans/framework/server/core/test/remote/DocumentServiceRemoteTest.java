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

package de.wasabibeans.framework.server.core.test.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class DocumentServiceRemoteTest extends WasabiRemoteTest {

	private WasabiDocumentDTO document1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();
		document1 = testhelper.initDocumentServiceTest();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1DocumentByNameTest() throws Exception {
		WasabiDocumentDTO test = documentService().getDocumentByName(rootRoom, "document1");
		AssertJUnit.assertEquals(document1, test);

		try {
			documentService().getDocumentByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		AssertJUnit.assertNull(documentService().getDocumentByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1ContentTest() throws Exception {
		Serializable content = documentService().getContent(document1).getValue();
		AssertJUnit.assertEquals("document1", content);
	}

	@Test
	public void get1EnvironmentTest() throws Exception {
		WasabiLocationDTO environment = documentService().getEnvironment(document1).getValue();
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test
	public void get1DocumentsTest() throws Exception {
		Vector<WasabiDocumentDTO> documents = documentService().getDocuments(rootRoom);
		AssertJUnit.assertTrue(documents.contains(document1));
		AssertJUnit.assertEquals(2, documents.size());
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void createTest() throws Exception {
		WasabiDocumentDTO newDocument = documentService().create("document3", rootRoom);
		AssertJUnit.assertNotNull(newDocument);
		AssertJUnit.assertEquals(newDocument, documentService().getDocumentByName(rootRoom, "document3"));

		try {
			documentService().create(null, rootRoom);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			documentService().create("test", null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		try {
			documentService().create("document3", rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setContentTest() throws Exception {
		WasabiDocumentDTO document = documentService().create("test", rootRoom);

		File file = new File("src/test/resources/testfile.txt");
		byte[] fileBytes = new byte[(int) file.length()];
		FileInputStream in = new FileInputStream(file);
		in.read(fileBytes);
		in.close();
		documentService().setContent(document, fileBytes, null);

		byte[] loadedBytes = documentService().getContent(document).getValue();
		AssertJUnit.assertEquals(fileBytes, loadedBytes);

		documentService().setContent(document, "Hallo", null);
		AssertJUnit.assertEquals("Hallo", documentService().getContent(document).getValue());

		try {
			documentService().setContent(null, "Hallo", null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		documentService().setContent(document, null, null);
		AssertJUnit.assertNull(documentService().getContent(document).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiRoomDTO newLocation = roomService().create("newLocation", rootRoom);
		documentService().create("document1", newLocation);

		try {
			documentService().move(document1, newLocation, null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiDocumentDTO> documentsOfRoot = documentService().getDocuments(rootRoom);
			AssertJUnit.assertTrue(documentsOfRoot.contains(document1));
			AssertJUnit.assertEquals(2, documentsOfRoot.size());
			AssertJUnit.assertEquals(1, documentService().getDocuments(newLocation).size());
		}

		WasabiDocumentDTO document2 = documentService().getDocumentByName(rootRoom, "document2");
		documentService().move(document2, newLocation, null);
		Vector<WasabiDocumentDTO> documentsOfRoot = documentService().getDocuments(rootRoom);
		AssertJUnit.assertFalse(documentsOfRoot.contains(document2));
		AssertJUnit.assertEquals(1, documentsOfRoot.size());
		Vector<WasabiDocumentDTO> documentsOfNewLocation = documentService().getDocuments(newLocation);
		AssertJUnit.assertTrue(documentsOfNewLocation.contains(document2));
		AssertJUnit.assertEquals(2, documentsOfNewLocation.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		try {
			documentService().rename(document1, "document2", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(documentService().getDocumentByName(rootRoom, "document1"));
			AssertJUnit.assertEquals(2, documentService().getDocuments(rootRoom).size());
		}

		try {
			documentService().rename(document1, null, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		documentService().rename(document1, "document_2", null);
		AssertJUnit.assertEquals("document_2", documentService().getName(document1).getValue());
		AssertJUnit.assertNotNull(documentService().getDocumentByName(rootRoom, "document_2"));
		AssertJUnit.assertEquals(2, documentService().getDocuments(rootRoom).size());
		AssertJUnit.assertNull(documentService().getDocumentByName(rootRoom, "document1"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws Exception {
		documentService().remove(document1);
		Vector<WasabiDocumentDTO> documents = documentService().getDocuments(rootRoom);
		AssertJUnit.assertFalse(documents.contains(document1));
		AssertJUnit.assertEquals(1, documents.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 documents with 5 different timestamps (before, begin, in-between, end, after)
		WasabiDocumentDTO[] documents = new WasabiDocumentDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			documents[i] = documentService().create("link" + i, room);
			objectService().setCreatedOn(documents[i], dates[i], null);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByCreationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(documents[0]));
		AssertJUnit.assertFalse(result.contains(documents[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByCreationDateTestDepth() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiDocumentDTO[][] documents = new WasabiDocumentDTO[5][5];
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[4][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					documents[d][t] = documentService().create("document" + t, room);
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					documents[d][t] = documentService().create("document" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					if (d != 4) {
						rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					}
				}
				objectService().setCreatedOn(documents[d][t], dates[t], null);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByCreationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(documents[d][0]));
			AssertJUnit.assertFalse(result.contains(documents[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(documents[4][t]));
		}

		// test for layer 0 only
		result = documentService().getDocumentsByCreationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(documents[0][0]));
		AssertJUnit.assertFalse(result.contains(documents[0][4]));

		// test for all 5 layers
		result = documentService().getDocumentsByCreationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(documents[d][0]));
			AssertJUnit.assertFalse(result.contains(documents[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByCreatorTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create document that should not be returned
		WasabiDocumentDTO document1ThisUser = documentService().create("document1ThisUser", rootRoom);
		WasabiDocumentDTO document2ThisUser = documentService().create("document2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another document to be found
		documentService().create("anotherDocumentOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get documents modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByCreator(root);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(document1ThisUser));
		AssertJUnit.assertFalse(result.contains(document2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByCreatorTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a document that should not be returned (wrong creator)
		WasabiDocumentDTO document1ThisUser = documentService().create("document1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another document that should not be returned (correct creator, but wrong location)
		documentService().create("anotherDocumentOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get documents modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByCreator(root, rootRoom);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertFalse(result.contains(document1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByModificationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 documents with 5 different timestamps (before, begin, in-between, end, after)
		WasabiDocumentDTO[] documents = new WasabiDocumentDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			documents[i] = documentService().create("link" + i, room);
			objectService().setModifiedOn(documents[i], dates[i], null);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByModificationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(documents[0]));
		AssertJUnit.assertFalse(result.contains(documents[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByModificationDateTestDepth() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiDocumentDTO[][] documents = new WasabiDocumentDTO[5][5];
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[4][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					documents[d][t] = documentService().create("document" + t, room);
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					documents[d][t] = documentService().create("document" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					if (d != 4) {
						rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					}
				}
				objectService().setModifiedOn(documents[d][t], dates[t], null);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiDocumentDTO> result = documentService()
				.getDocumentsByModificationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(documents[d][0]));
			AssertJUnit.assertFalse(result.contains(documents[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(documents[4][t]));
		}

		// test for layer 0 only
		result = documentService().getDocumentsByModificationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(documents[0][0]));
		AssertJUnit.assertFalse(result.contains(documents[0][4]));

		// test for all 5 layers
		result = documentService().getDocumentsByModificationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(documents[d][0]));
			AssertJUnit.assertFalse(result.contains(documents[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByModifierTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create document that should not be returned
		WasabiDocumentDTO document1ThisUser = documentService().create("document1ThisUser", rootRoom);
		WasabiDocumentDTO document2ThisUser = documentService().create("document2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another document to be found
		documentService().create("anotherDocumentOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get documents modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByModifier(root);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(document1ThisUser));
		AssertJUnit.assertFalse(result.contains(document2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsByModifierTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a document that should not be returned (wrong creator)
		WasabiDocumentDTO document1ThisUser = documentService().create("document1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another document that should not be returned (correct creator, but wrong location)
		documentService().create("anotherDocumentOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get documents modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsByModifier(root, rootRoom);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertFalse(result.contains(document1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getDocumentsOrderedByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 document with 5 different timestamps
		WasabiDocumentDTO[] documents = new WasabiDocumentDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			documents[i] = documentService().create("document" + i, room);
			objectService().setCreatedOn(documents[i], dates[i], null);

			cal.add(Calendar.SECOND, 1);
		}

		// do the test
		Vector<WasabiDocumentDTO> result = documentService().getDocumentsOrderedByCreationDate(room,
				SortType.DESCENDING);
		for (int i = 0; i < 5; i++) {
			AssertJUnit.assertEquals(documents[4 - i], result.get(i));
		}
		result = documentService().getDocumentsOrderedByCreationDate(room, SortType.ASCENDING);
		for (int i = 0; i < 5; i++) {
			AssertJUnit.assertEquals(documents[i], result.get(i));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void hasDocumentsCreatedAfterTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO document = documentService().create("test", room);

		Calendar cal = Calendar.getInstance();
		objectService().setCreatedOn(document, cal.getTime(), null);

		AssertJUnit.assertTrue(documentService().hasDocumentsCreatedAfter(room, cal.getTimeInMillis()));

		cal.add(Calendar.MILLISECOND, 1);
		AssertJUnit.assertFalse(documentService().hasDocumentsCreatedAfter(room, cal.getTimeInMillis()));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void hasDocumentsCreatedBeforeTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO document = documentService().create("test", room);

		Calendar cal = Calendar.getInstance();
		objectService().setCreatedOn(document, cal.getTime(), null);

		AssertJUnit.assertTrue(documentService().hasDocumentsCreatedBefore(room, cal.getTimeInMillis()));

		cal.add(Calendar.MILLISECOND, -1);
		AssertJUnit.assertFalse(documentService().hasDocumentsCreatedBefore(room, cal.getTimeInMillis()));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void hasDocumentsModifiedAfterTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO document = documentService().create("test", room);

		Calendar cal = Calendar.getInstance();
		objectService().setModifiedOn(document, cal.getTime(), null);

		AssertJUnit.assertTrue(documentService().hasDocumentsModifiedAfter(room, cal.getTimeInMillis()));

		cal.add(Calendar.MILLISECOND, 1);
		AssertJUnit.assertFalse(documentService().hasDocumentsModifiedAfter(room, cal.getTimeInMillis()));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void hasDocumentsModifiedBeforeTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO document = documentService().create("test", room);

		Calendar cal = Calendar.getInstance();
		objectService().setModifiedOn(document, cal.getTime(), null);

		AssertJUnit.assertTrue(documentService().hasDocumentsModifiedBefore(room, cal.getTimeInMillis()));

		cal.add(Calendar.MILLISECOND, -1);
		AssertJUnit.assertFalse(documentService().hasDocumentsModifiedBefore(room, cal.getTimeInMillis()));
	}
}
