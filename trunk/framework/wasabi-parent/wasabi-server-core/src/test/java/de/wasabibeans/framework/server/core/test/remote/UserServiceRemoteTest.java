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

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.manager.WasabiManagerRemote;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class UserServiceRemoteTest extends Arquillian {

	private RemoteWasabiConnector reWaCon;
	private WasabiManagerRemote waMan;

	private UserServiceRemote userService;
	private RoomServiceRemote roomService;

	private WasabiRoomDTO rootRoom;

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // auth
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(RoomServiceImpl.class.getPackage());

		return testArchive;
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws LoginException, NamingException {
		// connect and login
		reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		// lookup wasabi manager
		waMan = (WasabiManagerRemote) reWaCon.lookup("WasabiManager/remote");

		// lookup services
		userService = (UserServiceRemote) reWaCon.lookup("UserService/remote");
		roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
	}

	@AfterClass
	public void tearDownAfterAllMethods() throws LoginException, NamingException {
		// disconnect and logout
		reWaCon.disconnect();
	}

	@BeforeMethod
	public void setUpBeforeEachMethod() throws LoginException, NamingException {
		// initialize jcr repository
		rootRoom = waMan.initWorkspace("default");
		
		// initialize database
		waMan.initDatabase();
	}
	
	@Test
	public void createTest() throws WasabiException {
		WasabiUserDTO user = userService.create("user", "pwd");
		AssertJUnit.assertEquals("user", userService.getDisplayName(user));
		WasabiRoomDTO homeRoom = roomService.getRoomByName(roomService.getRootHome(), "user");
		AssertJUnit.assertNotNull(homeRoom);
		AssertJUnit.assertEquals(homeRoom, userService.getHomeRoom(user));
		AssertJUnit.assertEquals(homeRoom, userService.getStartRoom(user));
		AssertJUnit.assertEquals(HashGenerator.generateHash("pwd",
					hashAlgorithms.SHA), userService.getPassword(user));
		
		try {
			userService.create(null, "pwd");
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			userService.create("name", null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			assert e.getCausedByException() instanceof IllegalArgumentException;
		}

		try {
			userService.create("user", "pwd");
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}
	}
}
