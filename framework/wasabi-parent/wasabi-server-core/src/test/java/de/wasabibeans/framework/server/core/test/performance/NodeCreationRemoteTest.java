package de.wasabibeans.framework.server.core.test.performance;

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

import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class NodeCreationRemoteTest extends WasabiRemoteTest {

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
	// results: 41306, 41623 
	public void create() throws Exception {
		long start = System.currentTimeMillis();
		UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < 5000; i++) {
			//System.out.println(i);
			roomService().create("hu" + i, rootRoom);
		}
		utx.commit();
		System.out.println("------ " + (System.currentTimeMillis() - start));
		AssertJUnit.assertNotNull(roomService().getRoomByName(rootRoom, "hu4"));
	}
}
