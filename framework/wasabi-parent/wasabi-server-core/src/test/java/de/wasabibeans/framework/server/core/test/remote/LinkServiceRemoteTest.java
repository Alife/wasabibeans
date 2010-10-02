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

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class LinkServiceRemoteTest extends WasabiRemoteTest {

	private Long optLockId = -1L;
	private WasabiLinkDTO link1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		link1 = testhelper.initLinkServiceTest();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1DestinationTest() throws Exception {
		WasabiValueDTO test = linkService().getDestination(link1);
		AssertJUnit.assertEquals(rootRoom, test.getValue());
	}

	@Test
	public void get1EnvironmentTest() throws Exception {
		WasabiValueDTO test = linkService().getEnvironment(link1);
		AssertJUnit.assertEquals(rootRoom, test.getValue());
	}

	@Test
	public void get1LinkByNameTest() throws Exception {
		WasabiLinkDTO test = linkService().getLinkByName(rootRoom, "link1");
		AssertJUnit.assertEquals(link1, test);

		try {
			linkService().getLinkByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertNull(linkService().getLinkByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1LinksTest() throws Exception {
		Vector<WasabiLinkDTO> links = linkService().getLinks(rootRoom);
		AssertJUnit.assertTrue(links.contains(link1));
		AssertJUnit.assertEquals(2, links.size());
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void createTest() throws Exception {
		WasabiLinkDTO link3 = linkService().create("link3", link1, rootRoom);
		AssertJUnit.assertNotNull(link3);
		AssertJUnit.assertEquals(link3, linkService().getLinkByName(rootRoom, "link3"));
		AssertJUnit.assertEquals(link1, linkService().getDestination(link3).getValue());

		try {
			linkService().create(null, link1, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			linkService().create("test", link1, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			linkService().create("test", null, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			linkService().create("link3", link1, rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}

	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiContainerDTO newEnvironment = containerService().create("newEnvironment", rootRoom);
		linkService().create("link1", rootRoom, newEnvironment);

		try {
			linkService().move(link1, newEnvironment, optLockId);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiLinkDTO> linksOfRoot = linkService().getLinks(rootRoom);
			AssertJUnit.assertTrue(linksOfRoot.contains(link1));
			AssertJUnit.assertEquals(2, linksOfRoot.size());
			AssertJUnit.assertEquals(1, linkService().getLinks(newEnvironment).size());
		}

		WasabiLinkDTO link2 = linkService().getLinkByName(rootRoom, "link2");
		linkService().move(link2, newEnvironment, optLockId);
		Vector<WasabiLinkDTO> linksOfRoot = linkService().getLinks(rootRoom);
		AssertJUnit.assertFalse(linksOfRoot.contains(link2));
		AssertJUnit.assertEquals(1, linksOfRoot.size());
		Vector<WasabiLinkDTO> linksOfNewEnvironment = linkService().getLinks(newEnvironment);
		AssertJUnit.assertTrue(linksOfNewEnvironment.contains(link2));
		AssertJUnit.assertEquals(2, linksOfNewEnvironment.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws Exception {
		linkService().remove(link1, optLockId);
		Vector<WasabiLinkDTO> links = linkService().getLinks(rootRoom);
		AssertJUnit.assertFalse(links.contains(link1));
		AssertJUnit.assertEquals(1, links.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		try {
			linkService().rename(link1, "link2", optLockId);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(linkService().getLinkByName(rootRoom, "link1"));
			AssertJUnit.assertEquals(2, linkService().getLinks(rootRoom).size());
		}

		try {
			linkService().rename(link1, null, optLockId);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		linkService().rename(link1, "link_2", optLockId);
		AssertJUnit.assertEquals("link_2", linkService().getName(link1).getValue());
		AssertJUnit.assertNotNull(linkService().getLinkByName(rootRoom, "link_2"));
		AssertJUnit.assertEquals(2, linkService().getLinks(rootRoom).size());
		AssertJUnit.assertNull(linkService().getLinkByName(rootRoom, "link1"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setDestinationTest() throws Exception {
		WasabiDocumentDTO document = documentService().create("document", rootRoom);
		linkService().setDestination(link1, document, optLockId);
		AssertJUnit.assertEquals(document, linkService().getDestination(link1).getValue());

		// test null value
		linkService().setDestination(link1, null, optLockId);
		AssertJUnit.assertNull(linkService().getDestination(link1).getValue());

		// test whether a new value can be set after null
		linkService().setDestination(link1, rootRoom, optLockId);
		AssertJUnit.assertEquals(rootRoom, linkService().getDestination(link1).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 link with 5 different timestamps (before, begin, in-between, end, after)
		WasabiLinkDTO[] links = new WasabiLinkDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			links[i] = linkService().create("link" + i, rootRoom, room);
			objectService().setCreatedOn(links[i], dates[i], optLockId);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiLinkDTO> result = linkService().getLinksByCreationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(links[0]));
		AssertJUnit.assertFalse(result.contains(links[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByCreationDateDepthTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiLinkDTO[][] links = new WasabiLinkDTO[5][5];
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[4][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					links[d][t] = linkService().create("link" + t, rootRoom, room);
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					links[d][t] = linkService().create("link" + t, rootRoom, rooms[d - 1][(int) (Math.random() * 5)]);
					if (d != 4) {
						rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					}
				}
				objectService().setCreatedOn(links[d][t], dates[t], optLockId);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiLinkDTO> result = linkService().getLinksByCreationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(links[d][0]));
			AssertJUnit.assertFalse(result.contains(links[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(links[4][t]));
		}

		// test for layer 0 only
		result = linkService().getLinksByCreationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(links[0][0]));
		AssertJUnit.assertFalse(result.contains(links[0][4]));

		// test for all 5 layers
		result = linkService().getLinksByCreationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(links[d][0]));
			AssertJUnit.assertFalse(result.contains(links[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByCreatorTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create links that should not be returned
		WasabiLinkDTO link1ThisUser = linkService().create("link1ThisUser", rootRoom, rootRoom);
		WasabiLinkDTO link2ThisUser = linkService().create("link2ThisUser", rootRoom, room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another link to be found
		linkService().create("anotherLinkOfRoot", rootRoom, room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get link created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiLinkDTO> result = linkService().getLinksByCreator(root);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(link1ThisUser));
		AssertJUnit.assertFalse(result.contains(link2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByCreatorTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a link that should not be returned (wrong creator)
		WasabiLinkDTO link1ThisUser = linkService().create("link1ThisUser", rootRoom, rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another link that should not be returned (correct creator, but wrong location)
		linkService().create("anotherLinkOfRoot", rootRoom, room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get links created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiLinkDTO> result = linkService().getLinksByCreator(root, rootRoom);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertFalse(result.contains(link1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByModificationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 link with 5 different timestamps (before, begin, in-between, end, after)
		WasabiLinkDTO[] links = new WasabiLinkDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			links[i] = linkService().create("link" + i, rootRoom, room);
			objectService().setModifiedOn(links[i], dates[i], optLockId);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiLinkDTO> result = linkService().getLinksByModificationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(links[0]));
		AssertJUnit.assertFalse(result.contains(links[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByModificationDateDepthTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiLinkDTO[][] links = new WasabiLinkDTO[5][5];
		WasabiRoomDTO[][] rooms = new WasabiRoomDTO[4][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					links[d][t] = linkService().create("link" + t, rootRoom, room);
					rooms[d][t] = roomService().create("room" + t, room);
				} else {
					links[d][t] = linkService().create("link" + t, rootRoom, rooms[d - 1][(int) (Math.random() * 5)]);
					if (d != 4) {
						rooms[d][t] = roomService().create("room" + t, rooms[d - 1][(int) (Math.random() * 5)]);
					}
				}
				objectService().setModifiedOn(links[d][t], dates[t], optLockId);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiLinkDTO> result = linkService().getLinksByModificationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(links[d][0]));
			AssertJUnit.assertFalse(result.contains(links[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(links[4][t]));
		}

		// test for layer 0 only
		result = linkService().getLinksByModificationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(links[0][0]));
		AssertJUnit.assertFalse(result.contains(links[0][4]));

		// test for all 5 layers
		result = linkService().getLinksByModificationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(links[d][0]));
			AssertJUnit.assertFalse(result.contains(links[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByModifierTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create links that should not be returned
		WasabiLinkDTO link1ThisUser = linkService().create("link1ThisUser", rootRoom, rootRoom);
		WasabiLinkDTO link2ThisUser = linkService().create("link2ThisUser", rootRoom, room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another link to be found
		linkService().create("anotherLinkOfRoot", rootRoom, room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get link modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiLinkDTO> result = linkService().getLinksByModifier(root);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(link1ThisUser));
		AssertJUnit.assertFalse(result.contains(link2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksByModifierTestEnvironment() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a link that should not be returned (wrong creator)
		WasabiLinkDTO link1ThisUser = linkService().create("link1ThisUser", rootRoom, rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another link that should not be returned (correct creator, but wrong location)
		linkService().create("anotherLinkOfRoot", rootRoom, room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get links modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiLinkDTO> result = linkService().getLinksByModifier(root, rootRoom);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertFalse(result.contains(link1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getLinksOrderedByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 links with 5 different timestamps
		WasabiLinkDTO[] links = new WasabiLinkDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			links[i] = linkService().create("link" + i, rootRoom, room);
			objectService().setCreatedOn(links[i], dates[i], optLockId);

			cal.add(Calendar.SECOND, 1);
		}

		// do the test
		Vector<WasabiLinkDTO> result = linkService().getLinksOrderedByCreationDate(room, SortType.DESCENDING);
		for (int i = 0; i < 5; i++) {
			AssertJUnit.assertEquals(links[4 - i], result.get(i));
		}
		result = linkService().getLinksOrderedByCreationDate(room, SortType.ASCENDING);
		for (int i = 0; i < 5; i++) {
			AssertJUnit.assertEquals(links[i], result.get(i));
		}
	}
}
