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
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class GroupRightsTest extends WasabiRemoteTest {

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

	@Test
	public void groupRightsTest() throws WasabiException {
		WasabiGroupDTO g1 = groupService().create("g1", null);
		WasabiGroupDTO g2 = groupService().create("g2", g1);
		WasabiGroupDTO g3 = groupService().create("g3", g1);
		WasabiGroupDTO g4 = groupService().create("g4", g2);
		WasabiGroupDTO g5 = groupService().create("g5", g2);
		WasabiGroupDTO g6 = groupService().create("g6", g4);
		WasabiGroupDTO g7 = groupService().create("g7", g3);

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		groupService().addMember(g5, user);
		groupService().addMember(g6, user);
		groupService().addMember(g7, user);

		System.out.print("Creating groupRightsRoom at usersHome... ");
		WasabiRoomDTO groupRightsRoom = null;
		try {
			groupRightsRoom = roomService().create("groupRightsRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for userInheritedTimeRightsRoom... ");
		aclService().deactivateInheritance(groupRightsRoom);
		System.out.println("done.");

		System.out.print("Setting INSERT with allowance as groupRight at groupRightsRoom... ");
		aclService().create(groupRightsRoom, g1, WasabiPermission.INSERT, true);
		System.out.println("done.");

		displayACLEntry(groupRightsRoom, "groupRightsRoom");
		
		System.out.print("Creating subRoom1 at groupRightsRoom... ");
		WasabiRoomDTO subRoom1 = null;
		try {
			subRoom1 = roomService().create("subRoom1", groupRightsRoom);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		System.out.print("Setting INSERT with forbiddance as groupRight at groupRightsRoom... ");
		aclService().create(groupRightsRoom, g6, WasabiPermission.INSERT, false);
		System.out.println("done.");
		
		displayACLEntry(subRoom1, "subRoom1");
		
		System.out.print("Creating subSubRoom1 at subRoom1... ");
		WasabiRoomDTO subSubRoom1 = null;
		try {
			subSubRoom1 = roomService().create("subRoom1", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		System.out.print("Setting INSERT with allowance as groupTimeRight at groupRightsRoom... ");
		aclService().create(groupRightsRoom, g4, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");
		
		displayACLEntry(subSubRoom1, "subRoom1");
		
		System.out.print("Creating subSubRoom1 at subRoom1... ");
		try {
			subSubRoom1 = roomService().create("subSubRoom1", subRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		displayACLEntry(subSubRoom1, "subSubRoom1");
		
		System.out.print("Setting INSERT with forbiddance as groupRight at subSubRoom1... ");
		aclService().create(subSubRoom1, g3, WasabiPermission.INSERT, false);
		System.out.println("done.");
		
		System.out.print("Creating someRoom at subSubRoom1... ");
		WasabiRoomDTO someRoom = null;
		try {
			someRoom = roomService().create("someRoom", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		System.out.print("Setting INSERT with allowance as groupTimeRight at subSubRoom1... ");
		aclService().create(subSubRoom1, g1, WasabiPermission.INSERT, true, java.lang.System.currentTimeMillis(), (java.lang.System.currentTimeMillis() + 123456));
		System.out.println("done.");
		
		System.out.print("Creating someRoom1 at subSubRoom1... ");
		WasabiRoomDTO someRoom1 = null;
		try {
			someRoom1 = roomService().create("someRoom1", subSubRoom1);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}

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

}
