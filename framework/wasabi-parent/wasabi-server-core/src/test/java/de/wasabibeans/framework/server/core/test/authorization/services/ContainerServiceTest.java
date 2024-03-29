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

package de.wasabibeans.framework.server.core.test.authorization.services;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ContainerServiceTest extends WasabiRemoteTest {

	@Test
	public void createTest() throws WasabiException {
		System.out.println("=== createTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating createTestRoom at usersHome... ");
		WasabiRoomDTO createTestRoom = null;
		try {
			createTestRoom = roomService().create("createTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(createTestRoom);
		System.out.println("done.");

		aclService().remove(
				createTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Creating container at createTestRoom... ");
		try {
			containerService().create("someContainer", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating container at createTestRoom... ");
		try {
			containerService().create("someContainer", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	private void displayACLEntry(WasabiObjectDTO room, String name) throws WasabiException {
		Vector<WasabiACLEntryDTO> ACLEntries = new Vector<WasabiACLEntryDTO>();
		if (room != null) {
			ACLEntries = aclService().getAclEntries(room);

			System.out.println("---- ACL entries for object (" + name + ") " + objectService().getUUID(room) + " ----");

			for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntries) {
				System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
						+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id="
						+ wasabiACLEntryDTO.getParentId() + ",view=" + wasabiACLEntryDTO.getView() + ",read="
						+ wasabiACLEntryDTO.getRead() + ",insert=" + wasabiACLEntryDTO.getInsert() + ",execute="
						+ wasabiACLEntryDTO.getExecute() + ",write=" + wasabiACLEntryDTO.getWrite() + ",comment="
						+ wasabiACLEntryDTO.getComment() + ",grant=" + wasabiACLEntryDTO.getGrant() + ",start_time="
						+ wasabiACLEntryDTO.getStartTime() + ",end_time=" + wasabiACLEntryDTO.getEndTime()
						+ ",inheritance=" + wasabiACLEntryDTO.getInheritance() + ",inheritance_id="
						+ wasabiACLEntryDTO.getInheritanceId());
			}
		}
	}

	@Test
	public void getContainerByNameTest() throws WasabiException {
		System.out.println("=== getContainerByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getContainerByNameTestRoom at usersHome... ");
		WasabiRoomDTO getContainerByNameTestRoom = null;
		try {
			getContainerByNameTestRoom = roomService().create("getContainerByNameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getContainerByNameTestRoom... ");
		aclService().create(getContainerByNameTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getContainerByNameTestRoom... ");
		aclService().create(getContainerByNameTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getContainerByNameTestRoom... ");
		aclService().create(getContainerByNameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getContainerByNameTestRoom... ");
		aclService().deactivateInheritance(getContainerByNameTestRoom);
		System.out.println("done.");

		aclService().remove(
				getContainerByNameTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating container someContainer at getContainerByNameTestRoom... ");
		WasabiContainerDTO someContainer = null;
		try {
			someContainer = containerService().create("someContainer", getContainerByNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Using getContainerByName to get someContainer ... ");
		try {
			containerService().getContainerByName(getContainerByNameTestRoom, "someContainer");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getContainerByNameTestRoom... ");
		aclService().create(getContainerByNameTestRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Using getContainerByName to get someContainer ... ");
		try {
			containerService().getContainerByName(getContainerByNameTestRoom, "someContainer");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getContainers() throws WasabiException {
		System.out.println("=== getContainers() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getContainersRoom at usersHome... ");
		WasabiRoomDTO getContainersRoom = null;
		try {
			getContainersRoom = roomService().create("getContainersRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getContainersRoom... ");
		aclService().create(getContainersRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getContainersRoom... ");
		aclService().create(getContainersRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getContainersRoom... ");
		aclService().create(getContainersRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getContainersRoom... ");
		aclService().deactivateInheritance(getContainersRoom);
		System.out.println("done.");

		aclService().remove(
				getContainersRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating container someContainer1 at getContainersRoom... ");
		WasabiContainerDTO someContainer1 = null;
		try {
			someContainer1 = containerService().create("someContainer1", getContainersRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating container someContainer2 at getContainersRoom... ");
		WasabiContainerDTO someContainer2 = null;
		try {
			someContainer2 = containerService().create("someContainer2", getContainersRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating container someContainer3 at getContainersRoom... ");
		WasabiContainerDTO someContainer3 = null;
		try {
			someContainer3 = containerService().create("someContainer3", getContainersRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Display all conterains with VIEW at getContainersRoom");
		try {
			Vector<WasabiContainerDTO> containers = containerService().getContainers(getContainersRoom);
			for (WasabiContainerDTO wasabiContainerDTO : containers) {
				System.out.println(objectService().getName(wasabiContainerDTO).getValue());
			}
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for someContainer1... ");
		aclService().create(someContainer1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for someContainer3... ");
		aclService().create(someContainer3, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Display all conterains with VIEW at getContainersRoom");
		try {
			Vector<WasabiContainerDTO> containers = containerService().getContainers(getContainersRoom);
			for (WasabiContainerDTO wasabiContainerDTO : containers) {
				System.out.println(objectService().getName(wasabiContainerDTO).getValue());
			}
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void moveTest() throws WasabiException {
		System.out.println("=== moveTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating moveTestRoom1 at usersHome... ");
		WasabiRoomDTO moveTestRoom1 = null;
		try {
			moveTestRoom1 = roomService().create("moveTestRoom1", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating moveTestRoom2 at usersHome... ");
		WasabiRoomDTO moveTestRoom2 = null;
		try {
			moveTestRoom2 = roomService().create("moveTestRoom2", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting GRANT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for moveTestRoom1... ");
		aclService().deactivateInheritance(moveTestRoom1);
		System.out.println("done.");

		aclService().remove(
				moveTestRoom1,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Deactivating inheritance for moveTestRoom2... ");
		aclService().deactivateInheritance(moveTestRoom2);
		System.out.println("done.");

		aclService().remove(
				moveTestRoom2,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Setting INSERT as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting EXECUTE as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		displayACLEntry(moveTestRoom1, "moveTestRoom1");
		displayACLEntry(moveTestRoom2, "moveTestRoom2");

		System.out.print("Creating moveContainer at moveTestRoom1... ");
		WasabiContainerDTO moveContainer = null;
		try {
			moveContainer = containerService().create("moveContainer", moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someContainer at moveContainer... ");
		WasabiContainerDTO someContainer = null;
		try {
			someContainer = containerService().create("someContainer", moveContainer);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting COMMENT as userRight for moveContainer... ");
		aclService().create(moveContainer, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		displayACLEntry(moveContainer, "moveContainer");
		displayACLEntry(someContainer, "someContainer");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			containerService().move(moveContainer, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			containerService().move(moveContainer, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting WRITE as forbiddance userRight for someContainer... ");
		aclService().create(someContainer, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		displayACLEntry(moveTestRoom1, "moveTestRoom1");
		displayACLEntry(moveTestRoom2, "moveTestRoom2");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			containerService().move(moveContainer, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Delete WRITE as forbiddance userRight for someContainer... ");
		aclService().remove(someContainer, user, WasabiPermission.WRITE);
		System.out.println("done.");

		System.out.print("Moving moveRoom from moveTestRoom1 to moveTestRoom2... ");
		try {
			containerService().move(moveContainer, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(moveContainer, "moveContainer");
		displayACLEntry(someContainer, "someContainer");

		System.out.println("===========================");
	}

	@Test
	public void removeTest() throws WasabiException {
		System.out.println("=== removeTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating removeTestRoom at usersHome... ");
		WasabiRoomDTO removeTestRoom = null;
		try {
			removeTestRoom = roomService().create("removeTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for removeTestRoom... ");
		aclService().deactivateInheritance(removeTestRoom);
		System.out.println("done.");

		aclService().remove(
				removeTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating container someContainer at removeTestRoom... ");
		WasabiContainerDTO someContainer = null;
		try {
			someContainer = containerService().create("someContainer", removeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing container at removeTestRoom... ");
		try {
			containerService().remove(someContainer, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Removing container at removeTestRoom... ");
		try {
			containerService().remove(someContainer, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void renameTest() throws WasabiException {
		System.out.println("=== renameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating renameTestRoom at usersHome... ");
		WasabiRoomDTO renameTestRoom = null;
		try {
			renameTestRoom = roomService().create("renameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for renameTestRoom... ");
		aclService().deactivateInheritance(renameTestRoom);
		System.out.println("done.");

		aclService().remove(
				renameTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating container someContainer at renameTestRoom... ");
		WasabiContainerDTO someContainer = null;
		try {
			someContainer = containerService().create("someContainer", renameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Renaming someContainer to renameContainer... ");
		try {
			containerService().rename(someContainer, "renameContainer", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Renaming someContainer to renameContainer... ");
		try {
			containerService().rename(someContainer, "renameContainer", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

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
}
