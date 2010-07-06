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

package de.wasabibeans.framework.server.core.manager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.jackrabbit.commons.cnd.CndImporter;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.SqlConnector;

@Stateless(name = "WasabiManager")
public class WasabiManager implements WasabiManagerLocal, WasabiManagerRemote {

	public final static String rootUserName = "root";
	public final static String rootUserPassword = "meerrettich";
	
	private JndiConnector jndi;
	private JcrConnector jcr;
	
	public WasabiManager() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector();
	}

	@Override
	public void initDatabase() {

		/**
		 * Create user table and entries
		 */
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

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

		/**
		 * Create user rights table and entries
		 */
		String dropWasabiRightsTable = "DROP TABLE IF EXISTS wasabi_rights";
		String createWasabiRightsTable = "CREATE TABLE `wasabi_rights` (" + " `object_uuid` varchar(64) NOT NULL,"
				+ "`user_id` varchar(64) NOT NULL," + "`p_id` varchar(64) NOT NULL," + "`g_id` varchar(64) NOT NULL,"
				+ "`view` tinyint(2) NOT NULL," + "`read` tinyint(2) NOT NULL," + "`insert` tinyint(2) NOT NULL,"
				+ "`write` tinyint(2) NOT NULL," + "`comment` tinyint(2) NOT NULL," + "`execute` tinyint(2) NOT NULL,"
				+ "`grant` tinyint(2) NOT NULL," + "`start_time` float NOT NULL," + "`end_time` float NOT NULL,"
				+ " KEY `user_id` (`user_id`));";
		try {
			run.update(dropWasabiRightsTable);
			run.update(createWasabiRightsTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void initRepository() {
		try {
			Session s = jcr.getJCRSession();
			// register wasabi nodetypes (also registers the wasabi jcr namespace)
			InputStream in = getClass().getClassLoader().getResourceAsStream(WasabiConstants.JCR_NODETYPES_RESOURCE_PATH);
			Reader r = new InputStreamReader(in, "utf-8");
			CndImporter.registerNodeTypes(r, s);
			s.logout();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initializes the JCR workspace with the given name.
	 * 
	 * @param workspacename
	 *            name of the JCR workspace
	 * @return DTO of the wasabi root room
	 */
	public WasabiRoomDTO initWorkspace(String workspacename) {
		try {
			// init store for user 2 jcr session mapping
			ConcurrentHashMap<String, Session> user2JCRSession = new ConcurrentHashMap<String, Session>();
			jndi.unbind(WasabiConstants.JNDI_JCR_USER2SESSION);
			jndi.bind(WasabiConstants.JNDI_JCR_USER2SESSION, user2JCRSession);

			Session s = jcr.getJCRSession();

			// for testing: clear maybe existing wasabi content of workspace
			NodeIterator ni = s.getRootNode().getNodes();
			while (ni.hasNext()) {
				Node aNode = ni.nextNode();
				if (!aNode.getName().equals("jcr:system")) {
					aNode.remove();
				}
			}

			// create basic wasabi content
			Node rootRoomNode = s.getRootNode().addNode(WasabiConstants.ROOT_ROOM_NAME, WasabiNodeType.WASABI_ROOM);
			WasabiRoomDTO rootRoomDTO = new WasabiRoomDTO();
			rootRoomDTO.setId(rootRoomNode.getIdentifier());
			rootRoomNode.addNode(WasabiNodeProperty.ROOMS + "/" + WasabiConstants.HOME_ROOM_NAME,
					WasabiNodeType.WASABI_ROOM);
			s.save();
			s.logout();
			return rootRoomDTO;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
