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

import java.util.Map;
import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class TagServiceRemoteTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTagServiceTest();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void getTagsTest() throws Exception {
		Vector<String> tags = tagService().getTags(rootRoom);
		AssertJUnit.assertEquals(2, tags.size());
		AssertJUnit.assertTrue(tags.contains("tag1") && tags.contains("tag2"));
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void addTagTest() throws Exception {
		tagService().addTag(rootRoom, "tag3");
		Vector<String> tags = tagService().getTags(rootRoom);
		AssertJUnit.assertEquals(3, tags.size());
		AssertJUnit.assertTrue(tags.contains("tag1") && tags.contains("tag2") && tags.contains("tag3"));

		try {
			tagService().addTag(rootRoom, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		// test that adding tags with same content does not cause problems
		tagService().addTag(rootRoom, "tag4");
		tagService().addTag(rootRoom, "tag4");
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void clearTagsTest() throws Exception {
		tagService().clearTags(rootRoom);
		AssertJUnit.assertTrue(tagService().getTags(rootRoom).isEmpty());
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void getMostUsedDocumentTagsTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO doc0 = documentService().create("document0", room);
		WasabiDocumentDTO doc1 = documentService().create("document1", rootRoom);
		WasabiDocumentDTO doc2 = documentService().create("document2", rootRoom);
		WasabiDocumentDTO doc3 = documentService().create("document3", rootRoom);

		tagService().addTag(doc0, "eins");
		tagService().addTag(doc0, "zwei");
		tagService().addTag(doc1, "eins");
		tagService().addTag(doc1, "zwei");
		tagService().addTag(doc2, "eins");
		tagService().addTag(doc2, "zwei");
		tagService().addTag(doc3, "eins");

		Map<String, Integer> result = tagService().getMostUsedDocumentTags(rootRoom, 0);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertEquals(3, (int) result.get("eins"));
		AssertJUnit.assertEquals(2, (int) result.get("zwei"));
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void getObjectsByTagTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiContainerDTO container = containerService().create("container", rootRoom);
		WasabiDocumentDTO document = documentService().create("document", room);
		WasabiLinkDTO link = linkService().create("link", rootRoom, container);

		tagService().addTag(room, "eins");
		tagService().addTag(room, "zwei");
		tagService().addTag(container, "eins");
		tagService().addTag(container, "zwei");
		tagService().addTag(document, "eins");
		tagService().addTag(link, "eins");
		tagService().addTag(link, "zwei");

		Vector<WasabiObjectDTO> result = tagService().getObjectsByTag("eins");
		AssertJUnit.assertEquals(4, result.size());
		AssertJUnit.assertTrue(result.contains(room) && result.contains(container) && result.contains(document)
				&& result.contains(link));

		result = tagService().getObjectsByTag("zwei");
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertTrue(result.contains(room) && result.contains(container) && result.contains(link));

		try {
			result = tagService().getObjectsByTag(null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void removeTagTest() throws Exception {
		try {
			tagService().removeTag(rootRoom, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		tagService().removeTag(rootRoom, "noSuchTag");
		AssertJUnit.assertEquals(2, tagService().getTags(rootRoom).size());

		tagService().removeTag(rootRoom, "tag1");
		Vector<String> tags = tagService().getTags(rootRoom);
		AssertJUnit.assertEquals(1, tags.size());
		AssertJUnit.assertTrue(tags.contains("tag2"));
	}

	@Test(dependsOnMethods = { "getTagsTest" })
	public void getDocumentsByTagsTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiDocumentDTO doc0 = documentService().create("document0", room);
		WasabiDocumentDTO doc1 = documentService().create("document1", rootRoom);
		WasabiDocumentDTO doc2 = documentService().create("document2", rootRoom);
		WasabiDocumentDTO doc3 = documentService().create("document3", rootRoom);

		tagService().addTag(doc0, "eins");
		tagService().addTag(doc0, "zwei");
		tagService().addTag(doc1, "eins");
		tagService().addTag(doc1, "zwei");
		tagService().addTag(doc2, "eins");
		tagService().addTag(doc2, "zwei");
		tagService().addTag(doc3, "eins");

		try {
			tagService().getDocumentsByTags(rootRoom, null);
			AssertJUnit.fail();
		} catch (IllegalArgumentException e) {
			// passed
		}

		Vector<String> test = new Vector<String>();
		test.add("eins");
		Vector<WasabiDocumentDTO> result = tagService().getDocumentsByTags(rootRoom, test);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertTrue(result.contains(doc1) && result.contains(doc2) && result.contains(doc3));

		test.add("zwei");
		result = tagService().getDocumentsByTags(rootRoom, test);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertTrue(result.contains(doc1) && result.contains(doc2));

		test.add("drei");
		result = tagService().getDocumentsByTags(rootRoom, test);
		AssertJUnit.assertTrue(result.isEmpty());
	}
}