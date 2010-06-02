package de.wasabibeans.framework.server.core.jcrTest;

import javax.ejb.Stateful;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.naming.InitialContext;

import de.wasabibeans.framework.server.core.transfer.model.dto.WasabiRoomDTO;

/**
 * Session Bean implementation class ConnectionTest
 */
@Stateful
public class ConnectionTest implements ConnectionTestRemote {
	
	/**
	 * Default constructor.
	 */
	public ConnectionTest() {
		// TODO Auto-generated constructor stub
	}

	public WasabiRoomDTO login() {
		try {
			InitialContext ctx = new InitialContext();
			Repository rep = (Repository) ctx.lookup("java:jcr/local");
			Credentials cred = new SimpleCredentials("user", new char[] { 'p',
					'w', 'd' });
			Session s = rep.login(cred);

			Node rootNode = s.getRootNode();
			Node test = rootNode.addNode("test");
			test.setPrimaryType(NodeType.NT_UNSTRUCTURED);
			s.save();
			
			WasabiRoomDTO root = new WasabiRoomDTO();
			root.setId(test.getIdentifier());
			return root;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
