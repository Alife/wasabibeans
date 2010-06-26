/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.manager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;

import javax.ejb.Stateless;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.jackrabbit.commons.cnd.CndImporter;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;

@Stateless(name = "WasabiManager")
public class WasabiManager implements WasabiManagerLocal, WasabiManagerRemote {

	public final static String rootUserName = "root";
	public final static String rootUserPassword = "meerrettich";

	private static final String WASABI_NODETYPES_RESOURCE_PATH = "wasabi_nodetypes.cnd";

	@Override
	public void initDatabase() {

		/**
		 * Create user database and entries
		 */
		QueryRunner run = new QueryRunner(SqlConnector.connect());

		String dropWasabiUserTableQuery = "DROP TABLE IF EXISTS wasabi_user";
		String createWasabiUserTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_user ("
				+ "`username` varchar(255) NOT NULL," + "`password` varchar(64) NOT NULL," + "PRIMARY KEY (username));";
		try {
			run.update(dropWasabiUserTableQuery);
			run.update(createWasabiUserTableQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String insertWasabiRootUser = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

		try {
			run.update(insertWasabiRootUser, rootUserName, HashGenerator.generateHash(rootUserPassword,
					hashAlgorithms.SHA));
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void initRepository() {
		try {
			InitialContext ctx = new InitialContext();
			Repository rep = (Repository) ctx.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
			Credentials cred = new SimpleCredentials("user", new char[] { 'p', 'w', 'd' });
			Session s = rep.login(cred);
			// register wasabi nodetypes (also registers the wasabi jcr namespace)
			InputStream in = getClass().getClassLoader().getResourceAsStream(WASABI_NODETYPES_RESOURCE_PATH);
			Reader r = new InputStreamReader(in, "utf-8");
			CndImporter.registerNodeTypes(r, s);
			s.logout();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void initWorkspace(String workspacename) {
		try {
			InitialContext ctx = new InitialContext();
			Repository rep = (Repository) ctx.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
			Credentials cred = new SimpleCredentials("user", new char[] { 'p', 'w', 'd' });
			Session s = rep.login(cred, workspacename);

			// for testing: clear maybe existing wasabi content of workspace
			NodeIterator ni = s.getRootNode().getNodes();
			while (ni.hasNext()) {
				Node aNode = ni.nextNode();
				if (!aNode.getName().equals("jcr:system")) {
					aNode.remove();
				}
			}

			Node rootRoomNode = s.getRootNode().addNode(WasabiConstants.ROOT_ROOM_NAME, WasabiNodeType.WASABI_ROOM);
			rootRoomNode.addNode(WasabiNodeProperty.ROOMS + "/" + WasabiConstants.HOME_ROOM_NAME,
					WasabiNodeType.WASABI_ROOM);
			s.save();
			s.logout();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
