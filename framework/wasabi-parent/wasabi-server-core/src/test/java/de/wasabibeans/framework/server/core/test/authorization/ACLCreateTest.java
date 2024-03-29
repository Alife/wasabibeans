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
public class ACLCreateTest extends WasabiRemoteTest {

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
	public void createTest() throws WasabiException {
		// Create user
		WasabiUserDTO user = userService().create("aclTestUser", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user).getValue();
		WasabiDocumentDTO document = documentService().create("UserTestDocu", usersHome);
		aclService().create(document, user, new int[] { WasabiPermission.VIEW, WasabiPermission.READ },
				new boolean[] { true, true });

		// try to change content of document without rights
		// documentService.setContent(document, "May the force be with you!");

		// add write right
		aclService().create(document, user, WasabiPermission.WRITE, true);

		// change content of document
		// documentService.setContent(document, "You should have seen his face, when I told him I was his father...");

		// remove write right
		aclService().remove(document, user, WasabiPermission.WRITE);
		aclService().remove(document, user, WasabiPermission.VIEW);
		aclService().remove(document, user, WasabiPermission.READ);

		// try to change content of document without rights
		// documentService.setContent(document, "...dark...light...dark...light...");

		// delete all rights from document
		// aclService.reset(document);
	}
}
