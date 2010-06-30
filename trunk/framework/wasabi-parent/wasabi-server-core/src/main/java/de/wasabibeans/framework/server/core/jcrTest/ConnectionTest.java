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
public class ConnectionTest extends TransferManager implements ConnectionTestRemote, ConnectionTestLocal {

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
			Credentials cred = new SimpleCredentials("user", new char[] { 'p', 'w', 'd' });
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
			Credentials cred = new SimpleCredentials("user", new char[] { 'p', 'w', 'd' });
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
