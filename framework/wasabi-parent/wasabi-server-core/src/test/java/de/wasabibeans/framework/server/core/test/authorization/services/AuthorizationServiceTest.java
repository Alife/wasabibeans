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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiCertificateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class AuthorizationServiceTest extends WasabiRemoteTest {

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
	public void existsCertificateTest() throws WasabiException {
		System.out.println("=== existsCertificateTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for testRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(
				testRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Setting VIEW forbiddance as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Check if Cert for user, testRoom and INSERT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.INSERT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Check if Cert for user, testRoom and INSERT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.INSERT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for user... ");
		aclService().create(user, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Check if Cert for user, testRoom and INSERT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.INSERT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room testRoom2...");
		WasabiRoomDTO testRoom2 = null;
		try {
			testRoom2 = roomService().create("testRoom2", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Check if Cert for user, testRoom and INSERT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.INSERT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void hasPermission1Test() throws WasabiException {
		System.out.println("=== hasPermission1Test() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiGroupDTO wasabiGroup = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);

		System.out.print("Setting INSERT as groupRight for group wasabi... ");
		aclService().create(wasabiGroup, wasabiGroup, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Using hasPermission...");
		try {
			authorizationService().hasPermission(testRoom, newUser, WasabiPermission.COMMENT);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Using hasPermission...");
		try {
			authorizationService().hasPermission(testRoom, newUser, WasabiPermission.COMMENT);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for newUser... ");
		aclService().create(newUser, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Using hasPermission: ");
		try {
			System.out.println(authorizationService().hasPermission(testRoom, newUser, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(testRoom, "testRoom");

		System.out.print("Setting COMMENT as userRight for testRoom and newUser... ");
		aclService().create(testRoom, newUser, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		displayACLEntry(testRoom, "testRoom");

		System.out.print("Using hasPermission: ");
		try {
			System.out.println(authorizationService().hasPermission(testRoom, newUser, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void hasPermission2Test() throws WasabiException {
		System.out.println("=== hasPermission2Test() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating user newUser...");
		WasabiUserDTO newUser = null;
		try {
			newUser = userService().create("newUser", "password");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.print("Using hasPermission:");
		try {
			System.out.println(authorizationService().hasPermission(testRoom, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Using hasPermission:");
		try {
			System.out.println(authorizationService().hasPermission(testRoom, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting COMMENT forbidance as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.COMMENT, false);
		System.out.println("done.");

		System.out.print("Using hasPermission:");
		try {
			System.out.println(authorizationService().hasPermission(testRoom, WasabiPermission.COMMENT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void listCertificateByObjectTest() throws WasabiException, ItemNotFoundException, RepositoryException {
		System.out.println("=== listCertificateByObjectTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for testRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(testRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating room testRoom2...");
		WasabiRoomDTO testRoom2 = null;
		try {
			testRoom2 = roomService().create("testRoom2", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("List certificates by object...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificatesByObject(testRoom,
					WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("List certificates by object...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificatesByObject(testRoom,
					WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void listCertificateByUserTest() throws WasabiException, ItemNotFoundException, RepositoryException {
		System.out.println("=== listCertificateByUserTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for testRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(testRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating room testRoom2...");
		WasabiRoomDTO testRoom2 = null;
		try {
			testRoom2 = roomService().create("testRoom2", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("List certificates by user...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificatesByUser(user,
					WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbidance as userRight for testRoom... ");
		aclService().create(testRoom, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		System.out.println("List certificates by user...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificatesByUser(user,
					WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void listCertificateTest() throws WasabiException, ItemNotFoundException, RepositoryException {
		System.out.println("=== listCertificateTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating room testRoom...");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for testRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(
				testRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Creating room testRoom2...");
		WasabiRoomDTO testRoom2 = null;
		try {
			testRoom2 = roomService().create("testRoom2", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("List certificates...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificates(WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()) + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()) + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for group admin... ");
		aclService().create(groupService().getGroupByName(WasabiConstants.ADMINS_GROUP_NAME), user,
				WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Add user to admin group...");
		try {
			groupService().addMember(groupService().getGroupByName(WasabiConstants.ADMINS_GROUP_NAME), user);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("List certificates...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificates(WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room testRoom3...");
		WasabiRoomDTO testRoom3 = null;
		try {
			testRoom3 = roomService().create("testRoom3", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating room testRoom4...");
		WasabiRoomDTO testRoom4 = null;
		try {
			testRoom4 = roomService().create("testRoom4", testRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Check if Cert for user, testRoom and INSERT exists:");
		try {
			System.out.println(authorizationService().existsCertificate(testRoom, user, WasabiPermission.INSERT));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("List certificates...");
		try {
			Vector<WasabiCertificateDTO> cert = authorizationService().listCertificates(WasabiPermission.INSERT);
			for (WasabiCertificateDTO wasabiCertificateDTO : cert) {
				System.out.println("[id=" + wasabiCertificateDTO.getId() + ", user="
						+ objectService().getName(wasabiCertificateDTO.getUser()).getValue() + ", object="
						+ objectService().getName(wasabiCertificateDTO.getObject()).getValue() + ", permission="
						+ wasabiCertificateDTO.getPermission() + "]");
			}
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
