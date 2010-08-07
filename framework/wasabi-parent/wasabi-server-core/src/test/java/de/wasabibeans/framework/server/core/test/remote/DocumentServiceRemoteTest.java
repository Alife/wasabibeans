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
import java.util.Vector;

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class DocumentServiceRemoteTest extends WasabiRemoteTest {

	private WasabiDocumentDTO document1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		reWaCon.defaultLogin();
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		rootRoom = testhelper.initRepository();
		testhelper.initDatabase();
		testhelper.initTestUser();
		document1 = testhelper.initDocumentServiceTest();
		reWaCon.logout();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void getDocumentByNameTest() throws Exception {
		WasabiDocumentDTO test = documentService().getDocumentByName(rootRoom, "document1");
		AssertJUnit.assertEquals(document1, test);

		try {
			documentService().getDocumentByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		AssertJUnit.assertNull(documentService().getDocumentByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void getContentTest() throws Exception {
		Serializable content = documentService().getContent(document1);
		AssertJUnit.assertEquals("document1", content);
	}

	@Test
	public void getEnvironmentTest() throws Exception {
		WasabiLocationDTO environment = documentService().getEnvironment(document1);
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test
	public void getDocumentsTest() throws Exception {
		Vector<WasabiDocumentDTO> documents = documentService().getDocuments(rootRoom);
		AssertJUnit.assertTrue(documents.contains(document1));
		AssertJUnit.assertEquals(2, documents.size());
	}

	@Test(dependsOnMethods = { ".*get.*" })
	public void createTest() throws Exception {
		WasabiDocumentDTO newDocument = documentService().create("document3", rootRoom);
		AssertJUnit.assertNotNull(newDocument);
		AssertJUnit.assertEquals(newDocument, documentService().getDocumentByName(rootRoom, "document3"));

		try {
			documentService().create(null, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			documentService().create("test", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
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
		documentService().setContent(document, fileBytes);

		byte[] loadedBytes = (byte[]) documentService().getContent(document);
		AssertJUnit.assertEquals(fileBytes, loadedBytes);

		documentService().setContent(document, "Hallo");
		AssertJUnit.assertEquals("Hallo", documentService().getContent(document));

		try {
			documentService().setContent(null, "Hallo");
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}
		
		documentService().setContent(document, null);
		AssertJUnit.assertNull(documentService().getContent(document));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiRoomDTO newLocation = roomService().create("newLocation", rootRoom);
		documentService().create("document1", newLocation);

		try {
			documentService().move(document1, newLocation);
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiDocumentDTO> documentsOfRoot = documentService().getDocuments(rootRoom);
			AssertJUnit.assertTrue(documentsOfRoot.contains(document1));
			AssertJUnit.assertEquals(2, documentsOfRoot.size());
			AssertJUnit.assertEquals(1, documentService().getDocuments(newLocation).size());
		}

		WasabiDocumentDTO document2 = documentService().getDocumentByName(rootRoom, "document2");
		documentService().move(document2, newLocation);
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
			documentService().rename(document1, "document2");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(documentService().getDocumentByName(rootRoom, "document1"));
			AssertJUnit.assertEquals(2, documentService().getDocuments(rootRoom).size());
		}

		documentService().rename(document1, "document_2");
		AssertJUnit.assertEquals("document_2", documentService().getName(document1));
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

}
