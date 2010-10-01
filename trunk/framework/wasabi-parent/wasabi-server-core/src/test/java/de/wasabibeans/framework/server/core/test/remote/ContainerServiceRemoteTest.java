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
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ContainerServiceRemoteTest extends WasabiRemoteTest {

	private WasabiContainerDTO container1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		container1 = testhelper.initContainerServiceTest();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1ContainerByNameTest() throws Exception {
		WasabiContainerDTO test = containerService().getContainerByName(rootRoom, "container1");
		AssertJUnit.assertEquals(container1, test);

		try {
			containerService().getContainerByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertNull(containerService().getContainerByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1EnvironmentTest() throws Exception {
		WasabiLocationDTO environment = containerService().getEnvironment(container1).getValue();
		AssertJUnit.assertEquals(rootRoom, environment);
	}

	@Test
	public void get1ContainersTest() throws Exception {
		Vector<WasabiContainerDTO> containers = containerService().getContainers(rootRoom);
		AssertJUnit.assertTrue(containers.contains(container1));
		AssertJUnit.assertEquals(3, containers.size());
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void createTest() throws Exception {
		WasabiContainerDTO container = containerService().create("container4", rootRoom);
		AssertJUnit.assertNotNull(container);
		AssertJUnit.assertEquals(container, containerService().getContainerByName(rootRoom, "container4"));

		try {
			containerService().create(null, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			containerService().create("test", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			containerService().create("container1", rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		containerService().create("container1", room);

		try {
			containerService().move(container1, room, null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiContainerDTO> containersOfRoot = containerService().getContainers(rootRoom);
			AssertJUnit.assertTrue(containersOfRoot.contains(container1));
			AssertJUnit.assertEquals(3, containersOfRoot.size());
			AssertJUnit.assertEquals(1, containerService().getContainers(room).size());
		}

		WasabiContainerDTO container2 = containerService().getContainerByName(rootRoom, "container2");
		containerService().move(container2, room, null);
		Vector<WasabiContainerDTO> containersOfRoot = containerService().getContainers(rootRoom);
		AssertJUnit.assertFalse(containersOfRoot.contains(container2));
		AssertJUnit.assertEquals(2, containersOfRoot.size());
		Vector<WasabiContainerDTO> containersOfRoom = containerService().getContainers(room);
		AssertJUnit.assertTrue(containersOfRoom.contains(container2));
		AssertJUnit.assertEquals(2, containersOfRoom.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws Exception {
		containerService().remove(container1, null);
		Vector<WasabiContainerDTO> containers = containerService().getContainers(rootRoom);
		AssertJUnit.assertFalse(containers.contains(container1));
		AssertJUnit.assertEquals(2, containers.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		WasabiContainerDTO container2 = containerService().getContainerByName(rootRoom, "container2");

		try {
			containerService().rename(container2, "container1", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(containerService().getContainerByName(rootRoom, "container2"));
			AssertJUnit.assertEquals(3, containerService().getContainers(rootRoom).size());
		}

		containerService().rename(container2, "container_2", null);
		AssertJUnit.assertEquals("container_2", containerService().getName(container2).getValue());
		AssertJUnit.assertNotNull(containerService().getContainerByName(rootRoom, "container_2"));
		AssertJUnit.assertEquals(3, containerService().getContainers(rootRoom).size());
		AssertJUnit.assertNull(containerService().getContainerByName(rootRoom, "container2"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByCreationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 containers with 5 different timestamps (before, begin, in-between, end, after)
		WasabiContainerDTO[] containers = new WasabiContainerDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			containers[i] = containerService().create("container" + i, room);
			objectService().setCreatedOn(containers[i], dates[i], null);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiContainerDTO> result = containerService().getContainersByCreationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(containers[0]));
		AssertJUnit.assertFalse(result.contains(containers[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByCreationDateDepthTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiContainerDTO[][] containers = new WasabiContainerDTO[5][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					containers[d][t] = containerService().create("container" + t + d, room);
				} else {
					containers[d][t] = containerService().create("container" + t + d,
							containers[d - 1][(int) (Math.random() * 5)]);
				}
				objectService().setCreatedOn(containers[d][t], dates[t], null);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiContainerDTO> result = containerService().getContainersByCreationDate(room, dates[1], dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(containers[d][0]));
			AssertJUnit.assertFalse(result.contains(containers[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(containers[4][t]));
		}

		// test for layer 0 only
		result = containerService().getContainersByCreationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(containers[0][0]));
		AssertJUnit.assertFalse(result.contains(containers[0][4]));

		// test for all 5 layers
		result = containerService().getContainersByCreationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(containers[d][0]));
			AssertJUnit.assertFalse(result.contains(containers[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByCreatorTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create containers that should not be returned
		WasabiContainerDTO container1ThisUser = containerService().create("container1ThisUser", rootRoom);
		WasabiContainerDTO container2ThisUser = containerService().create("container2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another container to be found
		containerService().create("anotherContainerOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get containers created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiContainerDTO> result = containerService().getContainersByCreator(root);
		AssertJUnit.assertEquals(4, result.size());
		AssertJUnit.assertFalse(result.contains(container1ThisUser));
		AssertJUnit.assertFalse(result.contains(container2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByCreatorEnvironmentTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a container that should not be returned (wrong creator)
		WasabiContainerDTO container1ThisUser = containerService().create("container1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another container that should not be returned (correct creator, but wrong location)
		containerService().create("anotherContainerOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get containers created by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiContainerDTO> result = containerService().getContainersByCreator(root, rootRoom);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(container1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByModificationDateTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// create 5 containers with 5 different timestamps (before, begin, in-between, end, after)
		WasabiContainerDTO[] containers = new WasabiContainerDTO[5];
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int i = 0; i < 5; i++) {
			dates[i] = cal.getTime();
			containers[i] = containerService().create("container" + i, room);
			objectService().setModifiedOn(containers[i], dates[i], null);

			cal.add(Calendar.MILLISECOND, 1);
		}

		// do the test
		Vector<WasabiContainerDTO> result = containerService()
				.getContainersByModificationDate(room, dates[1], dates[3]);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(containers[0]));
		AssertJUnit.assertFalse(result.contains(containers[4]));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByModificationDateDepthTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);

		// 5 timestamps (before, start, in-between, end, after)
		Calendar cal = Calendar.getInstance();
		Date[] dates = new Date[5];
		for (int t = 0; t < 5; t++) {
			dates[t] = cal.getTime();
			cal.add(Calendar.MILLISECOND, 1);
		}

		// tree with 5 sub-layers: room -> layer 0 (5 nodes, 5 timestamps) -> layer 1 (5 nodes, 5 timestamps) -> etc.
		WasabiContainerDTO[][] containers = new WasabiContainerDTO[5][5];
		for (int d = 0; d < 5; d++) {
			for (int t = 0; t < 5; t++) {
				if (d == 0) {
					containers[d][t] = containerService().create("container" + t, room);
				} else {
					containers[d][t] = containerService().create("container" + t,
							containers[d - 1][(int) (Math.random() * 5)]);
				}
				objectService().setModifiedOn(containers[d][t], dates[t], null);
			}
		}

		// test for layers 0, 1, 2, 3
		Vector<WasabiContainerDTO> result = containerService().getContainersByModificationDate(room, dates[1],
				dates[3], 3);
		AssertJUnit.assertEquals(12, result.size());
		for (int d = 0; d < 4; d++) {
			AssertJUnit.assertFalse(result.contains(containers[d][0]));
			AssertJUnit.assertFalse(result.contains(containers[d][4]));
		}
		for (int t = 0; t < 5; t++) {
			AssertJUnit.assertFalse(result.contains(containers[4][t]));
		}

		// test for layer 0 only
		result = containerService().getContainersByModificationDate(room, dates[1], dates[3], 0);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(containers[0][0]));
		AssertJUnit.assertFalse(result.contains(containers[0][4]));

		// test for all 5 layers
		result = containerService().getContainersByModificationDate(room, dates[1], dates[3], -1);
		AssertJUnit.assertEquals(15, result.size());
		for (int d = 0; d < 5; d++) {
			AssertJUnit.assertFalse(result.contains(containers[d][0]));
			AssertJUnit.assertFalse(result.contains(containers[d][4]));
		}
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByModifierTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create containers that should not be returned
		WasabiContainerDTO container1ThisUser = containerService().create("container1ThisUser", rootRoom);
		WasabiContainerDTO container2ThisUser = containerService().create("container2ThisUser", room);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another container to be found
		containerService().create("anotherContainerOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get containers modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiContainerDTO> result = containerService().getContainersByModifier(root);
		AssertJUnit.assertEquals(4, result.size());
		AssertJUnit.assertFalse(result.contains(container1ThisUser));
		AssertJUnit.assertFalse(result.contains(container2ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByModifierEnvironmentTest() throws Exception {
		WasabiRoomDTO room = roomService().create("room", rootRoom);
		// create a container that should not be returned (wrong modifier)
		WasabiContainerDTO container1ThisUser = containerService().create("container1ThisUser", rootRoom);
		reWaCon.logout();

		reWaCon.defaultLogin();
		// create another container that should not be returned (correct modifier, but wrong location)
		containerService().create("anotherContainerOfRoot", room);
		reWaCon.logout();

		reWaCon.login("user", "user");
		// get containers modified by root
		WasabiUserDTO root = userService().getUserByName(WasabiConstants.ROOT_USER_NAME);
		Vector<WasabiContainerDTO> result = containerService().getContainersByModifier(root, rootRoom);
		AssertJUnit.assertEquals(3, result.size());
		AssertJUnit.assertFalse(result.contains(container1ThisUser));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void getContainersByNamePatternTest() throws Exception {
		// create some containers for pattern matching
		WasabiContainerDTO containerMatch1 = containerService().create("dhjllhl", rootRoom);
		WasabiContainerDTO containerMatch2 = containerService().create("e", rootRoom);
		containerService().create("cadsf", rootRoom);

		// test a simple regex
		String pattern = "[^abc].*";
		Vector<WasabiContainerDTO> result = containerService().getContainersByNamePattern(rootRoom, pattern);
		AssertJUnit.assertEquals(2, result.size());
		AssertJUnit.assertTrue(result.contains(containerMatch1));
		AssertJUnit.assertTrue(result.contains(containerMatch2));
	}
}
