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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.event.WasabiEventProperty;
import de.wasabibeans.framework.server.core.event.WasabiEventRegistration;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class EventRemoteTest extends WasabiRemoteTest {

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

	@Test
	public void test() throws Exception {
		WasabiDocumentDTO document = documentService().create("test", rootRoom);

		Connection connection = null;
		Session session = null;
		MessageConsumer consumer = null;
		try {
			ConnectionFactory factory = (ConnectionFactory) reWaCon.lookupGeneral("ConnectionFactory");
			connection = factory.createConnection();
			session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			Queue myQueue = session.createTemporaryQueue();
			Message regMsg = session.createMessage();
			regMsg.setStringProperty(WasabiEventRegistration.USERNAME, "user");
			regMsg.setStringProperty(WasabiEventRegistration.PASSWORD, "user");
			regMsg.setStringProperty(WasabiEventRegistration.WASABI_OBJECT_ID, document.getId());
			regMsg.setJMSReplyTo(myQueue);
			connection.start();
			Destination registerDestination = (Destination) reWaCon.lookupGeneral(WasabiConstants.JMS_QUEUE_REGISTRAR);
			MessageProducer sender = session.createProducer(registerDestination);
			sender.send(regMsg);
			sender.close();

			consumer = session.createConsumer(myQueue);
			consumer.setMessageListener(new MessageListener() {

				@Override
				public void onMessage(Message message) {
					try {
						System.out.println("Client got event: "
								+ (message.getStringProperty(WasabiEventProperty.OBJECT_NAME)));
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

			});

			UserTransaction utx = (UserTransaction) reWaCon.lookupGeneral("UserTransaction");
			try {
				utx.begin();
				documentService().setContent(document, "bla", null);
				//throw new Exception();
				utx.commit();
			} catch (Exception e) {
				utx.rollback();
			}
			Thread.sleep(5000);

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
}
