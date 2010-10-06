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
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class LinkServiceTest extends WasabiRemoteTest {

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

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(createTestRoom);
		System.out.println("done.");

		aclService().remove(
				createTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Create Link for createTestRoom... ");
		try {
			linkService().create("link", usersHome, createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Create Link for createTestRoom... ");
		try {
			linkService().create("link", usersHome, createTestRoom);
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
	public void getDestinationTest() throws WasabiException {
		System.out.println("=== getDestinationTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDestinationRoomTest at usersHome... ");
		WasabiRoomDTO getDestinationRoomTest = null;
		try {
			getDestinationRoomTest = roomService().create("getDestinationRoomTest", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDestinationTestRoom... ");
		aclService().create(getDestinationRoomTest, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDestinationTestRoom... ");
		aclService().create(getDestinationRoomTest, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDestinationTestRoom... ");
		aclService().deactivateInheritance(getDestinationRoomTest);
		System.out.println("done.");

		aclService().remove(
				getDestinationRoomTest,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for getDestinationTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, getDestinationRoomTest);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Get destination for link... ");
		try {
			linkService().getDestination(link);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getDestinationTestRoom... ");
		aclService().create(getDestinationRoomTest, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Get destination for link... ");
		try {
			linkService().getDestination(link);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getEnvironmentTest() throws WasabiException {
		System.out.println("=== getEnvironmentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getEnvironmentTestRoom at usersHome... ");
		WasabiRoomDTO getEnvironmentTestRoom = null;
		try {
			getEnvironmentTestRoom = roomService().create("getEnvironmentTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getEnvironmentTestRoom... ");
		aclService().create(getEnvironmentTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getEnvironmentTestRoom... ");
		aclService().create(getEnvironmentTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getEnvironmentTestRoom... ");
		aclService().deactivateInheritance(getEnvironmentTestRoom);
		System.out.println("done.");

		aclService().remove(
				getEnvironmentTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for getEnvironmentTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, getEnvironmentTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting environment of link... ");
		try {
			linkService().getEnvironment(link);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getEnvironmentTestRoom... ");
		aclService().create(getEnvironmentTestRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Getting environment of link... ");
		try {
			linkService().getEnvironment(link);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getLinkByNameTest() throws WasabiException {
		System.out.println("=== getLinkByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getLinkByNameTestRoom at usersHome... ");
		WasabiRoomDTO getLinkByNameTestRoom = null;
		try {
			getLinkByNameTestRoom = roomService().create("getLinkByNameTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getLinkByNameTestRoom... ");
		aclService().create(getLinkByNameTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getLinkByNameTestRoom... ");
		aclService().create(getLinkByNameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getLinkByNameTestRoom... ");
		aclService().deactivateInheritance(getLinkByNameTestRoom);
		System.out.println("done.");

		aclService().remove(
				getLinkByNameTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for getLinkByNameTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, getLinkByNameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting link by name ... ");
		try {
			linkService().getLinkByName(getLinkByNameTestRoom, "link");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getLinkByNameTestRoom... ");
		aclService().create(getLinkByNameTestRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Getting link by name ... ");
		try {
			linkService().getLinkByName(getLinkByNameTestRoom, "link");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getLinksTest() throws WasabiException {
		System.out.println("=== getLinksTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getLinksRoom at usersHome... ");
		WasabiRoomDTO getLinksTestRoom = null;
		try {
			getLinksTestRoom = roomService().create("getLinksTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getLinksTestRoom... ");
		aclService().create(getLinksTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getLinksTestRoom... ");
		aclService().create(getLinksTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getLinksTestRoom... ");
		aclService().create(getLinksTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getLinksTestRoom... ");
		aclService().deactivateInheritance(getLinksTestRoom);
		System.out.println("done.");

		aclService().remove(
				getLinksTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.COMMENT, WasabiPermission.EXECUTE,
						WasabiPermission.WRITE });

		System.out.print("Create link1 for getLinksTestRoom... ");
		WasabiLinkDTO link1 = null;
		try {
			link1 = linkService().create("link1", usersHome, getLinksTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Create link2 for getLinksTestRoom... ");
		WasabiLinkDTO link2 = null;
		try {
			link2 = linkService().create("link2", usersHome, getLinksTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Create link3 for getLinksTestRoom... ");
		WasabiLinkDTO link3 = null;
		try {
			link3 = linkService().create("link3", usersHome, getLinksTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting links with VIEW at getLinksTestRoom... ");
		try {
			Vector<WasabiLinkDTO> link = linkService().getLinks(getLinksTestRoom);
			for (WasabiLinkDTO wasabiLinkDTO : link)
				System.out.println(objectService().getName(wasabiLinkDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for link1... ");
		aclService().create(link1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for link3... ");
		aclService().create(link3, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		displayACLEntry(link1, "link1");
		displayACLEntry(link3, "link3");

		System.out.println("Getting links with VIEW at getLinksTestRoom... ");
		try {
			Vector<WasabiLinkDTO> link = linkService().getLinks(getLinksTestRoom);
			for (WasabiLinkDTO wasabiLinkDTO : link)
				System.out.println(objectService().getName(wasabiLinkDTO).getValue());
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

		System.out.print("Setting INSERT as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for moveTestRoom1... ");
		aclService().deactivateInheritance(moveTestRoom1);
		System.out.println("done.");

		aclService().remove(
				moveTestRoom1,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Deactivating inheritance for moveTestRoom2... ");
		aclService().deactivateInheritance(moveTestRoom2);
		System.out.println("done.");

		aclService().remove(
				moveTestRoom2,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.EXECUTE, WasabiPermission.INSERT,
						WasabiPermission.WRITE });

		System.out.print("Creating Link for moveTestRoom1... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Moving link1 from moveTestRoom1 to  moveTestRoom2... ");
		try {
			linkService().move(link, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Moving link1 from moveTestRoom1 to  moveTestRoom2... ");
		try {
			linkService().move(link, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Moving link1 from moveTestRoom1 to  moveTestRoom2... ");
		try {
			linkService().move(link, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(link, "link");

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

		System.out.print("Setting INSERT as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for removeTestRoom... ");
		aclService().deactivateInheritance(removeTestRoom);
		System.out.println("done.");

		aclService().remove(
				removeTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for removeTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, removeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Remove link ... ");
		try {
			linkService().remove(link, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for removeTestRoom... ");
		aclService().create(removeTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Remove link ... ");
		try {
			linkService().remove(link, null);
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

		System.out.print("Setting INSERT as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for renameTestRoom... ");
		aclService().deactivateInheritance(renameTestRoom);
		System.out.println("done.");

		aclService().remove(
				renameTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for renameTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, renameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Renaming link... ");
		try {
			linkService().rename(link, "rename", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Renaming link... ");
		try {
			linkService().rename(link, "rename", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setDestinationTest() throws WasabiException {
		System.out.println("=== setDestinationTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating setDestinationTestRoom at usersHome... ");
		WasabiRoomDTO setDestinationTestRoom = null;
		try {
			setDestinationTestRoom = roomService().create("setDestinationTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for setDestinationTestRoom... ");
		aclService().create(setDestinationTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for setDestinationTestRoom... ");
		aclService().create(setDestinationTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for setDestinationTestRoom... ");
		aclService().deactivateInheritance(setDestinationTestRoom);
		System.out.println("done.");

		aclService().remove(
				setDestinationTestRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Create Link for setDestinationTestRoom... ");
		WasabiLinkDTO link = null;
		try {
			link = linkService().create("link", usersHome, setDestinationTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting destination... ");
		try {
			linkService().setDestination(link, usersHome, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for setDestinationTestRoom... ");
		aclService().create(setDestinationTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting destination... ");
		try {
			linkService().setDestination(link, usersHome, null);
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
