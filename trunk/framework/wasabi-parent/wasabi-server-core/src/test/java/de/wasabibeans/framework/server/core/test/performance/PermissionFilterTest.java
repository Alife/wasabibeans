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

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class PermissionFilterTest extends WasabiRemoteTest {

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
	public void permissionFilter() throws WasabiException, LoginException, NamingException, NotSupportedException,
			SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		System.out.println("=== permissionFilter() ===");

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
				new int[] { WasabiPermission.COMMENT, WasabiPermission.EXECUTE, WasabiPermission.WRITE });

		UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
		utx.begin();
		System.out.println("Creating 2000 documents with VIEW and 2000 without VIEW... ");
		int c = 0;
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 10; j++) {
				documentService().create(new Integer(c).toString(), testRoom);
				c++;
			}

			for (int j = 0; j < 10; j++) {
				WasabiDocumentDTO doc = documentService().create(new Integer(c).toString(), testRoom);
				aclService().create(doc, user, WasabiPermission.VIEW, true);
				c++;
			}
		}
		utx.commit();

		System.out.println("Using permissionFilter... ");

		long startTime = java.lang.System.currentTimeMillis();
		Vector<WasabiDocumentDTO> docs = documentService().getDocuments(testRoom);
		long endTime = java.lang.System.currentTimeMillis();

		System.out.println("Getting " + docs.size() + " in " + (endTime - startTime));
		
		System.out.println("Using permissionFilter with certificates... ");

		long startTimeC = java.lang.System.currentTimeMillis();
		Vector<WasabiDocumentDTO> docsC = documentService().getDocuments(testRoom);
		long endTimeC = java.lang.System.currentTimeMillis();

		System.out.println("Getting " + docsC.size() + " in " + (endTimeC - startTimeC));

		System.out.println("===========================");

	}

}
