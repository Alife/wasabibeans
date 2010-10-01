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
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class TagServiceTest extends WasabiRemoteTest {

	@Test
	public void addTagTest() throws WasabiException {
		System.out.println("=== addTagTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating addTagTestRoom at usersHome... ");
		WasabiRoomDTO addTagTestRoom = null;
		try {
			addTagTestRoom = roomService().create("addTagTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for addTagTestRoom... ");
		aclService().create(addTagTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for addTagTestRoom... ");
		aclService().deactivateInheritance(addTagTestRoom);
		System.out.println("done.");

		System.out.print("Adding Tag for addTagTestRoom... ");
		try {
			tagService().addTag(addTagTestRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting COMMENT as userRight for addTagTestRoom... ");
		aclService().create(addTagTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Adding Tag for addTagTestRoom... ");
		try {
			tagService().addTag(addTagTestRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void clearTagTest() throws WasabiException {
		System.out.println("=== clearTagTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating clearTagTestRoom at usersHome... ");
		WasabiRoomDTO clearTagTestRoom = null;
		try {
			clearTagTestRoom = roomService().create("clearTagTestRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for clearTagTestRoom... ");
		aclService().create(clearTagTestRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for clearTagTestRoom... ");
		aclService().create(clearTagTestRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for clearTagTestRoom... ");
		aclService().deactivateInheritance(clearTagTestRoom);
		System.out.println("done.");

		System.out.print("Adding Tag for clearTagTestRoom... ");
		try {
			tagService().addTag(clearTagTestRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Clearing Tags for clearTagTestRoom... ");
		try {
			tagService().clearTags(clearTagTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for clearTagTestRoom... ");
		aclService().create(clearTagTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Clearing Tags for clearTagTestRoom... ");
		try {
			tagService().clearTags(clearTagTestRoom);
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
	public void getDocumentsByTag() throws WasabiException {
		System.out.println("=== getDocumentsByTag() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getDocumentsByTagRoom at usersHome... ");
		WasabiRoomDTO getDocumentsByTagRoom = null;
		try {
			getDocumentsByTagRoom = roomService().create("getDocumentsByTagRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getDocumentsByTagRoom... ");
		aclService().create(getDocumentsByTagRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for getDocumentsByTagRoom... ");
		aclService().create(getDocumentsByTagRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for getDocumentsByTagRoom... ");
		aclService().create(getDocumentsByTagRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getDocumentsByTagRoom... ");
		aclService().create(getDocumentsByTagRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getDocumentsByTagRoom... ");
		aclService().deactivateInheritance(getDocumentsByTagRoom);
		System.out.println("done.");

		System.out.print("Creating document testDoc1 at getDocumentsByTagRoom... ");
		WasabiDocumentDTO testDoc1 = null;
		try {
			testDoc1 = documentService().create("testDoc1", getDocumentsByTagRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc2 at getDocumentsByTagRoom... ");
		WasabiDocumentDTO testDoc2 = null;
		try {
			testDoc2 = documentService().create("testDoc2", getDocumentsByTagRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating document testDoc3 at getDocumentsByTagRoom... ");
		WasabiDocumentDTO testDoc3 = null;
		try {
			testDoc3 = documentService().create("testDoc3", getDocumentsByTagRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Adding Tag for testDoc1... ");
		try {
			tagService().addTag(testDoc1, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Adding Tag for testDoc3... ");
		try {
			tagService().addTag(testDoc3, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting documents by Tag Blubbi... ");
		try {
			Vector<String> tags = new Vector<String>();
			tags.add("Blubbi");
			Vector<WasabiDocumentDTO> docs = tagService().getDocumentsByTags(getDocumentsByTagRoom, tags);
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getDocumentsByTagRoom... ");
		aclService().create(getDocumentsByTagRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Getting documents by Tag Blubbi... ");
		try {
			Vector<String> tags = new Vector<String>();
			tags.add("Blubbi");
			Vector<WasabiDocumentDTO> docs = tagService().getDocumentsByTags(getDocumentsByTagRoom, tags);
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW forbiddance as userRight for testDoc1... ");
		aclService().create(testDoc1, user, WasabiPermission.VIEW, false);
		System.out.println("done.");

		displayACLEntry(testDoc1, "testDoc1");

		System.out.println("Getting documents by Tag Blubbi... ");
		try {
			Vector<String> tags = new Vector<String>();
			tags.add("Blubbi");
			Vector<WasabiDocumentDTO> docs = tagService().getDocumentsByTags(getDocumentsByTagRoom, tags);
			for (WasabiDocumentDTO wasabiDocumentDTO : docs)
				System.out.println(objectService().getName(wasabiDocumentDTO).getValue());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getTags() throws WasabiException {
		System.out.println("=== getTags() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getTagsRoom at usersHome... ");
		WasabiRoomDTO getTagsRoom = null;
		try {
			getTagsRoom = roomService().create("getTagsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getTagsRoom... ");
		aclService().create(getTagsRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting COMMENT as userRight for getTagsRoom... ");
		aclService().create(getTagsRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getTagsRoom... ");
		aclService().deactivateInheritance(getTagsRoom);
		System.out.println("done.");

		System.out.print("Adding Tag for getTagsRoom... ");
		try {
			tagService().addTag(getTagsRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Adding Tag for getTagsRoom... ");
		try {
			tagService().addTag(getTagsRoom, "Blubbu");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting tags of getTagsRoom... ");
		try {
			Vector<String> tags = tagService().getTags(getTagsRoom);
			for (String string : tags)
				System.out.println(string);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getTagsRoom... ");
		aclService().create(getTagsRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Getting tags of getTagsRoom... ");
		try {
			Vector<String> tags = tagService().getTags(getTagsRoom);
			for (String string : tags)
				System.out.println(string);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void removeTag() throws WasabiException {
		System.out.println("=== removeTag() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating removeTagRoom at usersHome... ");
		WasabiRoomDTO removeTagRoom = null;
		try {
			removeTagRoom = roomService().create("removeTagRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for removeTagRoom... ");
		aclService().create(removeTagRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");
		
		System.out.print("Setting COMMENT as userRight for removeTagRoom... ");
		aclService().create(removeTagRoom, user, WasabiPermission.COMMENT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for removeTagRoom... ");
		aclService().deactivateInheritance(removeTagRoom);
		System.out.println("done.");

		System.out.print("Adding Tag for removeTagRoom... ");
		try {
			tagService().addTag(removeTagRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing Tag from removeTagRoom... ");
		try {
			tagService().removeTag(removeTagRoom, "Blubbi");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		System.out.print("Setting WRITE as userRight for removeTagRoom... ");
		aclService().create(removeTagRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");
		
		System.out.print("Removing Tag from removeTagRoom... ");
		try {
			tagService().removeTag(removeTagRoom, "Blubbi");
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
