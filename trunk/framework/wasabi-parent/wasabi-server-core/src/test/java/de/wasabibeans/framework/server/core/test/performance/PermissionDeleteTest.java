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

package de.wasabibeans.framework.server.core.test.performance;

import java.util.Vector;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class PermissionDeleteTest extends WasabiRemoteTest {

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

	private static int randNr(int n) {
		double decNr = Math.random();
		return (int) Math.round(decNr * n);
	}

	@Test
	public void permissionDeleteTest() throws WasabiException, LoginException, NamingException, NotSupportedException,
			SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		System.out.println("=== permissionDeleteTest() ===");

		WasabiUserDTO user = userService().getUserByName("user");
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();

		System.out.print("Creating testRoom at users home... ");
		WasabiRoomDTO testRoom = null;
		try {
			testRoom = roomService().create("testRoom", usersHome);
			System.out.println("done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.print("Deactivating inheritance for createTestRoom... ");
		aclService().deactivateInheritance(testRoom);
		System.out.println("done.");

		aclService().remove(testRoom, user,
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE});

		UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
		utx.begin();
		System.out.println("Creating 1000 rooms in one hierarchy ");
		WasabiRoomDTO roomRef = testRoom;
		for (int i = 0; i < 499; i++) {
			roomRef = roomService().create(new Integer(i).toString(), roomRef);
			System.out.println("create room " + i);
		}
		utx.commit();

		long startTime = java.lang.System.currentTimeMillis();
		utx.begin();
		roomService().remove(testRoom, null);
		utx.commit();
		long endTime = java.lang.System.currentTimeMillis();

		System.out.println("Time for deleting rooms: " + (endTime-startTime) );

		System.out.println("===========================");

	}

}
