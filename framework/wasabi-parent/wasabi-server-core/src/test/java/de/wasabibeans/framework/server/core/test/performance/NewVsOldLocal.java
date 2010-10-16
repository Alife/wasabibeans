package de.wasabibeans.framework.server.core.test.performance;

import java.util.Vector;

import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authentication.SqlLoginModule;
import de.wasabibeans.framework.server.core.authorization.WasabiUserACL;
import de.wasabibeans.framework.server.core.bean.RoomService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.local.AttributeServiceLocal;
import de.wasabibeans.framework.server.core.local.ContainerServiceLocal;
import de.wasabibeans.framework.server.core.local.DocumentServiceLocal;
import de.wasabibeans.framework.server.core.local.GroupServiceLocal;
import de.wasabibeans.framework.server.core.local.LinkServiceLocal;
import de.wasabibeans.framework.server.core.local.RoomServiceLocal;
import de.wasabibeans.framework.server.core.local.TagServiceLocal;
import de.wasabibeans.framework.server.core.local.UserServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.remote.RoomServiceRemote;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelper;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperLocal;
import de.wasabibeans.framework.server.core.test.util.LocalWasabiConnector;
import de.wasabibeans.framework.server.core.util.DebugInterceptor;
import de.wasabibeans.framework.server.core.util.HashGenerator;

/**
 * This class is a collection of tests that are used to compare the performance the new wasabi core to the performance
 * of the old wasabi core. No rights are checked.
 * 
 */
@Run(RunModeType.IN_CONTAINER)
public class NewVsOldLocal extends Arquillian {

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
				.addPackage(TestHelper.class.getPackage()) // testhelper
				.addClass(LocalWasabiConnector.class);

		return testArchive;
	}

	private LocalWasabiConnector loCon;

	private void beforeTest() throws Exception {
		loCon = new LocalWasabiConnector();
		loCon.defaultConnectAndLogin();
		TestHelperLocal testhelper = (TestHelperLocal) loCon.lookup("TestHelper");
		testhelper.initDatabase();
		testhelper.initRepository();
		loCon.disconnect();
	}

	// @Test
	public void createRooms() throws Exception {
		int numberOfRooms = 4800;

		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		long start = System.currentTimeMillis();
		utx.begin();
		for (int i = 0; i < numberOfRooms; i++) {
			// System.out.println(i);
			roomService.create("room" + i, rootRoom);
		}
		utx.commit();
		System.out.println("------ " + (System.currentTimeMillis() - start));

		Thread.sleep(60 * 1000);
		loCon.disconnect();
	}

	// @Test
	public void getRooms() throws Exception {
		int numberOfRooms = 4800;

		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		WasabiRoomDTO rootRoom = roomService.getRootRoom();

		// create rooms
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfRooms; i++) {
			// System.out.println(i);
			roomService.create("room" + i, rootRoom);
		}
		utx.commit();

		long start = System.currentTimeMillis();
		roomService.getRooms(rootRoom);
		System.out.println("------ " + (System.currentTimeMillis() - start));

		Thread.sleep(60 * 1000);
		loCon.disconnect();
	}

	// @Test
	public void createDocumentsWithContent() throws Exception {
		int numberOfDocuments = 2000;

		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		long start = System.currentTimeMillis();
		utx.begin();
		WasabiDocumentDTO document;
		for (int i = 0; i < numberOfDocuments; i++) {
			// System.out.println(i);
			document = documentService.create("document" + i, rootRoom);
			documentService.setContent(document, "test", null);
		}
		utx.commit();
		System.out.println("------ " + (System.currentTimeMillis() - start));

		Thread.sleep(60 * 1000);
		loCon.disconnect();
	}

	// @Test
	public void getAndReadDocuments() throws Exception {
		int numberOfDocuments = 2000;

		beforeTest();
		loCon.defaultConnectAndLogin();
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");

		// create the documents
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		WasabiDocumentDTO document;
		for (int i = 0; i < numberOfDocuments; i++) {
			document = documentService.create("document" + i, rootRoom);
			documentService.setContent(document, "test", null);
		}
		utx.commit();

		utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		long start = System.currentTimeMillis();
		utx.begin();
		for (WasabiDocumentDTO doc : documentService.getDocuments(rootRoom)) {
			documentService.getContent(doc);
		}
		utx.commit();
		System.out.println("------ " + (System.currentTimeMillis() - start));

		Thread.sleep(60 * 1000);
		loCon.disconnect();
	}

	// @Test
	// tests searching within many objects
	public void getUserByDisplayname() throws Exception {
		int numberOfUsers = 4800;
		int whichOneToQuery = 1312;

		beforeTest();
		loCon.defaultConnectAndLogin();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		// create users
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfUsers; i++) {
			// System.out.println("----- " + i);
			userService.create("user" + i, "user" + i);
		}
		utx.commit();

		// query a user
		long start = System.currentTimeMillis();
		userService.getUsersByDisplayName("user" + whichOneToQuery);
		System.out.println("------ " + (System.currentTimeMillis() - start));

		loCon.disconnect();
	}

	// @Test
	public void createDocumentsWithContentConcurrently() throws Throwable {
		final int numberOfDocuments = 200;
		int numberOfUsers = 10;

		beforeTest();
		loCon.defaultConnectAndLogin();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
		documentService.create("initialDoc", roomService.getRootRoom());
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfUsers; i++) {
			userService.create("user" + i, "user" + i);
		}
		utx.commit();
		loCon.disconnect();

		UserThread[] users = new UserThread[numberOfUsers];
		for (int i = 0; i < numberOfUsers; i++) {
			users[i] = new UserThread("user" + i) {

				@Override
				public void run() {
					try {
						synchronized (barrier) {
							barrier.wait();
						}
						LocalWasabiConnector loCon = new LocalWasabiConnector();
						loCon.connect();
						loCon.login(username, username);
						RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
						WasabiRoomDTO rootRoom = roomService.getRootRoom();
						DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
						UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						WasabiDocumentDTO document;
						for (int i = 0; i < numberOfDocuments; i++) {
							document = documentService.create(username + "-document" + i, rootRoom);
							documentService.setContent(document, "test", null);
						}
						utx.commit();
						loCon.disconnect();

					} catch (Throwable t) {
						throwables.add(t);
					}
				}

			};
		}
		executeUserThreads(users);
		Thread.sleep(60 * 1000);
	}

	// @Test
	public void getAndReadDocumentsConcurrently() throws Throwable {
		final int numberOfDocuments = 2000;
		int numberOfUsers = 10;

		beforeTest();
		loCon.defaultConnectAndLogin();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		WasabiRoomDTO rootRoom = roomService.getRootRoom();
		DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
		// create users
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfUsers; i++) {
			userService.create("user" + i, "user" + i);
		}
		utx.commit();
		// create documents
		utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		WasabiDocumentDTO document;
		for (int i = 0; i < numberOfDocuments; i++) {
			document = documentService.create("document" + i, rootRoom);
			documentService.setContent(document, "test", null);
		}
		utx.commit();
		loCon.disconnect();

		UserThread[] users = new UserThread[numberOfUsers];
		for (int i = 0; i < numberOfUsers; i++) {
			users[i] = new UserThread("user" + i) {

				@Override
				public void run() {
					try {
						synchronized (barrier) {
							barrier.wait();
						}

						LocalWasabiConnector loCon = new LocalWasabiConnector();
						loCon.connect();
						loCon.login(username, username);
						RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
						WasabiRoomDTO rootRoom = roomService.getRootRoom();
						DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
						UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (WasabiDocumentDTO doc : documentService.getDocuments(rootRoom)) {
							documentService.getContent(doc);
						}
						utx.commit();
						loCon.disconnect();

					} catch (Throwable t) {
						throwables.add(t);
					}
				}

			};
		}
		executeUserThreads(users);
	}

	// @Test
	/*
	 * concurrently: create users and groups, add members to wasabiGroup, remove members, delete some groups and users
	 * again
	 */
	public void adminScenarioConcurrently() throws Throwable {
		final int numberOfGroupsAndUsers = 200;
		int numberOfAdmins = 10;

		beforeTest();
		loCon.defaultConnectAndLogin();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		GroupServiceLocal groupService = (GroupServiceLocal) loCon.lookup("GroupService");
		groupService.create("initialSubgroup", groupService.getGroupByName(WasabiConstants.WASABI_GROUP_NAME));
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfAdmins; i++) {
			userService.create("admin" + i, "admin" + i);
		}
		utx.commit();
		loCon.disconnect();

		UserThread[] users = new UserThread[numberOfAdmins];
		for (int i = 0; i < numberOfAdmins; i++) {
			users[i] = new UserThread("admin" + i) {

				@Override
				public void run() {
					try {
						synchronized (barrier) {
							barrier.wait();
						}

						LocalWasabiConnector loCon = new LocalWasabiConnector();
						loCon.connect();
						loCon.login(username, username);
						GroupServiceLocal groupService = (GroupServiceLocal) loCon.lookup("GroupService");
						UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
						WasabiGroupDTO wasabiGroup = groupService.getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
						WasabiUserDTO[] users = new WasabiUserDTO[numberOfGroupsAndUsers];
						WasabiGroupDTO[] groups = new WasabiGroupDTO[numberOfGroupsAndUsers];

						// create
						UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (int i = 0; i < numberOfGroupsAndUsers; i++) {
							// System.out.println(username + "creates " + i);
							users[i] = userService.create(username + "-user" + i, "pwd");
							groups[i] = groupService.create(username + "-group" + i, wasabiGroup);
							groupService.addMember(wasabiGroup, users[i]);
						}
						utx.commit();

						// add members
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (WasabiUserDTO aUser : users) {
							// System.out.println(username + "adds member");
							groupService.addMember(wasabiGroup, aUser);
						}
						utx.commit();

						// delete groups
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (int i = 0; i < groups.length - 1; i++) {
							if (i % 10 == 0) {
								// System.out.println(username + "deletes group " + i);
								groupService.remove(groups[i], null);
							}
						}
						utx.commit();

						// remove members
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (int i = 0; i < users.length - 1; i++) {
							if (i % 10 == 0) {
								// System.out.println(username + "removes member " + i);
								groupService.removeMember(wasabiGroup, users[i]);
							}
						}
						utx.commit();

						loCon.disconnect();

					} catch (Throwable t) {
						throwables.add(t);
					}
				}

			};
		}
		executeUserThreads(users);
		Thread.sleep(60 * 1000);
	}

	@Test
	/*
	 * concurrently: create room-link-container-document-attribute-'groups' and delete/edit some stuff
	 */
	public void userScenarioConcurrently() throws Throwable {
		final int numberOfObjectGroups = 100;
		int numberOfUsers = 10;

		beforeTest();
		loCon.defaultConnectAndLogin();
		UserServiceLocal userService = (UserServiceLocal) loCon.lookup("UserService");
		RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
		roomService.create("initialSubroom", roomService.getRootRoom());
		UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
		utx.begin();
		for (int i = 0; i < numberOfUsers; i++) {
			userService.create("user" + i, "user" + i);
		}
		utx.commit();
		loCon.disconnect();

		UserThread[] users = new UserThread[numberOfUsers];
		for (int i = 0; i < numberOfUsers; i++) {
			users[i] = new UserThread("user" + i) {

				@Override
				public void run() {
					try {
						synchronized (barrier) {
							barrier.wait();
						}

						LocalWasabiConnector loCon = new LocalWasabiConnector();
						loCon.connect();
						loCon.login(username, username);
						RoomServiceLocal roomService = (RoomServiceLocal) loCon.lookup("RoomService");
						LinkServiceLocal linkService = (LinkServiceLocal) loCon.lookup("LinkService");
						ContainerServiceLocal containerService = (ContainerServiceLocal) loCon
								.lookup("ContainerService");
						DocumentServiceLocal documentService = (DocumentServiceLocal) loCon.lookup("DocumentService");
						AttributeServiceLocal attributeService = (AttributeServiceLocal) loCon
								.lookup("AttributeService");
						TagServiceLocal tagService = (TagServiceLocal) loCon.lookup("TagService");

						WasabiRoomDTO[] rooms = new WasabiRoomDTO[numberOfObjectGroups];
						WasabiLinkDTO[] links = new WasabiLinkDTO[numberOfObjectGroups];
						WasabiContainerDTO[] containers = new WasabiContainerDTO[numberOfObjectGroups];
						WasabiDocumentDTO[] documents = new WasabiDocumentDTO[numberOfObjectGroups];
						WasabiAttributeDTO[] attributes = new WasabiAttributeDTO[numberOfObjectGroups];
						WasabiRoomDTO rootRoom = roomService.getRootRoom();

						// create
						UserTransaction utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (int i = 0; i < numberOfObjectGroups; i++) {
							System.out.println(username + " creates " + i);
							rooms[i] = roomService.create(username + "-room" + i, rootRoom);
							links[i] = linkService.create(username + "-link" + i, rootRoom, rooms[i]);
							containers[i] = containerService.create(username + "-container" + i, rooms[i]);
							documents[i] = documentService.create(username + "-document" + i, containers[i]);
							attributes[i] = attributeService.create(username + "-attribute" + i, "test", documents[i]);
						}
						utx.commit();

						// edit documents
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (WasabiDocumentDTO document : documents) {
							System.out.println(username + " edits document");
							documentService.setContent(document, "newTest", null);
						}
						utx.commit();

						// add tags
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (WasabiContainerDTO container : containers) {
							System.out.println(username + " adds tag");
							tagService.addTag(container, "test");
						}
						utx.commit();

						// remove some rooms
						utx = (UserTransaction) loCon.lookupGeneral("UserTransaction");
						utx.begin();
						for (int i = 0; i < rooms.length - 1; i++) {
							if (i % 10 == 0) {
								System.out.println(username + " removes room " + i);
								roomService.remove(rooms[i], null);
							}
						}
						utx.commit();

						loCon.disconnect();

					} catch (Throwable t) {
						throwables.add(t);
					}
				}

			};
		}
		executeUserThreads(users);
		Thread.sleep(60 * 1000);
	}

	// ------------- stuff for concurrent executions ----------------------------------------------------
	private static Object barrier = new Object();

	abstract class UserThread extends Thread {
		protected String username;
		protected Vector<Throwable> throwables;

		public UserThread(String username) {
			super();
			this.username = username;
		}

		public void setThrowables(Vector<Throwable> throwables) {
			this.throwables = throwables;
		}

		@Override
		public abstract void run();
	}

	public void executeUserThreads(UserThread[] users) throws Throwable {
		Vector<Throwable> throwables = new Vector<Throwable>();
		for (UserThread user : users) {
			user.setThrowables(throwables);
			user.start();
		}

		Thread.sleep(500);

		long start = System.currentTimeMillis();
		synchronized (barrier) {
			barrier.notifyAll();
		}
		for (UserThread user : users) {
			user.join();
		}
		System.out.println("------ " + (System.currentTimeMillis() - start));

		for (Throwable t : throwables) {
			t.printStackTrace();
		}
		if (!throwables.isEmpty()) {
			throw throwables.get(0);
		}
	}
}
