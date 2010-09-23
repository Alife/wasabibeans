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
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ObjectServiceTest extends WasabiRemoteTest {

	private void displayACLEntry(WasabiObjectDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
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
	public void existsTest() throws WasabiException {
		System.out.println("=== existsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating existsTestRoom at usersHome... ");
		WasabiRoomDTO existsTestRoom = null;
		try {
			existsTestRoom = roomService().create("existsTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for existsTestRoom... ");
		aclService().deactivateInheritance(existsTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for existsTestRoom... ");
		aclService().create(existsTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at existsTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", existsTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if someRoom exists...");
		try {
			System.out.println(objectService().exists(someRoom));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for existsTestRoom... ");
		aclService().create(existsTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking if someRoom exists...");
		try {
			System.out.println(objectService().exists(someRoom));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getCreatedByTest() throws WasabiException {
		System.out.println("=== getCreatedByTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getCreatedByTestRoom at usersHome... ");
		WasabiRoomDTO getCreatedByTestRoom = null;
		try {
			getCreatedByTestRoom = roomService().create("getCreatedByTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getCreatedByTestRoom... ");
		aclService().deactivateInheritance(getCreatedByTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getCreatedByTestRoom... ");
		aclService().create(getCreatedByTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at getCreatedByTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", getCreatedByTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking getCreatedBy of someRoom...");
		try {
			System.out.println(objectService().getCreatedBy(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getCreatedByTestRoom... ");
		aclService().create(getCreatedByTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking getCreatedBy of someRoom...");
		try {
			System.out.println(objectService().getCreatedBy(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getObjectsByAttributeNameTest() throws WasabiException {
		System.out.println("=== getObjectsByAttributeNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getObjectsByAttributeNameTestRoom at usersHome... ");
		WasabiRoomDTO getObjectsByAttributeNameTestRoom = null;
		try {
			getObjectsByAttributeNameTestRoom = roomService().create("getObjectsByAttributeNameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getObjectsByAttributeNameTestRoom... ");
		aclService().deactivateInheritance(getObjectsByAttributeNameTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getObjectsByAttributeNameTestRoom... ");
		aclService().create(getObjectsByAttributeNameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom1 at getObjectsByAttributeNameTestRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", getObjectsByAttributeNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someRoom2 at getObjectsByAttributeNameTestRoom... ");
		WasabiRoomDTO someRoom2 = null;
		try {
			someRoom2 = roomService().create("someRoom2", getObjectsByAttributeNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting someAttribute at someRoom2... ");
		WasabiAttributeDTO someAttribute = null;
		try {
			someAttribute = attributeService().create(
					"someAttribute",
					"Ich kenne mindestens 1000 Möglichkeiten wie wir aus der Situation hier wieder rauskommen"
							+ "... Leider sind sie alle tödlich.", someRoom2);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting objects by someAttribute... ");
		try {
			Vector<WasabiObjectDTO> objects = objectService().getObjectsByAttributeName("someAttribute");
			for (WasabiObjectDTO wasabiObjectDTO : objects) {
				System.out.println(objectService().getName(wasabiObjectDTO).getValue());
			}
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getObjectsByAttributeNameTestRoom... ");
		aclService().create(getObjectsByAttributeNameTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting objects by someAttribute... ");
		try {
			Vector<WasabiObjectDTO> objects = objectService().getObjectsByAttributeName("someAttribute");
			for (WasabiObjectDTO wasabiObjectDTO : objects) {
				System.out.println(objectService().getName(wasabiObjectDTO).getValue());
			}
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getObjectsByCreatorTest() throws WasabiException {
		System.out.println("=== getObjectsByCreatorTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getObjectsByCreatorTestRoom at usersHome... ");
		WasabiRoomDTO getObjectsByCreatorTestRoom = null;
		try {
			getObjectsByCreatorTestRoom = roomService().create("getObjectsByCreatorTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getObjectsByCreatorTestRoom... ");
		aclService().deactivateInheritance(getObjectsByCreatorTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getObjectsByCreatorTestRoom... ");
		aclService().create(getObjectsByCreatorTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom1 at getObjectsByCreatorTestRoom... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", getObjectsByCreatorTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		WasabiRoomDTO someRoom2 = null;
		try {
			someRoom2 = roomService().create("someRoom2", getObjectsByCreatorTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for someRoom1... ");
		aclService().create(someRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting objects by creator user... ");
		try {
			Vector<WasabiObjectDTO> objects = objectService().getObjectsByCreator(user);
			for (WasabiObjectDTO wasabiObjectDTO : objects) {
				System.out.println(objectService().getName(wasabiObjectDTO).getValue());
			}
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getCreatedOnTest() throws WasabiException {
		System.out.println("=== getCreatedOnTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getCreatedOnTestRoom at usersHome... ");
		WasabiRoomDTO getCreatedOnTestRoom = null;
		try {
			getCreatedOnTestRoom = roomService().create("getCreatedOnTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getCreatedOnTestRoom... ");
		aclService().deactivateInheritance(getCreatedOnTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getCreatedOnTestRoom... ");
		aclService().create(getCreatedOnTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at getCreatedOnTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", getCreatedOnTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking getCreatedOn of someRoom...");
		try {
			System.out.println(objectService().getCreatedOn(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getCreatedOnTestRoom... ");
		aclService().create(getCreatedOnTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking getCreatedOn of someRoom...");
		try {
			System.out.println(objectService().getCreatedOn(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getModifiedByTest() throws WasabiException {
		System.out.println("=== getModifiedByTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getModifiedByTestRoom at usersHome... ");
		WasabiRoomDTO getModifiedByTestRoom = null;
		try {
			getModifiedByTestRoom = roomService().create("getModifiedByTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getModifiedByTestRoom... ");
		aclService().deactivateInheritance(getModifiedByTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getModifiedByTestRoom... ");
		aclService().create(getModifiedByTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at getModifiedByTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", getModifiedByTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Checking getModifiedBy of someRoom...");
		try {
			System.out.println(objectService().getModifiedBy(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getModifiedByTestRoom... ");
		aclService().create(getModifiedByTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking getModifiedBy of someRoom...");
		try {
			System.out.println(objectService().getModifiedBy(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getModifiedOnTest() throws WasabiException {
		System.out.println("=== getModifiedOnTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getModifiedOnTestRoom at usersHome... ");
		WasabiRoomDTO getModifiedOnTestRoom = null;
		try {
			getModifiedOnTestRoom = roomService().create("getModifiedOnTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getModifiedOnTestRoom... ");
		aclService().deactivateInheritance(getModifiedOnTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getModifiedOnTestRoom... ");
		aclService().create(getModifiedOnTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at getModifiedOnTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", getModifiedOnTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking getModifiedOn of someRoom...");
		try {
			System.out.println(objectService().getModifiedOn(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getModifiedOnTestRoom... ");
		aclService().create(getModifiedOnTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking getModifiedOn of someRoom...");
		try {
			System.out.println(objectService().getModifiedOn(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getNameTest() throws WasabiException {
		System.out.println("=== getNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getNameTestRoom at usersHome... ");
		WasabiRoomDTO getNameTestRoom = null;
		try {
			getNameTestRoom = roomService().create("getNameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for getNameTestRoom... ");
		aclService().deactivateInheritance(getNameTestRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getNameTestRoom... ");
		aclService().create(getNameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at getNameTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", getNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Checking getName of someRoom...");
		try {
			System.out.println(objectService().getName(someRoom).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getNameTestRoom... ");
		aclService().create(getNameTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Checking getName of someRoom...");
		try {
			System.out.println(objectService().getName(someRoom).getValue());
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
