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

package de.wasabibeans.framework.server.core.test.event;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.EJBException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventProperty;
import de.wasabibeans.framework.server.core.event.WasabiEventType;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class EventRemoteTest extends WasabiRemoteTest {

	private static final String USER1 = "user1", USER2 = "user2", USER3 = "user3";
	private static final String[] users = { USER1, USER2, USER3 };
	private static ConcurrentHashMap<String, Byte> eventReceived;
	private static HashMap<String, Queue> userQueues;

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

	// helper class for testing the event functionality
	private class EventListener implements MessageListener {

		private String username;

		public EventListener(String username) {
			this.username = username;
		}

		@Override
		public void onMessage(Message message) {
			try {
				eventReceived.put(username, message.getByteProperty(WasabiEventProperty.EVENT_TYPE));
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

	// helper method for establishing event subscriptions
	private void subscribe(WasabiObjectDTO object) throws Exception {
		reWaCon.logout();

		for (String user : users) {
			reWaCon.login(user, user);
			eventService().subscribe(object, userQueues.get(user).getQueueName(), true);
			reWaCon.logout();
		}

		reWaCon.login("user", "user");
	}

	// helper method for removing event subscriptions
	private void unsubscribe(WasabiObjectDTO object) throws Exception {
		reWaCon.logout();

		for (String user : users) {
			reWaCon.login(user, user);
			eventService().unsubscribe(object);
			reWaCon.logout();
		}

		reWaCon.login("user", "user");
	}

	// helper method for checking whether expected events have been received
	private void assertEvents(byte expectedType) throws Exception {
		Thread.sleep(500); // slow the test thread down a little bit in order to have a chance to receive the events
		for (String user : users) {
			Byte type = eventReceived.get(user);
			AssertJUnit.assertNotNull(type);
			AssertJUnit.assertTrue(expectedType == type);
		}
	}

	// ------------------------------------------------------------------------------------------

	@Test
	// tests that subscribed users receive events
	public void test() throws Exception {
		// create the test users
		for (String user : users) {
			userService().create(user, user);
		}

		HashMap<String, Connection> connections = new HashMap<String, Connection>();
		HashMap<String, Session> sessions = new HashMap<String, Session>();
		HashMap<String, MessageConsumer> consumers = new HashMap<String, MessageConsumer>();
		userQueues = new HashMap<String, Queue>();
		try {
			ConnectionFactory factory = (ConnectionFactory) reWaCon.lookupGeneral("ConnectionFactory");
			// set up the users as event listeners
			for (String user : users) {
				// jms connection
				connections.put(user, factory.createConnection(user, user));
				// jms session
				sessions.put(user, connections.get(user).createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE));
				// temporary queue for receiving events
				userQueues.put(user, sessions.get(user).createTemporaryQueue());
				// jms consumer
				consumers.put(user, sessions.get(user).createConsumer(userQueues.get(user)));
				// message listener
				consumers.get(user).setMessageListener(new EventListener(user));
				// start connection
				connections.get(user).start();
			}

			eventReceived = new ConcurrentHashMap<String, Byte>();

			// roomService events
			subscribe(rootRoom);
			WasabiRoomDTO testRoom1 = roomService().create("testRoom1", rootRoom);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(rootRoom);

			WasabiRoomDTO testRoom2 = roomService().create("testRoom2", testRoom1);
			subscribe(testRoom2);
//			roomService().move(testRoom2, rootRoom, null);
//			assertEvents(WasabiEventType.MOVED);

			roomService().rename(testRoom2, "new", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			roomService().remove(testRoom2);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(testRoom2);

			// attributeService events
			subscribe(rootRoom);
			WasabiAttributeDTO attribute = attributeService().create("test", "hallo", rootRoom);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(rootRoom);

			subscribe(attribute);
			attributeService().rename(attribute, "test2", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);
			eventReceived.clear();

//			attributeService().move(attribute, testRoom1, null);
//			assertEvents(WasabiEventType.MOVED);

			attributeService().setValue(attribute, "newHallo", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);
			eventReceived.clear(); // next test has same event type

			attributeService().setWasabiValue(attribute, testRoom1, null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			attributeService().remove(attribute);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(attribute);

			// containerService events
			subscribe(rootRoom);
			WasabiContainerDTO container = containerService().create("test", rootRoom);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(rootRoom);

			subscribe(container);
//			containerService().move(container, testRoom1, null);
//			assertEvents(WasabiEventType.MOVED);

			containerService().rename(container, "newName", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			containerService().remove(container);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(container);

			// documentService events
			subscribe(rootRoom);
			WasabiDocumentDTO document = documentService().create("doc", rootRoom);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(rootRoom);

			subscribe(document);
			documentService().setContent(document, "content", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);
			eventReceived.clear(); // next test has same event type

//			documentService().move(document, testRoom1, null);
//			assertEvents(WasabiEventType.MOVED);

			documentService().rename(document, "newDoc", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			documentService().remove(document);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(document);

			// linkService events
			subscribe(rootRoom);
			WasabiLinkDTO link = linkService().create("link", testRoom1, rootRoom);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(rootRoom);

			subscribe(link);
			linkService().setDestination(link, rootRoom, null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			linkService().rename(link, "newLink", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

//			linkService().move(link, testRoom1, null);
//			assertEvents(WasabiEventType.MOVED);

			linkService().remove(link);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(link);

			// userService events
			WasabiUserDTO user = userService().create("testUser", "testUser");
			subscribe(user);
			userService().setDisplayName(user, "newName", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);
			eventReceived.clear(); // next test has same event type

			userService().setStartRoom(user, testRoom1, null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);
			eventReceived.clear(); // next test has same event type

			userService().setStatus(user, true, null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			userService().enter(user, testRoom1);
			assertEvents(WasabiEventType.ROOM_ENTERED);

			userService().leave(user, testRoom1);
			assertEvents(WasabiEventType.ROOM_LEFT);

			userService().rename(user, "testUser2", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			userService().remove(user);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(user);

			// groupService events
			WasabiGroupDTO wasabi = groupService().getGroupByName(WasabiConstants.WASABI_GROUP_NAME);
			subscribe(wasabi);
			WasabiGroupDTO group = groupService().create("group", wasabi);
			assertEvents(WasabiEventType.CREATED);
			unsubscribe(wasabi);

			subscribe(group);
			user = userService().create("testUser", "testUser");
			groupService().addMember(group, user);
			assertEvents(WasabiEventType.MEMBER_ADDED);

//			WasabiGroupDTO group2 = groupService().create("group2", wasabi);
//			groupService().move(group, group2, null);
//			assertEvents(WasabiEventType.MOVED);

			groupService().rename(group, "newGroup", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			groupService().removeMember(group, user);
			assertEvents(WasabiEventType.MEMBER_REMOVED);

			groupService().setDisplayName(group, "newGroup", null);
			assertEvents(WasabiEventType.PROPERTY_CHANGED);

			groupService().remove(group);
			assertEvents(WasabiEventType.REMOVED);
			unsubscribe(group);

		} finally {
			for (String user : users) {
				if (consumers.get(user) != null) {
					consumers.get(user).close();
				}
				if (sessions.get(user) != null) {
					sessions.get(user).close();
				}
				if (connections.get(user) != null) {
					connections.get(user).stop();
					connections.get(user).close();
				}
			}
		}
	}

	@Test
	// tests that events are not dispatched if triggered within a transaction that fails
	public void testTransaction() throws Exception {
		eventReceived = new ConcurrentHashMap<String, Byte>();

		Connection connection = null;
		Session session = null;
		MessageConsumer consumer = null;
		try {
			ConnectionFactory factory = (ConnectionFactory) reWaCon.lookupGeneral("ConnectionFactory");
			// jms connection
			connection = factory.createConnection("user", "user");
			// jms session
			session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			// temporary queue for receiving events
			Queue queue = session.createTemporaryQueue();
			// jms consumer
			consumer = session.createConsumer(queue);
			// message listener
			consumer.setMessageListener(new EventListener("user"));
			// start connection
			connection.start();

			// create test object
			WasabiRoomDTO testRoom = roomService().create("testRoom", rootRoom);

			// subscribe for events
			eventService().subscribe(rootRoom, queue.getQueueName(), true);
			eventService().subscribe(testRoom, queue.getQueueName(), true);

			// trigger events within a transaction that fails
			UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
			utx.begin();
			try {
				roomService().create("anotherTestRoom", rootRoom);
				roomService().remove(testRoom);
				// provoke failure of the transaction
				roomService().create(null, rootRoom);
				utx.commit();
			} catch (Exception e) {
				utx.rollback();
			}

			// slow down a little bit so that incorrectly triggered events (if any) are not missed
			Thread.sleep(500);
			// assert that no events have been received
			AssertJUnit.assertTrue(eventReceived.isEmpty());
		} finally {
			if (consumer != null) {
				consumer.close();
			}
			if (session != null) {
				session.close();
			}
			if (connection != null) {
				connection.stop();
				connection.close();
			}
		}
	}

	@Test
	// tests that no normal wasabi user can interact with the wasabi allocator queue
	public void testSecurity() throws Exception {
		Connection connection = null;
		Session session = null;
		MessageConsumer consumer = null;
		try {
			ConnectionFactory factory = (ConnectionFactory) reWaCon.lookupGeneral("ConnectionFactory");
			// jms connection
			connection = factory.createConnection("user", "user");
			// jms session
			session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			// start connection
			connection.start();

			// try to interact with the allocator queue
			Queue allocatorQueue = (Queue) reWaCon.lookupGeneral("queue/allocator");
			try {
				session.createConsumer(allocatorQueue);
				AssertJUnit.fail();
			} catch (JMSException e) {
				// passed
			}

			try {
				MessageProducer producer = session.createProducer(allocatorQueue);
				producer.send(session.createMessage());
				AssertJUnit.fail();
			} catch (JMSException e) {
				// passed
			}

		} finally {
			if (consumer != null) {
				consumer.close();
			}
			if (session != null) {
				session.close();
			}
			if (connection != null) {
				connection.stop();
				connection.close();
			}
		}
	}

	@Test
	// tests whether the subscribe method of the EventService only takes valid parameters
	public void testValidParameter() throws Exception {
		Connection connection = null;
		Session session = null;
		MessageConsumer consumer = null;
		try {
			ConnectionFactory factory = (ConnectionFactory) reWaCon.lookupGeneral("ConnectionFactory");
			// jms connection
			connection = factory.createConnection("user", "user");
			// jms session
			session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			// create a temporary queue
			Queue queue = session.createTemporaryQueue();
			// create a temporary topic
			Topic topic = session.createTemporaryTopic();

			// test the subscribe method
			try {
				eventService().subscribe(rootRoom, queue.getQueueName(), false);
				AssertJUnit.fail();
			} catch (EJBException e) {
				if (!(e.getCause() instanceof IllegalArgumentException)) {
					AssertJUnit.fail();
				}
			}

			try {
				eventService().subscribe(rootRoom, topic.getTopicName(), true);
				AssertJUnit.fail();
			} catch (EJBException e) {
				if (!(e.getCause() instanceof IllegalArgumentException)) {
					AssertJUnit.fail();
				}
			}
			
			try {
				eventService().subscribe(rootRoom, "randomName", true);
				AssertJUnit.fail();
			} catch (EJBException e) {
				if (!(e.getCause() instanceof IllegalArgumentException)) {
					AssertJUnit.fail();
				}
			}

		} finally {
			if (consumer != null) {
				consumer.close();
			}
			if (session != null) {
				session.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
	}
}
