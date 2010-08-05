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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.jackrabbit.commons.cnd.CndImporter;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.SqlConnector;

public class WasabiManager {

	public static void initDatabase() {
		/* Create user table and entries */
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String dropWasabiUserTableQuery = "DROP TABLE IF EXISTS wasabi_user";
		String createWasabiUserTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_user ("
				+ "`username` varchar(255) NOT NULL," + "`password` varchar(64) NOT NULL,"
				+ "PRIMARY KEY (username)) ENGINE =  InnoDB ;";
		try {
			run.update(dropWasabiUserTableQuery);
			run.update(createWasabiUserTableQuery);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		String insertWasabiRootUser = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

		try {
			run.update(insertWasabiRootUser, WasabiConstants.ROOT_USER_NAME, HashGenerator.generateHash(
					WasabiConstants.ROOT_USER_PASSWORD, hashAlgorithms.SHA));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		/* Create user rights table and entries */
		String dropWasabiRightsTable = "DROP TABLE IF EXISTS wasabi_rights";
		String createWasabiRightsTable = "CREATE TABLE `wasabi_rights` (" + "`id` int(11) NOT NULL AUTO_INCREMENT,"
				+ "`object_id` varchar(64) NOT NULL," + "`user_id` varchar(64) NOT NULL,"
				+ "`parent_id` varchar(64) NOT NULL," + "`group_id` varchar(64) NOT NULL,"
				+ "`view` tinyint(2) NOT NULL," + "`read` tinyint(2) NOT NULL," + "`insert` tinyint(2) NOT NULL,"
				+ "`write` tinyint(2) NOT NULL," + "`comment` tinyint(2) NOT NULL," + "`execute` tinyint(2) NOT NULL,"
				+ "`grant` tinyint(2) NOT NULL," + "`start_time` float NOT NULL DEFAULT '0',"
				+ "`end_time` float NOT NULL DEFAULT '0'," + "`inheritance_id` varchar(64) NOT NULL,"
				+ "PRIMARY KEY (`id`,`object_id`,`user_id`,`group_id`,`start_time`,`end_time`,`inheritance_id`)"
				+ ") ENGINE = InnoDB ;";
		try {
			run.update(dropWasabiRightsTable);
			run.update(createWasabiRightsTable);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		/* Create template rights table */
		String dropWasabiTemplateRightsTable = "DROP TABLE IF EXISTS wasabi_template_rights";
		String createWasabiTeplateRightsTable = "CREATE TABLE `wasabi_template_rights` ("
				+ "`id` int(11) NOT NULL AUTO_INCREMENT,"
				+ "`location_id` varchar(64) NOT NULL,"
				+ "`wasabi_type` enum('ROOM' ,'CONTAINER' ,'DOCUMENT' , 'LINK', 'ATTRIBUTE', 'USER', 'GROUP') NOT NULL,"
				+ "`view` tinyint(2) NOT NULL," + "`read` tinyint(2) NOT NULL," + "`insert` tinyint(2) NOT NULL,"
				+ "`write` tinyint(2) NOT NULL," + "`comment` tinyint(2) NOT NULL," + "`execute` tinyint(2) NOT NULL,"
				+ "`grant` tinyint(2) NOT NULL," + "`start_time` float NOT NULL DEFAULT '0',"
				+ "`end_time` float NOT NULL DEFAULT '0',"
				+ "PRIMARY KEY (`id`, `location_id`, `wasabi_type`, `start_time`,`end_time`)" + ") ENGINE = InnoDB ;";

		try {
			run.update(dropWasabiTemplateRightsTable);
			run.update(createWasabiTeplateRightsTable);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Initializes the JCR repository. Dependent on the given parameters, the wasabi node types are imported and the
	 * wasabi content of the repository is reset.
	 * 
	 * @param jcrNodeTypesResourcePath
	 *            Resource path to the .cnd file of the wasabi node types. Set to null, if the wasabi node types are
	 *            already imported.
	 * @param resetContent
	 *            Set to true, if the wasabi content of the repository should be reset
	 * @return the JCR node representing the wasabi root room
	 */
	public static Node initRepository(String jcrNodeTypesResourcePath, boolean resetContent) {
		Session baseSession = null;
		try {
			JcrConnector jcr = JcrConnector.getJCRConnector();
			baseSession = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
			
			// register wasabi nodetypes (also registers the wasabi jcr namespace) in case a path to a .cnd file has
			// been given
			if (jcrNodeTypesResourcePath != null) {
				InputStream in = WasabiManager.class.getClassLoader().getResourceAsStream(
						WasabiConstants.JCR_NODETYPES_RESOURCE_PATH);
				Reader r = new InputStreamReader(in, "utf-8");
				CndImporter.registerNodeTypes(r, baseSession);
			}

			if (resetContent) {
				// clear existing wasabi content
				NodeIterator ni = baseSession.getRootNode().getNodes();
				while (ni.hasNext()) {
					Node aNode = ni.nextNode();
					if (!aNode.getName().equals("jcr:system")) {
						aNode.remove();
					}
				}

				// create basic wasabi content
				Node workspaceRoot = baseSession.getRootNode();
				// wasabi root room + wasabi home room
				Node wasabiRoot = workspaceRoot.addNode(WasabiConstants.ROOT_ROOM_NAME, WasabiNodeType.WASABI_ROOM);
				Node wasabiHome = createRoom(WasabiConstants.HOME_ROOM_NAME, wasabiRoot);
				// root node for wasabi users and initial users
				Node wasabiUsers = workspaceRoot.addNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME,
						WasabiNodeType.WASABI_USERS);
				createUser(WasabiConstants.ROOT_USER_NAME, wasabiUsers, wasabiHome);
			}

			baseSession.save();
			return baseSession.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			baseSession.logout();
		}
	}

	private static Node createRoom(String name, Node environment) throws RepositoryException {
		return environment.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.WASABI_ROOM);
	}

	private static Node createUser(String name, Node rootOfUsersNode, Node wasabiHome) throws RepositoryException {
		Node userNode = rootOfUsersNode.addNode(name, WasabiNodeType.WASABI_USER);
		userNode.setProperty(WasabiNodeProperty.DISPLAY_NAME, name);
		Node homeRoomNode = wasabiHome.addNode(WasabiNodeProperty.ROOMS + "/" + name, WasabiNodeType.WASABI_ROOM);
		userNode.setProperty(WasabiNodeProperty.HOME_ROOM, homeRoomNode);
		userNode.setProperty(WasabiNodeProperty.START_ROOM, homeRoomNode);
		return userNode;
	}
}
