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
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ACLServiceTest extends WasabiRemoteTest {

	@Test
	public void activateInheritanceTest() throws WasabiException {
		System.out.println("=== activateInheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating activateInheritanceTestRoom at usersHome... ");
		WasabiRoomDTO activateInheritanceTestRoom = null;
		try {
			activateInheritanceTestRoom = roomService().create("activateInheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for activateInheritanceTestRoom... ");
		aclService().create(activateInheritanceTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for activateInheritanceTestRoom... ");
		aclService().create(activateInheritanceTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		displayACLEntry(activateInheritanceTestRoom, "activateInheritanceTestRoom");

		System.out.print("Deactivating inheritance for activateInheritanceTestRoom... ");
		aclService().deactivateInheritance(activateInheritanceTestRoom);
		System.out.println("done.");

		System.out.print("Delete GRANT as userRight for activateInheritanceTestRoom... ");
		aclService().remove(activateInheritanceTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Activating inheritance for activateInheritanceTestRoom... ");
		try {
			aclService().activateInheritance(activateInheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void createDefaultTest() throws WasabiException {
		System.out.println("=== createDefaultTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating createDefaultTestRoom at usersHome... ");
		WasabiRoomDTO createDefaultTestRoom = null;
		try {
			createDefaultTestRoom = roomService().create("createDefaultTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, READ, INSERT as userRight for createDefaultTestRoom... ");
		aclService().create(createDefaultTestRoom, user,
				new int[] { WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.GRANT },
				new boolean[] { true, true, true });
		System.out.println("done.");

		System.out.print("Deactivating inheritance for createDefaultTestRoom... ");
		aclService().deactivateInheritance(createDefaultTestRoom);
		System.out.println("done.");

		System.out.print("Setting TemplateRight for createDefaultTestRoom... ");
		aclService().createDefault(createDefaultTestRoom, WasabiType.ROOM, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Creating someRoom at createDefaultTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", createDefaultTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(someRoom, "someRoom");

		System.out.print("Delete GRANT as userRight for createDefaultTestRoom... ");
		aclService().remove(createDefaultTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Setting TemplateRight for createDefaultTestRoom... ");
		try {
			aclService().createDefault(createDefaultTestRoom, WasabiType.ROOM, WasabiPermission.EXECUTE, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

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

		displayACLEntry(createTestRoom, "createTestRoom");

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(createTestRoom);
		System.out.println("done.");

		System.out.print("Delete GRANT as userRight for createTestRoom... ");
		aclService().remove(createTestRoom, user, WasabiPermission.GRANT);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for createTestRoom... ");
		try {
			aclService().create(createTestRoom, user, WasabiPermission.READ, true);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void deactivateInheritanceTest() throws WasabiException {
		System.out.println("=== deactivateInheritanceTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating deactivateInheritanceTestRoom at usersHome... ");
		WasabiRoomDTO deactivateInheritanceTestRoom = null;
		try {
			deactivateInheritanceTestRoom = roomService().create("deactivateInheritanceTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, READ, INSERT as userRight for deactivateInheritanceTestRoom... ");
		aclService().create(deactivateInheritanceTestRoom, user,
				new int[] { WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.GRANT },
				new boolean[] { true, true, true });
		System.out.println("done.");

		System.out.print("Creating someRoom at deactivateInheritanceTestRoom... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", deactivateInheritanceTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating someSubRoom at someRoom... ");
		WasabiRoomDTO someSubRoom = null;
		try {
			someSubRoom = roomService().create("someSubRoom", someRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for someRoom... ");
		aclService().create(someRoom, user, new int[] { WasabiPermission.WRITE }, new boolean[] { true });
		System.out.println("done.");

		System.out.print("Setting READ as userRight for someRoom... ");
		aclService().create(someRoom, user, new int[] { WasabiPermission.READ }, new boolean[] { true });
		System.out.println("done.");

		System.out.print("Setting READ as userRight for someSubRoom... ");
		aclService().create(someSubRoom, user, new int[] { WasabiPermission.READ }, new boolean[] { true });
		System.out.println("done.");

		displayACLEntry(someRoom, "someRoom");
		displayACLEntry(someSubRoom, "someSubRoom");

		System.out.print("Deactivating inheritance for deactivateInheritanceTestRoom... ");
		aclService().deactivateInheritance(deactivateInheritanceTestRoom);
		System.out.println("done.");

		displayACLEntry(deactivateInheritanceTestRoom, "deactivateInheritanceTestRoom");
		displayACLEntry(someRoom, "someRoom");
		displayACLEntry(someSubRoom, "someSubRoom");

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
	public void getAclEntriesByIdentityTest() throws WasabiException {
		System.out.println("=== getAclEntriesByIdentityTest() ===");

		// TODO: Entscheidung von Jonas zum übergegeben User abwarten.

		System.out.println("===========================");
	}

	@Test
	public void getAclEntriesTest() throws WasabiException {
		System.out.println("=== getAclEntriesTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getAclEntriesTestRoom at usersHome... ");
		WasabiRoomDTO getAclEntriesTestRoom = null;
		try {
			getAclEntriesTestRoom = roomService().create("getAclEntriesTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getAclEntriesTestRoom... ");
		aclService().create(getAclEntriesTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for getAclEntriesTestRoom... ");
		aclService().create(getAclEntriesTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getAclEntriesTestRoom... ");
		aclService().deactivateInheritance(getAclEntriesTestRoom);
		System.out.println("done.");

		System.out.println("Try to use getAclEntries for getAclEntriesTestRoom...");
		try {
			displayACLEntry(getAclEntriesTestRoom, "getAclEntriesTestRoom");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getAclEntriesTestRoom... ");
		aclService().create(getAclEntriesTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Try to disply ACLEntries for getAclEntriesTestRoom...");
		try {
			displayACLEntry(getAclEntriesTestRoom, "getAclEntriesTestRoom");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Activating inheritance for getAclEntriesTestRoom... ");
		aclService().activateInheritance(getAclEntriesTestRoom);
		System.out.println("done.");

		System.out.println("Try to disply ACLEntries for getAclEntriesTestRoom...");
		try {
			displayACLEntry(getAclEntriesTestRoom, "getAclEntriesTestRoom");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getACLEntriesTest() throws WasabiException {
		System.out.println("=== getACLEntriesTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getACLEntriesTestRoom at usersHome... ");
		WasabiRoomDTO getACLEntriesTestRoom = null;
		try {
			getACLEntriesTestRoom = roomService().create("getACLEntriesTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getACLEntriesTestRoom... ");
		aclService().create(getACLEntriesTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for getACLEntriesTestRoom... ");
		aclService().create(getACLEntriesTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getACLEntriesTestRoom... ");
		aclService().deactivateInheritance(getACLEntriesTestRoom);
		System.out.println("done.");

		System.out.println("Try to use getACLEntries for getACLEntriesTestRoom...");
		try {
			Vector<WasabiACLEntryDTODeprecated> ACLEntries = aclService().getACLEntries(getACLEntriesTestRoom);
			System.out.println("---- ACL entries for object " + objectService().getUUID(getACLEntriesTestRoom)
					+ " (depreciated way)----");

			for (WasabiACLEntryDTODeprecated wasabiACLEntryDTODeprecated : ACLEntries) {
				System.out.println("[id=" + wasabiACLEntryDTODeprecated.getId() + ",identity_id="
						+ wasabiACLEntryDTODeprecated.getIdentity() + ",permission="
						+ wasabiACLEntryDTODeprecated.getPermission() + ",allowance="
						+ wasabiACLEntryDTODeprecated.isAllowance() + ",inheritance="
						+ wasabiACLEntryDTODeprecated.getInheritance());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getACLEntriesTestRoom... ");
		aclService().create(getACLEntriesTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Try to disply ACLEntries for getACLEntriesTestRoom...");
		try {
			Vector<WasabiACLEntryDTODeprecated> ACLEntries = aclService().getACLEntries(getACLEntriesTestRoom);
			System.out.println("---- ACL entries for object " + objectService().getUUID(getACLEntriesTestRoom)
					+ " (depreciated way)----");

			for (WasabiACLEntryDTODeprecated wasabiACLEntryDTODeprecated : ACLEntries) {
				System.out.println("[id=" + wasabiACLEntryDTODeprecated.getId() + ",identity_id="
						+ wasabiACLEntryDTODeprecated.getIdentity() + ",permission="
						+ wasabiACLEntryDTODeprecated.getPermission() + ",allowance="
						+ wasabiACLEntryDTODeprecated.isAllowance() + ",inheritance="
						+ wasabiACLEntryDTODeprecated.getInheritance());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Activating inheritance for getACLEntriesTestRoom... ");
		aclService().activateInheritance(getACLEntriesTestRoom);
		System.out.println("done.");

		System.out.println("Try to disply ACLEntries for getACLEntriesTestRoom...");
		try {
			Vector<WasabiACLEntryDTODeprecated> ACLEntries = aclService().getACLEntries(getACLEntriesTestRoom);
			System.out.println("---- ACL entries for object " + objectService().getUUID(getACLEntriesTestRoom)
					+ " (depreciated way)----");

			for (WasabiACLEntryDTODeprecated wasabiACLEntryDTODeprecated : ACLEntries) {
				System.out.println("[id=" + wasabiACLEntryDTODeprecated.getId() + ",identity_id="
						+ wasabiACLEntryDTODeprecated.getIdentity() + ",permission="
						+ wasabiACLEntryDTODeprecated.getPermission() + ",allowance="
						+ wasabiACLEntryDTODeprecated.isAllowance() + ",inheritance="
						+ wasabiACLEntryDTODeprecated.getInheritance());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getACLlEntriesByIdentityTest() throws WasabiException {
		System.out.println("=== getACLEntriesByIdentityTest() ===");

		// TODO: Entscheidung von Jonas zum übergegeben User abwarten.

		System.out.println("===========================");
	}

	@Test
	public void getDefaultAclEntriesByTypeTest() throws WasabiException {
		System.out.println("=== getDefaultAclEntriesByTypeTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDefaultAclEntriesByTypeTestRoom at usersHome... ");
		WasabiRoomDTO getDefaultAclEntriesByTypeTestRoom = null;
		try {
			getDefaultAclEntriesByTypeTestRoom = roomService().create("getDefaultAclEntriesByTypeTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, INSERT as userRight for getDefaultAclEntriesByTypeTestRoom... ");
		aclService().create(getDefaultAclEntriesByTypeTestRoom, user,
				new int[] { WasabiPermission.INSERT, WasabiPermission.GRANT }, new boolean[] { true, true });
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDefaultAclEntriesByTypeTestRoom... ");
		aclService().deactivateInheritance(getDefaultAclEntriesByTypeTestRoom);
		System.out.println("done.");

		System.out.print("Setting TemplateRight COMMENT, type ROOM for getDefaultAclEntriesByTypeTestRoom... ");
		aclService().createDefault(getDefaultAclEntriesByTypeTestRoom, WasabiType.ROOM, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Setting TemplateRight EXECUTE, type CONTAINER for getDefaultAclEntriesByTypeTestRoom... ");
		aclService().createDefault(getDefaultAclEntriesByTypeTestRoom, WasabiType.CONTAINER, WasabiPermission.EXECUTE,
				true);
		System.out.println("done.");

		System.out.println("Try to disply default ACLEntries for getDefaultAclEntriesByTypeTestRoom...");
		try {
			Vector<WasabiACLEntryTemplateDTO> entries = new Vector<WasabiACLEntryTemplateDTO>();
			entries = aclService().getDefaultAclEntriesByType(getDefaultAclEntriesByTypeTestRoom, WasabiType.CONTAINER);

			System.out.println("---- Default ACL entries for location (getDefaultAclEntriesByTypeTestRoom) "
					+ objectService().getUUID(getDefaultAclEntriesByTypeTestRoom) + " ----");

			for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTO : entries) {
				System.out.println("[id=" + wasabiDefaultACLEntryDTO.getId() + ",location_id="
						+ objectService().getUUID(getDefaultAclEntriesByTypeTestRoom) + ",wasabi_type="
						+ wasabiDefaultACLEntryDTO.getWasabiType() + ",view=" + wasabiDefaultACLEntryDTO.getView()
						+ ",read=" + wasabiDefaultACLEntryDTO.getRead() + ",insert="
						+ wasabiDefaultACLEntryDTO.getInsert() + ",execute=" + wasabiDefaultACLEntryDTO.getExecute()
						+ ",write=" + wasabiDefaultACLEntryDTO.getWrite() + ",comment="
						+ wasabiDefaultACLEntryDTO.getComment() + ",grant=" + wasabiDefaultACLEntryDTO.getGrant()
						+ ",start_time=" + wasabiDefaultACLEntryDTO.getStartTime() + ",end_time="
						+ wasabiDefaultACLEntryDTO.getEndTime());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getDefaultAclEntriesByTypeTestRoom... ");
		aclService().create(getDefaultAclEntriesByTypeTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Try to disply default ACLEntries for getDefaultAclEntriesByTypeTestRoom...");
		try {
			Vector<WasabiACLEntryTemplateDTO> entries = new Vector<WasabiACLEntryTemplateDTO>();
			entries = aclService().getDefaultAclEntriesByType(getDefaultAclEntriesByTypeTestRoom, WasabiType.CONTAINER);

			System.out.println("---- Default ACL entries for location (getDefaultAclEntriesByTypeTestRoom) "
					+ objectService().getUUID(getDefaultAclEntriesByTypeTestRoom) + " ----");

			for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTO : entries) {
				System.out.println("[id=" + wasabiDefaultACLEntryDTO.getId() + ",location_id="
						+ objectService().getUUID(getDefaultAclEntriesByTypeTestRoom) + ",wasabi_type="
						+ wasabiDefaultACLEntryDTO.getWasabiType() + ",view=" + wasabiDefaultACLEntryDTO.getView()
						+ ",read=" + wasabiDefaultACLEntryDTO.getRead() + ",insert="
						+ wasabiDefaultACLEntryDTO.getInsert() + ",execute=" + wasabiDefaultACLEntryDTO.getExecute()
						+ ",write=" + wasabiDefaultACLEntryDTO.getWrite() + ",comment="
						+ wasabiDefaultACLEntryDTO.getComment() + ",grant=" + wasabiDefaultACLEntryDTO.getGrant()
						+ ",start_time=" + wasabiDefaultACLEntryDTO.getStartTime() + ",end_time="
						+ wasabiDefaultACLEntryDTO.getEndTime());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDefaultAclEntriesTest() throws WasabiException {
		System.out.println("=== getDefaultAclEntriesTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDefaultAclEntriesTestRoom at usersHome... ");
		WasabiRoomDTO getDefaultAclEntriesTestRoom = null;
		try {
			getDefaultAclEntriesTestRoom = roomService().create("getDefaultAclEntriesTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT, INSERT as userRight for getDefaultAclEntriesTestRoom... ");
		aclService().create(getDefaultAclEntriesTestRoom, user,
				new int[] { WasabiPermission.INSERT, WasabiPermission.GRANT }, new boolean[] { true, true });
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDefaultAclEntriesTestRoom... ");
		aclService().deactivateInheritance(getDefaultAclEntriesTestRoom);
		System.out.println("done.");

		System.out.print("Setting TemplateRight COMMENT, type ROOM for getDefaultAclEntriesTestRoom... ");
		aclService().createDefault(getDefaultAclEntriesTestRoom, WasabiType.ROOM, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Setting TemplateRight EXECUTE, type CONTAINER for getDefaultAclEntriesTestRoom... ");
		aclService().createDefault(getDefaultAclEntriesTestRoom, WasabiType.CONTAINER, WasabiPermission.EXECUTE, true);
		System.out.println("done.");

		System.out.println("Try to disply default ACLEntries for getDefaultAclEntriesTestRoom...");
		try {
			Vector<WasabiACLEntryTemplateDTO> entries = new Vector<WasabiACLEntryTemplateDTO>();
			entries = aclService().getDefaultAclEntries(getDefaultAclEntriesTestRoom);

			System.out.println("---- Default ACL entries for location (getDefaultAclEntriesTestRoom) "
					+ objectService().getUUID(getDefaultAclEntriesTestRoom) + " ----");

			for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTO : entries) {
				System.out.println("[id=" + wasabiDefaultACLEntryDTO.getId() + ",location_id="
						+ objectService().getUUID(getDefaultAclEntriesTestRoom) + ",wasabi_type="
						+ wasabiDefaultACLEntryDTO.getWasabiType() + ",view=" + wasabiDefaultACLEntryDTO.getView()
						+ ",read=" + wasabiDefaultACLEntryDTO.getRead() + ",insert="
						+ wasabiDefaultACLEntryDTO.getInsert() + ",execute=" + wasabiDefaultACLEntryDTO.getExecute()
						+ ",write=" + wasabiDefaultACLEntryDTO.getWrite() + ",comment="
						+ wasabiDefaultACLEntryDTO.getComment() + ",grant=" + wasabiDefaultACLEntryDTO.getGrant()
						+ ",start_time=" + wasabiDefaultACLEntryDTO.getStartTime() + ",end_time="
						+ wasabiDefaultACLEntryDTO.getEndTime());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getDefaultAclEntriesTestRoom... ");
		aclService().create(getDefaultAclEntriesTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Try to disply default ACLEntries for getDefaultAclEntriesTestRoom...");
		try {
			Vector<WasabiACLEntryTemplateDTO> entries = new Vector<WasabiACLEntryTemplateDTO>();
			entries = aclService().getDefaultAclEntries(getDefaultAclEntriesTestRoom);

			System.out.println("---- Default ACL entries for location (getDefaultAclEntriesTestRoom) "
					+ objectService().getUUID(getDefaultAclEntriesTestRoom) + " ----");

			for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTO : entries) {
				System.out.println("[id=" + wasabiDefaultACLEntryDTO.getId() + ",location_id="
						+ objectService().getUUID(getDefaultAclEntriesTestRoom) + ",wasabi_type="
						+ wasabiDefaultACLEntryDTO.getWasabiType() + ",view=" + wasabiDefaultACLEntryDTO.getView()
						+ ",read=" + wasabiDefaultACLEntryDTO.getRead() + ",insert="
						+ wasabiDefaultACLEntryDTO.getInsert() + ",execute=" + wasabiDefaultACLEntryDTO.getExecute()
						+ ",write=" + wasabiDefaultACLEntryDTO.getWrite() + ",comment="
						+ wasabiDefaultACLEntryDTO.getComment() + ",grant=" + wasabiDefaultACLEntryDTO.getGrant()
						+ ",start_time=" + wasabiDefaultACLEntryDTO.getStartTime() + ",end_time="
						+ wasabiDefaultACLEntryDTO.getEndTime());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void isInheritanceAllowedTest() throws WasabiException {
		System.out.println("=== isInheritanceAllowedTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating isInheritanceAllowedTestRoom at usersHome... ");
		WasabiRoomDTO isInheritanceAllowedTestRoom = null;
		try {
			isInheritanceAllowedTestRoom = roomService().create("isInheritanceAllowedTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for isInheritanceAllowedTestRoom... ");
		aclService().create(isInheritanceAllowedTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for isInheritanceAllowedTestRoom... ");
		aclService().deactivateInheritance(isInheritanceAllowedTestRoom);
		System.out.println("done.");

		System.out.println("Check inheritance for isInheritanceAllowedTestRoom");
		try {
			boolean ihtc = aclService().isInheritanceAllowed(isInheritanceAllowedTestRoom);
			System.out.println("Inheritance = " + ihtc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for isInheritanceAllowedTestRoom... ");
		aclService().create(isInheritanceAllowedTestRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.println("Check inheritance for isInheritanceAllowedTestRoom");
		try {
			boolean ihtc = aclService().isInheritanceAllowed(isInheritanceAllowedTestRoom);
			System.out.println("Inheritance = " + ihtc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Activating inheritance for isInheritanceAllowedTestRoom... ");
		aclService().activateInheritance(isInheritanceAllowedTestRoom);
		System.out.println("done.");

		System.out.println("Check inheritance for isInheritanceAllowedTestRoom...");
		try {
			boolean ihtc = aclService().isInheritanceAllowed(isInheritanceAllowedTestRoom);
			System.out.println("Inheritance = " + ihtc);
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
