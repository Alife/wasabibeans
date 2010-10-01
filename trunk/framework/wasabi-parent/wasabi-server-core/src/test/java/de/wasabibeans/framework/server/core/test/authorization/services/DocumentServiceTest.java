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

import java.util.Date;
import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class DocumentServiceTest extends WasabiRemoteTest {

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

		System.out.print("Creating document at createTestRoom... ");
		try {
			documentService().create("testDoc", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating document at createTestRoom... ");
		try {
			documentService().create("testDoc", createTestRoom);
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
	public void getContentTest() throws WasabiException {
		System.out.println("=== getContentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getContentRoom at usersHome... ");
		WasabiRoomDTO getContentRoom = null;
		try {
			getContentRoom = roomService().create("getContentRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getContentRoom... ");
		aclService().deactivateInheritance(getContentRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at getContentRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", getContentRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to get content from testDoc... ");
		try {
			documentService().getContent(testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getContentRoom... ");
		aclService().create(getContentRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Try to get content from testDoc... ");
		try {
			documentService().getContent(testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDocumentByNameTest() throws WasabiException {
		System.out.println("=== getDocumentByNameTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDocumentByNameRoom at usersHome... ");
		WasabiRoomDTO getDocumentByNameRoom = null;
		try {
			getDocumentByNameRoom = roomService().create("getDocumentByNameRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDocumentByNameRoom... ");
		aclService().deactivateInheritance(getDocumentByNameRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at getDocumentByNameRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", getDocumentByNameRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to get document testDoc at getDocumentByNameRoom... ");
		try {
			documentService().getDocumentByName(getDocumentByNameRoom, "testDoc");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getDocumentByNameRoom... ");
		aclService().create(getDocumentByNameRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Try to get document testDoc1 at getDocumentByNameRoom... ");
		try {
			documentService().getDocumentByName(getDocumentByNameRoom, "testDoc");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDocumentsByCreationDateTest() throws WasabiException {
		System.out.println("=== getDocumentsByCreationDateTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDocumentsByCreationDateRoom at usersHome... ");
		WasabiRoomDTO getDocumentsByCreationDateRoom = null;
		try {
			getDocumentsByCreationDateRoom = roomService().create("getDocumentsByCreationDateRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDocumentsByCreationDateRoom... ");
		aclService().create(getDocumentsByCreationDateRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDocumentsByCreationDateRoom... ");
		aclService().create(getDocumentsByCreationDateRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDocumentsByCreationDateRoom... ");
		aclService().deactivateInheritance(getDocumentsByCreationDateRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at getDocumentsByCreationDateRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", getDocumentsByCreationDateRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Try to get document testDoc at getDocumentsByCreationDateRoom... ");
		try {
			Vector<WasabiDocumentDTO> docs = documentService().getDocumentsByCreationDate(
					getDocumentsByCreationDateRoom, new Date(0), new Date(2000000000));
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getDocumentsByCreationDateRoom... ");
		aclService().create(getDocumentsByCreationDateRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Try to get document testDoc at getDocumentsByCreationDateRoom... ");
		try {
			Vector<WasabiDocumentDTO> docs = documentService().getDocumentsByCreationDate(
					getDocumentsByCreationDateRoom, new Date(0), new Date(2000000000));
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getDocumentsTest() throws WasabiException {
		System.out.println("=== getDocumentsTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDocumentsRoom at usersHome... ");
		WasabiRoomDTO getDocumentsRoom = null;
		try {
			getDocumentsRoom = roomService().create("getDocumentsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDocumentsRoom... ");
		aclService().create(getDocumentsRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDocumentsRoom... ");
		aclService().create(getDocumentsRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDocumentsRoom... ");
		aclService().deactivateInheritance(getDocumentsRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc1 at getDocumentsRoom... ");
		WasabiDocumentDTO testDoc1 = null;
		try {
			testDoc1 = documentService().create("testDoc1", getDocumentsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc2 at getDocumentsRoom... ");
		WasabiDocumentDTO testDoc2 = null;
		try {
			testDoc2 = documentService().create("testDoc2", getDocumentsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc3 at getDocumentsRoom... ");
		WasabiDocumentDTO testDoc3 = null;
		try {
			testDoc3 = documentService().create("testDoc3", getDocumentsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting documents at getDocumentsRoom... ");
		try {
			Vector<WasabiDocumentDTO> docs = documentService().getDocuments(getDocumentsRoom);
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for testDoc1... ");
		aclService().create(testDoc1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for testDoc3... ");
		aclService().create(testDoc3, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Getting documents at getDocumentsRoom... ");
		try {
			Vector<WasabiDocumentDTO> docs = documentService().getDocuments(getDocumentsRoom);
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void hasDocumentsCreatedBefore() throws WasabiException {
		System.out.println("=== hasDocumentsCreatedBefore() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating hasDocumentsCreatedBeforeTestRoom at usersHome... ");
		WasabiRoomDTO hasDocumentsCreatedBeforeTestRoom = null;
		try {
			hasDocumentsCreatedBeforeTestRoom = roomService().create("hasDocumentsCreatedBeforeTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for hasDocumentsCreatedBeforeTestRoom... ");
		aclService().create(hasDocumentsCreatedBeforeTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for hasDocumentsCreatedBeforeTestRoom... ");
		aclService().create(hasDocumentsCreatedBeforeTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for hasDocumentsCreatedBeforeTestRoom... ");
		aclService().deactivateInheritance(hasDocumentsCreatedBeforeTestRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc1 at hasDocumentsCreatedBeforeTestRoom... ");
		WasabiDocumentDTO testDoc1 = null;
		try {
			testDoc1 = documentService().create("testDoc1", hasDocumentsCreatedBeforeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc2 at hasDocumentsCreatedBeforeTestRoom... ");
		WasabiDocumentDTO testDoc2 = null;
		try {
			testDoc2 = documentService().create("testDoc2", hasDocumentsCreatedBeforeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc3 at hasDocumentsCreatedBeforeTestRoom... ");
		WasabiDocumentDTO testDoc3 = null;
		try {
			testDoc3 = documentService().create("testDoc3", hasDocumentsCreatedBeforeTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Using hasDocumentsCreatedBefore at hasDocumentsCreatedBeforeTestRoom: ");
		try {
			System.out.println(documentService().hasDocumentsCreatedBefore(hasDocumentsCreatedBeforeTestRoom,
					java.lang.System.currentTimeMillis()));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for testDoc1... ");
		aclService().create(testDoc1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for testDoc3... ");
		aclService().create(testDoc3, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Using hasDocumentsCreatedBefore at hasDocumentsCreatedBeforeTestRoom: ");
		try {
			System.out.println(documentService().hasDocumentsCreatedBefore(hasDocumentsCreatedBeforeTestRoom,
					java.lang.System.currentTimeMillis()));
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

		System.out.print("Deactivating inheritance for moveTestRoom2... ");
		aclService().deactivateInheritance(moveTestRoom2);
		System.out.println("done.");

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

		System.out.print("Creating document testDoc at moveTestRoom1... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute at testDoc ... ");
		WasabiAttributeDTO attr = null;
		try {
			attr = attributeService().create("attr", "trallala", testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(attr, "attr");

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			documentService().move(testDoc, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			documentService().move(testDoc, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			documentService().move(testDoc, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		displayACLEntry(testDoc, "testDoc");
		displayACLEntry(attr, "attr");

		System.out.println("===========================");
	}

	@Test
	public void removeTest() throws WasabiException {
		System.out.println("=== removeTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating removeTestRoom1 at usersHome... ");
		WasabiRoomDTO removeTestRoom1 = null;
		try {
			removeTestRoom1 = roomService().create("removeTestRoom1", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for removeTestRoom1... ");
		aclService().deactivateInheritance(removeTestRoom1);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Creating document testDoc at removeTestRoom1... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", removeTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute at testDoc ... ");
		WasabiAttributeDTO attr = null;
		try {
			attr = attributeService().create("attr", "trallala", testDoc);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing testDoc ... ");
		try {
			documentService().remove(testDoc, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for testDoc... ");
		aclService().create(testDoc, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting WRITE with forbiddance as userRight for attr... ");
		aclService().create(attr, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Removing testDoc ... ");
		try {
			documentService().remove(testDoc, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if testDoc exists:");
		try {
			System.out.println(objectService().exists(testDoc));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if testDoc exists:");
		try {
			System.out.println(objectService().exists(attr));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing WRITE with forbiddance as userRight for attr... ");
		aclService().remove(attr, user, WasabiPermission.WRITE);
		System.out.println("done.");

		System.out.print("Removing testDoc ... ");
		try {
			documentService().remove(testDoc, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if testDoc exists:");
		try {
			System.out.println(objectService().exists(testDoc));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if testDoc exists:");
		try {
			System.out.println(objectService().exists(attr));
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

		System.out.print("Creating renameTestRoom1 at usersHome... ");
		WasabiRoomDTO renameTestRoom1 = null;
		try {
			renameTestRoom1 = roomService().create("renameTestRoom1", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for renameTestRoom1... ");
		aclService().create(renameTestRoom1, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for renameTestRoom1... ");
		aclService().deactivateInheritance(renameTestRoom1);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for renameTestRoom1... ");
		aclService().create(renameTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for renameTestRoom1... ");
		aclService().create(renameTestRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for renameTestRoom1... ");
		aclService().create(renameTestRoom1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Creating document testDoc at renameTestRoom1... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", renameTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Renaming document testDoc to renameTestDoc... ");
		try {
			documentService().rename(testDoc, "renameTestDoc", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for renameTestRoom1... ");
		aclService().create(renameTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Renaming document testDoc to renameTestDoc... ");
		try {
			documentService().rename(testDoc, "renameTestDoc", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setContentTest() throws WasabiException {
		System.out.println("=== setContentTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating setContentRoom at usersHome... ");
		WasabiRoomDTO setContentRoom = null;
		try {
			setContentRoom = roomService().create("setContentRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for setContentRoom... ");
		aclService().deactivateInheritance(setContentRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc at setContentRoom... ");
		WasabiDocumentDTO testDoc = null;
		try {
			testDoc = documentService().create("testDoc", setContentRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Try to set content from testDoc... ");
		try {
			documentService().setContent(testDoc, "Bratwurstbratgeraet", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for setContentRoom... ");
		aclService().create(setContentRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Try to set content from testDoc... ");
		try {
			documentService().setContent(testDoc, "Bratwurstbratgeraet", null);
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
