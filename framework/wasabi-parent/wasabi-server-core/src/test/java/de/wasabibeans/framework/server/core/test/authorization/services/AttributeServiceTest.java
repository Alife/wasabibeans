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
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class AttributeServiceTest extends WasabiRemoteTest {

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

		System.out.print("Creating attribute attr1 at createTestRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr2 at createTestRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", createTestRoom, createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for createTestRoom... ");
		aclService().create(createTestRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Creating document at createTestRoom... ");
		WasabiDocumentDTO document = null;
		try {
			document = documentService().create("testDoc", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr1 at createTestRoom... ");
		try {
			attr1 = attributeService().create("attr1", "42", createTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr2 at createTestRoom... ");
		try {
			attr2 = attributeService().create("attr2", document, createTestRoom);
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
	public void getAffiliation() throws WasabiException {
		System.out.println("=== getAffiliation() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getAffiliationRoom at usersHome... ");
		WasabiRoomDTO getAffiliationRoom = null;
		try {
			getAffiliationRoom = roomService().create("getAffiliationRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getAffiliationRoom... ");
		aclService().create(getAffiliationRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getAffiliationRoom... ");
		aclService().create(getAffiliationRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getAffiliationRoom... ");
		aclService().deactivateInheritance(getAffiliationRoom);
		System.out.println("done.");

		aclService().remove(
				getAffiliationRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr1 at getAffiliationRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", getAffiliationRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting affiliation of attr1... ");
		try {
			attributeService().getAffiliation(attr1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getAffiliationRoom... ");
		aclService().create(getAffiliationRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting affiliation of attr1... ");
		try {
			attributeService().getAffiliation(attr1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getAttributeByName() throws WasabiException {
		System.out.println("=== getAttributeByName() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getAttributeByNameRoom at usersHome... ");
		WasabiRoomDTO getAttributeByNameRoom = null;
		try {
			getAttributeByNameRoom = roomService().create("getAttributeByNameRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getAttributeByNameRoom... ");
		aclService().create(getAttributeByNameRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getAttributeByNameRoom... ");
		aclService().create(getAttributeByNameRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getAttributeByNameRoom... ");
		aclService().deactivateInheritance(getAttributeByNameRoom);
		System.out.println("done.");

		aclService().remove(
				getAttributeByNameRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr1 at getAttributeByNameRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", getAttributeByNameRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting attribute by name ... ");
		try {
			attributeService().getAttributeByName(getAttributeByNameRoom, "attr1");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for getAttributeByNameRoom... ");
		aclService().create(getAttributeByNameRoom, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Getting attribute by name ... ");
		try {
			attributeService().getAttributeByName(getAttributeByNameRoom, "attr1");
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getAttributes() throws WasabiException {
		System.out.println("=== getAttributes() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getAttributesRoom at usersHome... ");
		WasabiRoomDTO getAttributesRoom = null;
		try {
			getAttributesRoom = roomService().create("getAttributesRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getAttributesRoom... ");
		aclService().create(getAttributesRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getAttributesRoom... ");
		aclService().create(getAttributesRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getAttributesRoom... ");
		aclService().deactivateInheritance(getAttributesRoom);
		System.out.println("done.");

		aclService().remove(
				getAttributesRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr1 at getAttributesRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", getAttributesRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr2 at getAttributesRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", "42", getAttributesRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr3 at getAttributesRoom... ");
		WasabiAttributeDTO attr3 = null;
		try {
			attr3 = attributeService().create("attr3", "42", getAttributesRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Getting attributes at getAttriutesRoom... ");
		try {
			Vector<WasabiAttributeDTO> attrs = attributeService().getAttributes(getAttributesRoom);
			for (WasabiAttributeDTO wasabiAttributeDTO : attrs)
				System.out.println(objectService().getName(wasabiAttributeDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting VIEW as userRight for attr1... ");
		aclService().create(attr1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for attr2... ");
		aclService().create(attr2, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.println("Getting attributes at getAttriutesRoom... ");
		try {
			Vector<WasabiAttributeDTO> attrs = attributeService().getAttributes(getAttributesRoom);
			for (WasabiAttributeDTO wasabiAttributeDTO : attrs)
				System.out.println(objectService().getName(wasabiAttributeDTO).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getAttributeType() throws WasabiException {
		System.out.println("=== getAttributeType() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getAttributeTypeRoom at usersHome... ");
		WasabiRoomDTO getAttributeTypeRoom = null;
		try {
			getAttributeTypeRoom = roomService().create("getAttributeTypeRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getAttributeTypeRoom... ");
		aclService().create(getAttributeTypeRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getAttributeTypeRoom... ");
		aclService().create(getAttributeTypeRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getAttributeTypeRoom... ");
		aclService().deactivateInheritance(getAttributeTypeRoom);
		System.out.println("done.");

		aclService().remove(
				getAttributeTypeRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr1 at getAttributeTypeRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", getAttributeTypeRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting attribute type of attr1: ");
		try {
			System.out.println(attributeService().getAttributeType(attr1));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getAttributeTypeRoom... ");
		aclService().create(getAttributeTypeRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting attribute type of attr1: ");
		try {
			System.out.println(attributeService().getAttributeType(attr1));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println("===========================");
	}

	@Test
	public void getValue() throws WasabiException {
		System.out.println("=== getValue() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getValueRoom at usersHome... ");
		WasabiRoomDTO getValueRoom = null;
		try {
			getValueRoom = roomService().create("getValueRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getValueRoom... ");
		aclService().create(getValueRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getValueRoom... ");
		aclService().create(getValueRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getValueRoom... ");
		aclService().deactivateInheritance(getValueRoom);
		System.out.println("done.");

		aclService().remove(
				getValueRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr1 at getValueRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", getValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting getValue of attr1: ");
		try {
			System.out.println(attributeService().getValue(String.class, attr1).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getValueRoom... ");
		aclService().create(getValueRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting getValue of attr1: ");
		try {
			System.out.println(attributeService().getValue(String.class, attr1).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void getWasabiValue() throws WasabiException {
		System.out.println("=== getWasabiValue() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating getWasabiValueRoom at usersHome... ");
		WasabiRoomDTO getWasabiValueRoom = null;
		try {
			getWasabiValueRoom = roomService().create("getWasabiValueRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for getWasabiValueRoom... ");
		aclService().create(getWasabiValueRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for getWasabiValueRoom... ");
		aclService().create(getWasabiValueRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for getWasabiValueRoom... ");
		aclService().deactivateInheritance(getWasabiValueRoom);
		System.out.println("done.");

		aclService().remove(
				getWasabiValueRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr2 at getWasabiValueRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", "42", getWasabiValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr1 at getWasabiValueRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", attr2, getWasabiValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Getting WasabiValue of attr1: ");
		try {
			System.out.println(attributeService().getWasabiValue(attr1).getValue());
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting READ as userRight for getWasabiValueRoom... ");
		aclService().create(getWasabiValueRoom, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Getting WasabiValue of attr1: ");
		try {
			System.out.println(attributeService().getWasabiValue(attr1).getValue());
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

		System.out.print("Creating document at createTestRoom... ");
		WasabiDocumentDTO document = null;
		try {
			document = documentService().create("testDoc", moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr2 at createTestRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", document, moveTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			attributeService().move(attr2, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting INSERT as userRight for moveTestRoom2... ");
		aclService().create(moveTestRoom2, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			attributeService().move(attr2, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for moveTestRoom1... ");
		aclService().create(moveTestRoom1, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting WRITE forbiddance as userRight for document... ");
		aclService().create(document, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Moving testDoc at moveTestRoom2... ");
		try {
			attributeService().move(attr2, moveTestRoom2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

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

		aclService().remove(
				removeTestRoom1,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.INSERT, WasabiPermission.WRITE });

		System.out.print("Setting INSERT as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Setting READ as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.READ, true);
		System.out.println("done.");

		System.out.print("Setting VIEW as userRight for removeTestRoom1... ");
		aclService().create(removeTestRoom1, user, WasabiPermission.VIEW, true);
		System.out.println("done.");

		System.out.print("Creating attribute attr1 at createTestRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", removeTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr2 at createTestRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", attr1, removeTestRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing attr2 ... ");
		try {
			attributeService().remove(attr2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for testDoc... ");
		aclService().create(attr2, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting WRITE with forbiddance as userRight for attr... ");
		aclService().create(attr1, user, WasabiPermission.WRITE, false);
		System.out.println("done.");

		System.out.print("Removing attr2 ... ");
		try {
			attributeService().remove(attr2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if attr2 exists:");
		try {
			System.out.println(objectService().exists(attr2));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if attr1 exists:");
		try {
			System.out.println(objectService().exists(attr1));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Removing WRITE with forbiddance as userRight for attr1... ");
		aclService().remove(attr1, user, WasabiPermission.WRITE);
		System.out.println("done.");

		System.out.print("Removing attr2 ... ");
		try {
			attributeService().remove(attr2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if attr2 exists:");
		try {
			System.out.println(objectService().exists(attr2));
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Checking if attr1 exists:");
		try {
			System.out.println(objectService().exists(attr1));
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

		System.out.print("Creating attribute attr1 at renameTestRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", "42", renameTestRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Renaming attribute attr1 to renameAttr... ");
		try {
			attributeService().rename(attr1, "renameAttr", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for renameTestRoom... ");
		aclService().create(renameTestRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Renaming attribute attr1 to renameAttr... ");
		try {
			attributeService().rename(attr1, "renameAttr", null);
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

	@Test
	public void setValue() throws WasabiException {
		System.out.println("=== setValue() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating setValueRoom at usersHome... ");
		WasabiRoomDTO setValueRoom = null;
		try {
			setValueRoom = roomService().create("setValueRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for setValueRoom... ");
		aclService().create(setValueRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for setValueRoom... ");
		aclService().create(setValueRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for setValueRoom... ");
		aclService().deactivateInheritance(setValueRoom);
		System.out.println("done.");

		aclService().remove(
				setValueRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr2 at setValueRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", "42", setValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting Value of attr2: ");
		try {
			attributeService().setValue(attr2, "17", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for setValueRoom... ");
		aclService().create(setValueRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("Setting Value of attr2: ");
		try {
			attributeService().setValue(attr2, "17", null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@Test
	public void setWasabiValue() throws WasabiException {
		System.out.println("=== setWasabiValue() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating setWasabiValueRoom at usersHome... ");
		WasabiRoomDTO setWasabiValueRoom = null;
		try {
			setWasabiValueRoom = roomService().create("setWasabiValueRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting GRANT as userRight for setWasabiValueRoom... ");
		aclService().create(setWasabiValueRoom, user, WasabiPermission.GRANT, true);
		System.out.println("done.");

		System.out.print("Setting INSERT as userRight for setWasabiValueRoom... ");
		aclService().create(setWasabiValueRoom, user, WasabiPermission.INSERT, true);
		System.out.println("done.");

		System.out.print("Deactivating inheritance for setWasabiValueRoom... ");
		aclService().deactivateInheritance(setWasabiValueRoom);
		System.out.println("done.");

		aclService().remove(
				setWasabiValueRoom,
				user,
				new int[] { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
						WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		System.out.print("Creating attribute attr2 at setWasabiValueRoom... ");
		WasabiAttributeDTO attr2 = null;
		try {
			attr2 = attributeService().create("attr2", "42", setWasabiValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Creating attribute attr1 at setWasabiValueRoom... ");
		WasabiAttributeDTO attr1 = null;
		try {
			attr1 = attributeService().create("attr1", attr2, setWasabiValueRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("setting WasabiValue of attr1: ");
		try {
			attributeService().setWasabiValue(attr1, attr2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Setting WRITE as userRight for setWasabiValueRoom... ");
		aclService().create(setWasabiValueRoom, user, WasabiPermission.WRITE, true);
		System.out.println("done.");

		System.out.print("setting WasabiValue of attr1: ");
		try {
			attributeService().setWasabiValue(attr1, attr2, null);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("===========================");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}
}
