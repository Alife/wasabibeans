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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.util.JcrConnector;

@Stateless
public class TestHelper implements TestHelperRemote, TestHelperLocal {

	private JcrConnector jcr;

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
		Session s = jcr.getJCRSession(new SimpleCredentials("foo", "bar".toCharArray()));
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			Node room1Node = wasabiRootNode.addNode(WasabiNodeProperty.ROOMS + "/" + "room1",
					WasabiNodeType.WASABI_ROOM);
			s.save();
			return TransferManager.convertNode2DTO(room1Node);
		} finally {
			s.logout();
		}
	}

	public void registerEventForDisplayName(WasabiUserDTO user) throws Exception {
		Session s = jcr.getJCRSession(new SimpleCredentials("foo", "bar".toCharArray()));
		Node userNode = TransferManager.convertDTO2Node(user, s);
		try {
			EventListener listener = new DisplayNameListener();
			s.getWorkspace().getObservationManager().addEventListener(listener, Event.PROPERTY_CHANGED,
					userNode.getPath(), false, null, null, false);
			s.save();
		} finally {
			s.logout();
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public <V> V call(Callable<V> callable) throws Exception {
		return callable.call();
	}

	public Vector<String> createManyNodes(int number) throws Exception {
		Vector<String> nodeIds = new Vector<String>();
		Session s = jcr.getJCRSession(new SimpleCredentials("foo", "bar".toCharArray()));
		try {
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
			s.logout();
		}
	}

	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession(new SimpleCredentials("foo", "bar".toCharArray()));
		Vector<String> ids = new Vector<String>();
		try {
			Long startTime = System.currentTimeMillis();
			Node aNode;
			for (String id : nodeIds) {
				aNode = s.getNodeByIdentifier(id);
				ids.add(aNode.getIdentifier());
			}
			ids.add("ByIdLookup: " + (System.currentTimeMillis() - startTime) + "ms");
			return ids;
		} finally {
			s.logout();
		}
	}

	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception {
		Session s = jcr.getJCRSession(new SimpleCredentials("foo", "bar".toCharArray()));
		Vector<String> ids = new Vector<String>();
		try {
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
			s.logout();
		}
	}

}
