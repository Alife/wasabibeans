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

import java.util.Vector;

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
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ObjectServiceRemoteTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void existsTest() throws Exception {
		WasabiContainerDTO container = containerService().create("container", rootRoom);
		AssertJUnit.assertTrue(objectService().exists(container));

		containerService().remove(container);
		AssertJUnit.assertFalse(objectService().exists(container));
	}

	@Test
	public void getObjectsByAttributeNameTest() throws Exception {
		try {
			objectService().getObjectsByAttributeName(null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		// create some objects with attributes
		WasabiContainerDTO container = containerService().create("container", rootRoom);
		WasabiDocumentDTO document = documentService().create("document", container);
		WasabiLinkDTO link = linkService().create("link", document, container);
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiGroupDTO group = groupService().create("group", null);
		WasabiUserDTO user = userService().create("user2", "user2");

		attributeService().create("name", 0, container);
		attributeService().create("name", 0, document);
		attributeService().create("notName", 0, link);
		attributeService().create("notName", 0, room);
		attributeService().create("name", 0, group);
		attributeService().create("notName", 0, user);

		// do the test
		Vector<WasabiObjectDTO> result = objectService().getObjectsByAttributeName("name");
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertTrue(result.contains(container) && result.contains(document) && result.contains(group));
	}

	@Test
	public void getObjectsByCreatorTest() throws Exception {
		WasabiUserDTO user1 = userService().create("user1", "user1");
		reWaCon.logout();

		reWaCon.login("user1", "user1");
		// create some objects that should be found
		WasabiContainerDTO container = containerService().create("container", rootRoom);
		WasabiDocumentDTO document = documentService().create("document", container);
		WasabiLinkDTO link = linkService().create("link", document, container);
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiGroupDTO group = groupService().create("group", null);
		WasabiUserDTO user = userService().create("user2", "user2");
		reWaCon.logout();

		reWaCon.login("user", "user");
		// create another object that sould not be found
		documentService().create("anotherDocument", container);

		// do the test
		Vector<WasabiObjectDTO> result = objectService().getObjectsByCreator(user1);
		// 6 obviously created by this method + the home-room of the user 'user2' created by this method
		AssertJUnit.assertEquals(7, result.size());
		AssertJUnit.assertTrue(result.contains(container) && result.contains(document) && result.contains(link)
				&& result.contains(room) && result.contains(group) && result.contains(user));
	}

	@Test
	public void getObjectsByModifierTest() throws Exception {
		WasabiUserDTO user1 = userService().create("user1", "user1");
		reWaCon.logout();

		reWaCon.login("user1", "user1");
		// create some objects that should be found
		WasabiContainerDTO container = containerService().create("container", rootRoom);
		WasabiDocumentDTO document = documentService().create("document", container);
		WasabiLinkDTO link = linkService().create("link", document, container);
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		WasabiGroupDTO group = groupService().create("group", null);
		WasabiUserDTO user = userService().create("user2", "user2");
		reWaCon.logout();

		reWaCon.login("user", "user");
		// create another object that sould not be found
		documentService().create("anotherDocument", container);

		// do the test
		Vector<WasabiObjectDTO> result = objectService().getObjectsByModifier(user1);
		// 6 obviously created by this method + the home-room of the user 'user2' created by this method
		AssertJUnit.assertEquals(7, result.size());
		AssertJUnit.assertTrue(result.contains(container) && result.contains(document) && result.contains(link)
				&& result.contains(room) && result.contains(group) && result.contains(user));
	}

	@Test
	public void getObjectsByNameTest() throws Exception {
		try {
			objectService().getObjectsByName(null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		// create some objects
		WasabiContainerDTO container = containerService().create("name", rootRoom);
		WasabiDocumentDTO document = documentService().create("name", container);
		linkService().create("notName", document, container);
		roomService().create("notName", rootRoom);
		WasabiGroupDTO group = groupService().create("name", null);
		WasabiUserDTO user = userService().create("name", "name");

		// do the test
		Vector<WasabiObjectDTO> result = objectService().getObjectsByName("name");
		// 4 objects see above + the home-room of the user 'name'
		AssertJUnit.assertEquals(5, result.size());
		AssertJUnit.assertTrue(result.contains(container) && result.contains(document) && result.contains(group)
				&& result.contains(user));
	}
}
