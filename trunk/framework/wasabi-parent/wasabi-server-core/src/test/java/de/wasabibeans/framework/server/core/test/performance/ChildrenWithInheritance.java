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

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class ChildrenWithInheritance extends WasabiRemoteTest {
	
	private final static String USER1 = "user1", USER2 = "user2";
	private String parentId;
	
	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		reWaCon.defaultLogin();
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		rootRoom = testhelper.initRepository();
		testhelper.initDatabase();
		testhelper.initTestUser();
		
		userService().create(USER1, USER1);
		userService().create(USER2, USER2);
		
		WasabiRoomDTO parentRoom = roomService().create("parent", rootRoom);
		parentId = roomService().getUUID(parentRoom);
		
		for (int i = 0; i < 500; i++) {
			roomService().create("room" + i, parentRoom);
		}
		reWaCon.logout();
	}	
	
	@Test
	public void byQuery() throws Exception {
		reWaCon.login(USER1, USER1);
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		Vector<String> result = testhelper.getChildrenByQuery(parentId);
		System.out.println("=========Result byQuery:");
		System.out.println(result.firstElement());
		System.out.println("Returned elements: " + (result.size() - 2));
		System.out.println(result.lastElement());
		System.out.println("================================");
		
		reWaCon.logout();
	}
	
	@Test
	public void byFilter() throws Exception {
		reWaCon.login(USER2, USER2);
		
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		Vector<String> result = testhelper.getChildrenByFilter(parentId);
		System.out.println("=========Result byFilter:");
		System.out.println("Returned elements: " + (result.size() - 1));
		System.out.println(result.lastElement());
		System.out.println("================================");
		
		reWaCon.logout();
	}
	
}

