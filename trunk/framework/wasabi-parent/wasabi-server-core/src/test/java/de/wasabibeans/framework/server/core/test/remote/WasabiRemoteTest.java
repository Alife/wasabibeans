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

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;
import de.wasabibeans.framework.server.core.remote.AttributeServiceRemote;
import de.wasabibeans.framework.server.core.remote.AuthorizationServiceRemote;
import de.wasabibeans.framework.server.core.remote.ContainerServiceRemote;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;
import de.wasabibeans.framework.server.core.remote.EventServiceRemote;
import de.wasabibeans.framework.server.core.remote.GroupServiceRemote;
import de.wasabibeans.framework.server.core.remote.LinkServiceRemote;
import de.wasabibeans.framework.server.core.remote.LockingServiceRemote;
import de.wasabibeans.framework.server.core.remote.ObjectServiceRemote;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.TagServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.remote.VersioningServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;

public class WasabiRemoteTest extends Arquillian {

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
				.addPackage(WasabiUserACL.class.getPackage()) // authorization
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(WasabiException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(Locker.class.getPackage()) // locking
				.addPackage(WasabiEventType.class.getPackage()) // event
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(WasabiAOP.class.getPackage()) // AOP
				.addPackage(Filter.class.getPackage()) // pipes.filter
				.addPackage(FilterField.class.getPackage()) // pipes.filter.annotation
				.addPackage(DocumentSource.class.getPackage()) // pipes.filter.impl
				.addPackage(AuthTokenDelegate.class.getPackage()) // pipes.auth
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(RoomServiceImpl.class.getPackage()) // internal
				.addPackage(DebugInterceptor.class.getPackage()) // debug
				.addPackage(TestHelper.class.getPackage()); // testhelper

		return testArchive;
	}

	private ACLServiceRemote aclService;

	private AttributeServiceRemote attributeService;
	private AuthorizationServiceRemote authorizationService;
	private ContainerServiceRemote containerService;
	private DocumentServiceRemote documentService;
	private EventServiceRemote eventService;
	private GroupServiceRemote groupService;
	private LinkServiceRemote linkService;
	private LockingServiceRemote lockingService;
	private ObjectServiceRemote objectService;
	protected RemoteWasabiConnector reWaCon;
	private RoomServiceRemote roomService;
	protected WasabiRoomDTO rootRoom;
	private TagServiceRemote tagService;
	private UserServiceRemote userService;

	private VersioningServiceRemote versioningService;

	public ACLServiceRemote aclService() {
		try {
			if (aclService == null) {
				aclService = (ACLServiceRemote) reWaCon.lookup("ACLService");
			}
			return aclService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AttributeServiceRemote attributeService() {
		try {
			if (attributeService == null) {
				attributeService = (AttributeServiceRemote) reWaCon.lookup("AttributeService");
			}
			return attributeService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AuthorizationServiceRemote authorizationService() {
		try {
			if (authorizationService == null) {
				authorizationService = (AuthorizationServiceRemote) reWaCon.lookup("AuthorizationService");
			}
			return authorizationService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ContainerServiceRemote containerService() {
		try {
			if (containerService == null) {
				containerService = (ContainerServiceRemote) reWaCon.lookup("ContainerService");
			}
			return containerService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DocumentServiceRemote documentService() {
		try {
			if (documentService == null) {
				documentService = (DocumentServiceRemote) reWaCon.lookup("DocumentService");
			}
			return documentService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public EventServiceRemote eventService() {
		try {
			if (eventService == null) {
				eventService = (EventServiceRemote) reWaCon.lookup("EventService");
			}
			return eventService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public GroupServiceRemote groupService() {
		try {
			if (groupService == null) {
				groupService = (GroupServiceRemote) reWaCon.lookup("GroupService");
			}
			return groupService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LinkServiceRemote linkService() {
		try {
			if (linkService == null) {
				linkService = (LinkServiceRemote) reWaCon.lookup("LinkService");
			}
			return linkService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LockingServiceRemote lockingService() {
		try {
			if (lockingService == null) {
				lockingService = (LockingServiceRemote) reWaCon.lookup("LockingService");
			}
			return lockingService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectServiceRemote objectService() {
		try {
			if (objectService == null) {
				objectService = (ObjectServiceRemote) reWaCon.lookup("ObjectService");
			}
			return objectService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public RoomServiceRemote roomService() {
		try {
			if (roomService == null) {
				roomService = (RoomServiceRemote) reWaCon.lookup("RoomService");
			}
			return roomService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws LoginException, NamingException {
		// connect
		reWaCon = new RemoteWasabiConnector();
		reWaCon.connect();
	}

	public TagServiceRemote tagService() {
		try {
			if (tagService == null) {
				tagService = (TagServiceRemote) reWaCon.lookup("TagService");
			}
			return tagService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@AfterClass
	public void tearDownAfterAllMethods() throws LoginException, NamingException, UnexpectedInternalProblemException {
		// disconnect and logout
		reWaCon.disconnect();
	}

	public UserServiceRemote userService() {
		try {
			if (userService == null) {
				userService = (UserServiceRemote) reWaCon.lookup("UserService");
			}
			return userService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public VersioningServiceRemote versioningService() {
		try {
			if (versioningService == null) {
				versioningService = (VersioningServiceRemote) reWaCon.lookup("VersioningService");
			}
			return versioningService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
