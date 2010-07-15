package de.wasabibeans.framework.server.core.test.autorisation;

import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.DestinationNotFoundException;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.manager.WasabiManagerRemote;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.remote.UserServiceRemote;
import de.wasabibeans.framework.server.core.test.util.RemoteWasabiConnector;
import de.wasabibeans.framework.server.core.util.HashGenerator;

@Run(RunModeType.AS_CLIENT)
public class ACLCreateTest extends Arquillian {

	private RemoteWasabiConnector reWaCon;
	private WasabiManagerRemote waMan;

	private UserServiceRemote userService;
	private ACLServiceRemote aclService;
	private DocumentServiceRemote documentService;

	@Deployment
	public static JavaArchive deploy() {
		JavaArchive testArchive = ShrinkWrap.create("wasabibeans-test.jar", JavaArchive.class);
		testArchive.addPackage(SqlLoginModule.class.getPackage()) // authentication
				.addPackage(WasabiConstants.class.getPackage()) // common
				.addPackage(DestinationNotFoundException.class.getPackage()) // exception
				.addPackage(WasabiRoomDTO.class.getPackage()) // dto
				.addPackage(HashGenerator.class.getPackage()) // util
				.addPackage(WasabiManager.class.getPackage()) // manager
				.addPackage(RoomService.class.getPackage()) // bean impl
				.addPackage(RoomServiceLocal.class.getPackage()) // bean local
				.addPackage(RoomServiceRemote.class.getPackage()) // bean remote
				.addPackage(WasabiUserACL.class.getPackage()) // autorization
				.addPackage(RoomServiceImpl.class.getPackage()); // internal

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
		aclService = (ACLServiceRemote) reWaCon.lookup("ACLService/remote");
		documentService = (DocumentServiceRemote) reWaCon.lookup("DocumentService/remote");
	}

	@AfterClass
	public void tearDownAfterAllMethods() throws LoginException, NamingException {
		// disconnect and logout
		reWaCon.disconnect();
	}

	@BeforeMethod
	public void setUpBeforeEachMethod() throws LoginException, NamingException {
		// initialize jcr repository
		waMan.initWorkspace("default");

		// initialize database
		waMan.initDatabase();
	}

	@Test
	public void createTest() throws WasabiException {
		// Create user
		WasabiUserDTO user = userService.create("aclTestUser", "password");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService.getHomeRoom(user);
		WasabiDocumentDTO document = documentService.create("UserTestDocu", usersHome);
		aclService.create(document, user, new int[] { WasabiPermission.VIEW, WasabiPermission.READ }, new boolean[] {
				true, true });

		// try to change content of document without rights
		//documentService.setContent(document, "May the force be with you!");

		// add write right
		aclService.create(document, user, WasabiPermission.WRITE, true);

		// change content of document
		//documentService.setContent(document, "You should have seen his face, when I told him I was his father...");

		// remove write right
		aclService.remove(document, user, WasabiPermission.WRITE);
		aclService.remove(document, user, WasabiPermission.VIEW);
		aclService.remove(document, user, WasabiPermission.READ);

		// try to change content of document without rights
		//documentService.setContent(document, "...dark...light...dark...light...");

		// delete all rights from document
		//aclService.reset(document);
	}
}
