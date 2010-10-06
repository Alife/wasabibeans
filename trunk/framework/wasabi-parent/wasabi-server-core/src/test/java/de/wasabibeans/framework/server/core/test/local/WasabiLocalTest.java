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

package de.wasabibeans.framework.server.core.test.local;

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
import de.wasabibeans.framework.server.core.local.ACLServiceLocal;
import de.wasabibeans.framework.server.core.local.AttributeServiceLocal;
import de.wasabibeans.framework.server.core.local.AuthorizationServiceLocal;
import de.wasabibeans.framework.server.core.local.CertificateServiceLocal;
import de.wasabibeans.framework.server.core.local.ContainerServiceLocal;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.local.EventServiceLocal;
import de.wasabibeans.framework.server.core.local.GroupServiceLocal;
import de.wasabibeans.framework.server.core.local.LinkServiceLocal;
import de.wasabibeans.framework.server.core.local.LockingServiceLocal;
import de.wasabibeans.framework.server.core.local.ObjectServiceLocal;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.local.TagServiceLocal;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.local.VersioningServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;

public class WasabiLocalTest extends Arquillian {

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
				.addPackage(RoomServiceLocal.class.getPackage()) // bean Local
				.addPackage(RoomServiceImpl.class.getPackage()) // internal
				.addPackage(DebugInterceptor.class.getPackage()) // debug
				.addPackage(TestHelper.class.getPackage()); // testhelper

		return testArchive;
	}

	private ACLServiceLocal aclService;

	private AttributeServiceLocal attributeService;
	private AuthorizationServiceLocal authorizationService;
	private CertificateServiceLocal certificateService;
	private ContainerServiceLocal containerService;
	private DocumentServiceLocal documentService;
	private EventServiceLocal eventService;
	private GroupServiceLocal groupService;
	private LinkServiceLocal linkService;
	private LockingServiceLocal lockingService;
	private ObjectServiceLocal objectService;
	protected LocalWasabiConnector reWaCon;
	private RoomServiceLocal roomService;
	protected WasabiRoomDTO rootRoom;
	private TagServiceLocal tagService;
	private UserServiceLocal userService;

	private VersioningServiceLocal versioningService;

	public ACLServiceLocal aclService() {
		try {
			if (aclService == null) {
				aclService = (ACLServiceLocal) reWaCon.lookup("ACLService");
			}
			return aclService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AttributeServiceLocal attributeService() {
		try {
			if (attributeService == null) {
				attributeService = (AttributeServiceLocal) reWaCon.lookup("AttributeService");
			}
			return attributeService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AuthorizationServiceLocal authorizationService() {
		try {
			if (authorizationService == null) {
				authorizationService = (AuthorizationServiceLocal) reWaCon.lookup("AuthorizationService");
			}
			return authorizationService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public CertificateServiceLocal certificateService() {
		try {
			if (certificateService == null) {
				certificateService = (CertificateServiceLocal) reWaCon.lookup("CertificateService");
			}
			return certificateService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ContainerServiceLocal containerService() {
		try {
			if (containerService == null) {
				containerService = (ContainerServiceLocal) reWaCon.lookup("ContainerService");
			}
			return containerService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DocumentServiceLocal documentService() {
		try {
			if (documentService == null) {
				documentService = (DocumentServiceLocal) reWaCon.lookup("DocumentService");
			}
			return documentService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public EventServiceLocal eventService() {
		try {
			if (eventService == null) {
				eventService = (EventServiceLocal) reWaCon.lookup("EventService");
			}
			return eventService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public GroupServiceLocal groupService() {
		try {
			if (groupService == null) {
				groupService = (GroupServiceLocal) reWaCon.lookup("GroupService");
			}
			return groupService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LinkServiceLocal linkService() {
		try {
			if (linkService == null) {
				linkService = (LinkServiceLocal) reWaCon.lookup("LinkService");
			}
			return linkService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LockingServiceLocal lockingService() {
		try {
			if (lockingService == null) {
				lockingService = (LockingServiceLocal) reWaCon.lookup("LockingService");
			}
			return lockingService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectServiceLocal objectService() {
		try {
			if (objectService == null) {
				objectService = (ObjectServiceLocal) reWaCon.lookup("ObjectService");
			}
			return objectService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public RoomServiceLocal roomService() {
		try {
			if (roomService == null) {
				roomService = (RoomServiceLocal) reWaCon.lookup("RoomService");
			}
			return roomService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws LoginException, NamingException {
		// connect
		reWaCon = new LocalWasabiConnector();
		reWaCon.connect();
	}

	public TagServiceLocal tagService() {
		try {
			if (tagService == null) {
				tagService = (TagServiceLocal) reWaCon.lookup("TagService");
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

	public UserServiceLocal userService() {
		try {
			if (userService == null) {
				userService = (UserServiceLocal) reWaCon.lookup("UserService");
			}
			return userService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public VersioningServiceLocal versioningService() {
		try {
			if (versioningService == null) {
				versioningService = (VersioningServiceLocal) reWaCon.lookup("VersioningService");
			}
			return versioningService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
