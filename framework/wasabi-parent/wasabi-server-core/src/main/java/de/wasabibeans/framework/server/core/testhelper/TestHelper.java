package de.wasabibeans.framework.server.core.testhelper;

import java.util.Vector;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.manager.WasabiManager;
import de.wasabibeans.framework.server.core.util.JcrConnector;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateful
public class TestHelper implements TestHelperRemote {

	private JcrConnector jcr;

	public TestHelper() {
		this.jcr = JcrConnector.getJCRConnector();
	}

	@Override
	public void initDatabase() {
		WasabiManager.initDatabase();

	}

	@Override
	public void initRepository() throws Exception {
		WasabiManager.initRepository();
	}

	@Override
	public WasabiRoomDTO initWorkspace(String workspacename) throws Exception {
		WasabiManager.initWorkspace("default");
		Session s = jcr.getJCRSession();
		try {
			Node wasabiRootNode = s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
			return TransferManager.convertNode2DTO(wasabiRootNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public WasabiRoomDTO initRoomServiceTest() throws Exception {
		Session s = jcr.getJCRSession();
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

	Vector<String> nodeIds;

	public void createManyNodes(int number) throws Exception {
		nodeIds = new Vector<String>();
		Session s = jcr.getJCRSession();
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
		} finally {
			s.logout();
		}
	}

	public Vector<String> getManyNodesByIdLookup() throws Exception {
		Session s = jcr.getJCRSession();
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
	
	public Vector<String> getManyNodesByIdFilter() throws Exception {
		Session s = jcr.getJCRSession();
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
