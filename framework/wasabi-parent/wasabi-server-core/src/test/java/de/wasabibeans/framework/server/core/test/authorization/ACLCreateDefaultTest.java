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

package de.wasabibeans.framework.server.core.test.authorization;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ACLCreateDefaultTest extends WasabiRemoteTest {

	@Test
	public void createTest() throws WasabiException {
		// Create user
		WasabiUserDTO user = userService().create("aclCreateDefaultTestUser", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		WasabiRoomDTO room1 = roomService().create("room1", usersHome);
		aclService().deactivateInheritance(room1);
		WasabiRoomDTO room2 = roomService().create("room2", room1);

		aclService().create(room1, user, new int[] { WasabiPermission.VIEW }, new boolean[] { true });
		aclService().activateInheritance(room2);

		aclService().createDefault(room2, user, WasabiType.ROOM,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.WRITE }, new boolean[] { true, true });

		WasabiRoomDTO room3 = roomService().create("room3", room2);

		displayACLEntry(room3, "Raum3");

		WasabiRoomDTO room4 = roomService().create("room4", room3);

		displayACLEntry(room4, "Raum4");
	}

	private void displayACLEntry(WasabiObjectDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Vector<WasabiACLEntryDTO> ACLEntries = new Vector<WasabiACLEntryDTO>();
		ACLEntries = aclService().getAclEntries(room);

		System.out.println("---- ACL entries for object (" + name + ") " + objectService().getUUID(room) + " ----");

		for (WasabiACLEntryDTO wasabiACLEntryDTO : ACLEntries) {
			System.out.println("[id=" + wasabiACLEntryDTO.getId() + ",user_id=" + wasabiACLEntryDTO.getUserId()
					+ ",group_id=" + wasabiACLEntryDTO.getGroupId() + ",parent_id=" + wasabiACLEntryDTO.getParentId()
					+ ",view=" + wasabiACLEntryDTO.getView() + ",read=" + wasabiACLEntryDTO.getRead() + ",insert="
					+ wasabiACLEntryDTO.getInsert() + ",execute=" + wasabiACLEntryDTO.getExecute() + ",write="
					+ wasabiACLEntryDTO.getWrite() + ",comment=" + wasabiACLEntryDTO.getComment() + ",grant="
					+ wasabiACLEntryDTO.getGrant() + ",start_time=" + wasabiACLEntryDTO.getStartTime() + ",end_time="
					+ wasabiACLEntryDTO.getEndTime() + ",inheritance=" + wasabiACLEntryDTO.getInheritance()
					+ ",inheritance_id=" + wasabiACLEntryDTO.getInheritanceId());
		}
	}

	private void displayDefaultACLEntry(WasabiLocationDTO room, String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Vector<WasabiACLEntryTemplateDTO> ACLEntriesForRoom2AfterRemoveRead = new Vector<WasabiACLEntryTemplateDTO>();
		ACLEntriesForRoom2AfterRemoveRead = aclService().getDefaultAclEntries(room);

		System.out.println("---- Default ACL entries for location (" + name + ") " + objectService().getUUID(room)
				+ " ----");

		for (WasabiACLEntryTemplateDTO wasabiDefaultACLEntryDTOAfterRemoveRead : ACLEntriesForRoom2AfterRemoveRead) {
			System.out.println("[id=" + wasabiDefaultACLEntryDTOAfterRemoveRead.getId() + ",location_id="
					+ objectService().getUUID(room) + ",wasabi_type="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getWasabiType() + ",view="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getView() + ",read="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getRead() + ",insert="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getInsert() + ",execute="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getExecute() + ",write="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getWrite() + ",comment="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getComment() + ",grant="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getGrant() + ",start_time="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getStartTime() + ",end_time="
					+ wasabiDefaultACLEntryDTOAfterRemoveRead.getEndTime());
		}
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
