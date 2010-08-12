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
package de.wasabibeans.framework.server.core.test.testhelper;

import java.util.Vector;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import de.wasabibeans.framework.server.core.authorization.WasabiUserSQL;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.util.JcrConnector;

@Stateless
public class TestHelper implements TestHelperRemote, TestHelperLocal {

	private JcrConnector jcr;

	@Resource
	protected SessionContext ctx;

	public TestHelper() {
		this.jcr = JcrConnector.getJCRConnector();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initDatabase() {
		WasabiManager.initDatabase();

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiRoomDTO initRepository() throws Exception {
		return TransferManager.convertNode2DTO(WasabiManager.initRepository(null, true));
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiRoomDTO initRoomServiceTest() throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
		Node room1Node = RoomServiceImpl.create("room1", wasabiRootNode, "root", s);
		s.save();
		s.logout();
		return TransferManager.convertNode2DTO(room1Node);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiUserDTO initUserServiceTest() throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Node user1Node = UserServiceImpl.create("user1", "user1", s, "user");
		UserServiceImpl.create("user2", "user2", s, "user");
		s.save();
		s.logout();
		return TransferManager.convertNode2DTO(user1Node);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiDocumentDTO initDocumentServiceTest() throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
		Node document1Node = DocumentServiceImpl.create("document1", wasabiRootNode, "root", s);
		DocumentServiceImpl.setContent(document1Node, "document1");
		DocumentServiceImpl.create("document2", wasabiRootNode, "root", s);
		s.save();
		s.logout();
		return TransferManager.convertNode2DTO(document1Node);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initTestUser() throws Exception {
		String name = "user";
		WasabiUserSQL.SqlQueryForCreate(name, name);

		JcrConnector jcr = JcrConnector.getJCRConnector();
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Node rootOfUsersNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
		Node userNode = rootOfUsersNode.addNode(name, WasabiNodeType.WASABI_USER);
		userNode.setProperty(WasabiNodeProperty.DISPLAY_NAME, name);
		Node wasabiHome = s.getRootNode().getNode(
				WasabiConstants.ROOT_ROOM_NAME + "/" + WasabiNodeProperty.ROOMS + "/" + WasabiConstants.HOME_ROOM_NAME);
		Node homeRoomNode = wasabiHome.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.WASABI_ROOM);
		userNode.setProperty(WasabiNodeProperty.HOME_ROOM, homeRoomNode);
		userNode.setProperty(WasabiNodeProperty.START_ROOM, homeRoomNode);
		s.save();
		s.logout();
	}

	public void registerEventForDisplayName(WasabiUserDTO user) throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Node userNode = TransferManager.convertDTO2Node(user, s);
		EventListener listener = new DisplayNameListener();
		s.getWorkspace().getObservationManager().addEventListener(listener, Event.PROPERTY_CHANGED, userNode.getPath(),
				false, null, null, false);
		s.save();
		s.logout();
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <V> V call(Callable<V> callable) throws Exception {
		return callable.call();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> createManyNodes(int number) throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Vector<String> nodeIds = new Vector<String>();
		Node testroot = s.getRootNode().addNode("LookupTest");
		Node aNode;
		for (int i = 0; i < number; i++) {
			aNode = testroot.addNode("Node" + i);
			if (i % 2 == 0) {
				nodeIds.add(aNode.getIdentifier());
			}
		}
		s.save();
		s.logout();
		return nodeIds;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Vector<String> ids = new Vector<String>();
		Long startTime = System.currentTimeMillis();
		Node aNode;
		for (String id : nodeIds) {
			aNode = s.getNodeByIdentifier(id);
			ids.add(aNode.getIdentifier());
		}
		s.logout();
		ids.add("ByIdLookup: " + (System.currentTimeMillis() - startTime) + "ms");
		return ids;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Vector<String> ids = new Vector<String>();
		Long startTime = System.currentTimeMillis();
		NodeIterator ni = s.getRootNode().getNode("LookupTest").getNodes();
		Node aNode;
		int index;
		while (ni.hasNext()) {
			aNode = ni.nextNode();
			index = nodeIds.indexOf(aNode.getIdentifier());
			if (index >= 0) {
				ids.add(aNode.getIdentifier());
				nodeIds.remove(index);
			}
		}
		s.logout();
		ids.add("ByIdFilter: " + (System.currentTimeMillis() - startTime) + "ms");
		return ids;
	}

	@SuppressWarnings("deprecation")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getChildrenByQuery(String parentId) throws Exception {
		Session s = jcr.getJCRSession(ctx);
		Vector<String> result = new Vector<String>();

		// prepare query
		QueryManager qm = s.getWorkspace().getQueryManager();
		String xpathQuery = "//*[@jcr:uuid='" + parentId + "']/*/*[@" + WasabiNodeProperty.INHERITANCE + "='true']";
		result.add("Executed query: " + xpathQuery);
		Query query = qm.createQuery(xpathQuery, Query.XPATH);

		// execute and return result
		Long startTime = System.currentTimeMillis();
		NodeIterator ni = query.execute().getNodes();
		Long endTime = System.currentTimeMillis() - startTime;
		while (ni.hasNext()) {
			result.add(ni.nextNode().getName());
		}
		result.add("ByQuery: " + endTime + "ms");
		return result;
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getChildrenByFilter(String parentId) throws Exception {
		Session s = jcr.getJCRSession(ctx);
		Vector<String> result = new Vector<String>();

		Long startTime = System.currentTimeMillis();
		Node parentNode = s.getNodeByIdentifier(parentId);
		NodeIterator ni = parentNode.getNode(WasabiNodeProperty.ROOMS).getNodes();
		while (ni.hasNext()) {
			Node aNode = ni.nextNode();
			if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean()) {
				result.add(aNode.getName());
			}
		}
		Long endTime = System.currentTimeMillis() - startTime;

		result.add("ByFilter: " + endTime + "ms");
		return result;
	}
}