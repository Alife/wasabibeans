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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import de.wasabibeans.framework.server.core.bean.UserService;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.AttributeServiceImpl;
import de.wasabibeans.framework.server.core.internal.ContainerServiceImpl;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.LinkServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.internal.TagServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless
public class TestHelper implements TestHelperRemote, TestHelperLocal {

	@Resource
	SessionContext ctx;
	private JcrConnector jcr;
	private JmsConnector jms;

	private JndiConnector jndi;

	@EJB
	SharedFilterBean sharedFilterBean;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <V> V call(Callable<V> callable) throws Exception {
		return callable.call();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> createManyNodes(int number) throws Exception {
		Session s = jcr.getJCRSession();
		try {
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
			return nodeIds;
		} finally {
			jcr.cleanup(true);
		}
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getChildrenByFilter(String parentId) throws Exception {
		Session s = jcr.getJCRSession();
		try {
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
		} finally {
			jcr.cleanup(true);
		}
	}

	@SuppressWarnings("deprecation")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getChildrenByQuery(String parentId) throws Exception {
		Session s = jcr.getJCRSession();
		try {
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
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession();
		try {
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
			ids.add("ByIdFilter: " + (System.currentTimeMillis() - startTime) + "ms");
			return ids;
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Vector<String> ids = new Vector<String>();
			Long startTime = System.currentTimeMillis();
			Node aNode;
			for (String id : nodeIds) {
				aNode = s.getNodeByIdentifier(id);
				ids.add(aNode.getIdentifier());
			}
			ids.add("ByIdLookup: " + (System.currentTimeMillis() - startTime) + "ms");
			return ids;
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initACLTimeEntryCleaner() throws Exception {
		if (ctx.getInvokedBusinessInterface().equals(TestHelperLocal.class)) {
			WasabiManager.initACLTimeEntryCleaner("test");
		} else {
			WasabiManager.initACLTimeEntryCleaner(null);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiAttributeDTO initAttributeServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node attribute1Node = AttributeServiceImpl.create("attribute1", "attribute1", wasabiRootNode, s, false,
					WasabiConstants.ROOT_USER_NAME);
			AttributeServiceImpl.create("attribute2", wasabiRootNode, wasabiRootNode, s, false,
					WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(attribute1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiContainerDTO initContainerServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node container1Node = ContainerServiceImpl.create("container1", wasabiRootNode, s, false,
					WasabiConstants.ROOT_USER_NAME);
			ContainerServiceImpl.create("container2", wasabiRootNode, s, false, WasabiConstants.ROOT_USER_NAME);
			ContainerServiceImpl.create("container3", wasabiRootNode, s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(container1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initDatabase() {
		WasabiManager.initDatabase();

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public WasabiDocumentDTO initDocumentServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node document1Node = DocumentServiceImpl.create("document1", wasabiRootNode, s, false,
					WasabiConstants.ROOT_USER_NAME);
			DocumentServiceImpl.setContentPiped(document1Node, "document1", s, false, jms, sharedFilterBean,
					WasabiConstants.ROOT_USER_NAME);
			DocumentServiceImpl.create("document2", wasabiRootNode, s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(document1Node);
		} finally {
			jcr.cleanup(false);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiGroupDTO initGroupServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node group1Node = GroupServiceImpl.create("group1", null, s, false, WasabiConstants.ROOT_USER_NAME);
			Node group1_1Node = GroupServiceImpl.create("group1_1", group1Node, s, false,
					WasabiConstants.ROOT_USER_NAME);
			Node user1 = UserServiceImpl.create("user1", "user1", s, false, WasabiConstants.ROOT_USER_NAME);
			Node user2 = UserServiceImpl.create("user2", "user2", s, false, WasabiConstants.ROOT_USER_NAME);
			GroupServiceImpl.addMember(group1_1Node, user1, s, false);
			GroupServiceImpl.addMember(group1_1Node, user2, s, false);
			GroupServiceImpl.create("group1_2", group1Node, s, false, WasabiConstants.ROOT_USER_NAME);
			GroupServiceImpl.create("group1_1_1", group1_1Node, s, false, WasabiConstants.ROOT_USER_NAME);
			GroupServiceImpl.create("group1_1_2", group1_1Node, s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(group1_1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiLinkDTO initLinkServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node link1Node = LinkServiceImpl.create("link1", wasabiRootNode, wasabiRootNode, s, false,
					WasabiConstants.ROOT_USER_NAME);
			LinkServiceImpl.create("link2", wasabiRootNode, wasabiRootNode, s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(link1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiRoomDTO initRepository() throws Exception {
		return TransferManager.convertNode2DTO(WasabiManager.initRepository(null, true));
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiRoomDTO initRoomServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node room1Node = RoomServiceImpl.create("room1", wasabiRootNode, s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
			return TransferManager.convertNode2DTO(room1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initScheduledTasks() throws Exception {
		if (ctx.getInvokedBusinessInterface().equals(TestHelperLocal.class)) {
			WasabiManager.initScheduledTasks("test");
		} else {
			WasabiManager.initScheduledTasks(null);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initTagServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			TagServiceImpl.addTag(wasabiRootNode, "tag1", s, false, WasabiConstants.ROOT_USER_NAME);
			TagServiceImpl.addTag(wasabiRootNode, "tag2", s, false, WasabiConstants.ROOT_USER_NAME);
			s.save();
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void initTestUser() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node user = UserServiceImpl.create("user", "user", s, false, WasabiConstants.ROOT_USER_NAME);

			ACLServiceImpl.create(GroupServiceImpl.getWasabiGroup(s), user, new int[] { WasabiPermission.GRANT },
					new boolean[] { true }, 0, 0);

			ACLServiceImpl.create(GroupServiceImpl.getAdminGroup(s), user, new int[] { WasabiPermission.VIEW,
					WasabiPermission.GRANT }, new boolean[] { true, true }, 0, 0);

			s.save();
		} finally {
			jcr.cleanup(true);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public WasabiUserDTO initUserServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node user1Node = UserServiceImpl.create("user1", "user1", s, false, "user");
			UserServiceImpl.create("user2", "user2", s, false, "user");
			s.save();
			return TransferManager.convertNode2DTO(user1Node);
		} finally {
			jcr.cleanup(true);
		}
	}

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
		this.jms = JmsConnector.getJmsConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void deactivateUser(String user) throws Exception {
		Session s = jcr.getJCRSession();
		try {
			Node userNode = UserServiceImpl.getUserByName(user, s);
			UserServiceImpl.setStatus(userNode, false, s, true, user);
		} finally {
			jcr.cleanup(true);
		}
	}
}
