package de.wasabibeans.framework.server.core.jcrTest;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.naming.InitialContext;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;

/**
 * Session Bean implementation class ConnectionTest
 */
@SecurityDomain("wasabi")
@Stateless
public class ConnectionTest extends TransferManager implements ConnectionTestRemote,
		ConnectionTestLocal {
	
	@Resource
	private SessionContext sessionContext;

	/**
	 * Default constructor.
	 */
	public ConnectionTest() {
		// TODO Auto-generated constructor stub
	}

	public WasabiRoomDTO login() {
		logger.info("LOGGED IN AS: " + sessionContext.getCallerPrincipal());
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

	public String printNodeTypes() {
		try {
			InitialContext ctx = new InitialContext();
			Repository rep = (Repository) ctx.lookup("java:jcr/local");
			Credentials cred = new SimpleCredentials("user", new char[] { 'p',
					'w', 'd' });
			Session s = rep.login(cred);
			
			String result = "ALL NODE TYPES:<br/>";
			NodeTypeIterator it = s.getWorkspace().getNodeTypeManager().getAllNodeTypes();
			while (it.hasNext()) {
				NodeType nt = it.nextNodeType();
				result += nt.getName() + "<br/>";
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
