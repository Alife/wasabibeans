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

package de.wasabibeans.framework.server.core.test.remote;

import java.util.Vector;

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class UserServiceRemoteTest extends WasabiRemoteTest {
	
	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize jcr repository
		rootRoom = testhelper.initWorkspace("default");

		// initialize database
		testhelper.initDatabase();
	}

	@Test
	public void createTest() throws WasabiException {
		WasabiUserDTO user = userService().create("user", "pwd");
		AssertJUnit.assertEquals("user", userService().getDisplayName(user));
		WasabiRoomDTO homeRoom = roomService().getRoomByName(roomService().getRootHome(), "user");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService().getHomeRoom(user));
		AssertJUnit.assertEquals(homeRoom, userService().getStartRoom(user));
		AssertJUnit.assertEquals(HashGenerator.generateHash("pwd",
					hashAlgorithms.SHA), userService().getPassword(user));
		
		try {
			userService().create(null, "pwd");
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			userService().create("name", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			userService().create("user", "pwd");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}
	
	@Test
	public void getUsersByDisplayName() throws WasabiException {
		Vector<WasabiUserDTO> users = userService().getUsersByDisplayName("root");
		AssertJUnit.assertEquals(1, users.size());
		AssertJUnit.assertEquals("root", userService().getName(users.get(0)));
	}
}
