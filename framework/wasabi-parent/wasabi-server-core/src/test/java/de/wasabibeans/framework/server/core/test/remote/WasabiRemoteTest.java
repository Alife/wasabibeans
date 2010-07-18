package de.wasabibeans.framework.server.core.test.remote;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;
import de.wasabibeans.framework.server.core.remote.AttributeServiceRemote;
import de.wasabibeans.framework.server.core.remote.AuthorizationServiceRemote;
import de.wasabibeans.framework.server.core.remote.ContainerServiceRemote;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;
import de.wasabibeans.framework.server.core.remote.GroupServiceRemote;
import de.wasabibeans.framework.server.core.remote.LinkServiceRemote;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.TagServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.testhelper.TestDataCreator;
import de.wasabibeans.framework.server.core.testhelper.TestDataCreatorRemote;
import de.wasabibeans.framework.server.core.util.HashGenerator;

public class WasabiRemoteTest extends Arquillian {

	private RemoteWasabiConnector reWaCon;
	protected TestDataCreatorRemote testhelper;

	protected WasabiRoomDTO rootRoom;

	private ACLServiceRemote aclService;
	private AttributeServiceRemote attributeService;
	private AuthorizationServiceRemote authorizationService;
	private ContainerServiceRemote containerService;
	private DocumentServiceRemote documentService;
	private GroupServiceRemote groupService;
	private LinkServiceRemote linkService;
	private RoomServiceRemote roomService;
	private TagServiceRemote tagService;
	private UserServiceRemote userService;

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
				.addPackage(WasabiUserACL.class.getPackage()) // authorization
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(TestDataCreator.class.getPackage()) // testhelper
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(RoomServiceImpl.class.getPackage()); // internal

		return testArchive;
	}

	@BeforeClass
	public void setUpBeforeAllMethods() throws LoginException, NamingException {
		// connect and login
		reWaCon = new RemoteWasabiConnector();
		reWaCon.defaultConnectAndLogin();

		// lookup wasabi testhelper
		testhelper = (TestDataCreatorRemote) reWaCon.lookup("TestDataCreator/remote");
	}

	@AfterClass
	public void tearDownAfterAllMethods() throws LoginException, NamingException {
		// disconnect and logout
		reWaCon.disconnect();
	}

	public ACLServiceRemote aclService() {
		try {
			if (aclService == null) {
				aclService = (ACLServiceRemote) reWaCon.lookup("ACLService/remote");
			}
			return aclService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AttributeServiceRemote attributeService() {
		try {
			if (attributeService == null) {
				attributeService = (AttributeServiceRemote) reWaCon.lookup("AttributeService/remote");
			}
			return attributeService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AuthorizationServiceRemote authorizationService() {
		try {
			if (authorizationService == null) {
				authorizationService = (AuthorizationServiceRemote) reWaCon.lookup("AuthorizationService/remote");
			}
			return authorizationService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ContainerServiceRemote containerService() {
		try {
			if (containerService == null) {
				containerService = (ContainerServiceRemote) reWaCon.lookup("ContainerService/remote");
			}
			return containerService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DocumentServiceRemote documentService() {
		try {
			if (documentService == null) {
				documentService = (DocumentServiceRemote) reWaCon.lookup("DocumentService/remote");
			}
			return documentService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public GroupServiceRemote groupService() {
		try {
			if (groupService == null) {
				groupService = (GroupServiceRemote) reWaCon.lookup("GroupService/remote");
			}
			return groupService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LinkServiceRemote linkService() {
		try {
			if (linkService == null) {
				linkService = (LinkServiceRemote) reWaCon.lookup("LinkService/remote");
			}
			return linkService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public RoomServiceRemote roomService() {
		try {
			if (roomService == null) {
				roomService = (RoomServiceRemote) reWaCon.lookup("RoomService/remote");
			}
			return roomService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TagServiceRemote tagService() {
		try {
			if (tagService == null) {
				tagService = (TagServiceRemote) reWaCon.lookup("TagService/remote");
			}
			return tagService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public UserServiceRemote userService() {
		try {
			if (userService == null) {
				userService = (UserServiceRemote) reWaCon.lookup("UserService/remote");
			}
			return userService;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
