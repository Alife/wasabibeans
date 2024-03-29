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
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.jackrabbit.commons.cnd.CndImporter;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.event.EventAuthorizationCheckerLocal;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.ACLTimeEntryCleanerLocal;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class WasabiManager {

	private static WasabiLogger logger = WasabiLogger.getLogger(WasabiManager.class);

	public static void initDatabase() {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			/* Create user table and entries */
			String dropWasabiUserTableQuery = "DROP TABLE IF EXISTS wasabi_user";
			String createWasabiUserTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_user ("
					+ "`username` varchar(255) NOT NULL," + " `password` varchar(64) NOT NULL,"
					+ " `status` bit(1) NOT NULL DEFAULT 1," + " PRIMARY KEY (username)) ENGINE = InnoDB ;";

			run.update(dropWasabiUserTableQuery);
			run.update(createWasabiUserTableQuery);

			/* Create the internally used JMS_EVENT_ADMIN (administers the JMS queues used for event handling) */
			String insertWasabiEventAdmin = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

			run.update(insertWasabiEventAdmin, WasabiConstants.JMS_EVENT_ADMIN, HashGenerator.generateHash(
					WasabiConstants.JMS_EVENT_ADMIN_PASSWORD, WasabiConstants.hashAlgorithms.SHA));

			/* Create user rights table and entries */
			String dropWasabiRightsTable = "DROP TABLE IF EXISTS wasabi_rights";
			String createWasabiRightsTable = "CREATE TABLE `wasabi_rights` ("
					+ "`id` bigint(20) NOT NULL AUTO_INCREMENT,"
					+ "`object_id` varchar(64) NOT NULL,"
					+ "`user_id` varchar(64) NOT NULL,"
					+ "`parent_id` varchar(64) NOT NULL,"
					+ "`group_id` varchar(64) NOT NULL,"
					+ "`view` tinyint(2) NOT NULL,"
					+ "`read` tinyint(2) NOT NULL,"
					+ "`insert` tinyint(2) NOT NULL,"
					+ "`write` tinyint(2) NOT NULL,"
					+ "`comment` tinyint(2) NOT NULL,"
					+ "`execute` tinyint(2) NOT NULL,"
					+ "`grant` tinyint(2) NOT NULL,"
					+ "`start_time` bigint(20) NOT NULL DEFAULT '0',"
					+ "`end_time` bigint(20) NOT NULL DEFAULT '0',"
					+ "`inheritance_id` varchar(64) NOT NULL,"
					+ "`priority` tinyint(2) NOT NULL,"
					+ "`distance` smallint(5) NOT NULL,"
					+ "`wasabi_type` enum('ROOM', 'CONTAINER', 'DOCUMENT', 'LINK', 'ATTRIBUTE', 'USER', 'GROUP') NOT NULL,"
					+ "PRIMARY KEY (`id`), " + "KEY `object_id` (`object_id`), " + "KEY `parent_id` (`parent_id`)"
					+ ") ENGINE = InnoDB ;";

			run.update(dropWasabiRightsTable);
			run.update(createWasabiRightsTable);

			/* Create template rights table */
			String dropWasabiTemplateRightsTable = "DROP TABLE IF EXISTS wasabi_template_rights";
			String createWasabiTeplateRightsTable = "CREATE TABLE `wasabi_template_rights` ("
					+ "`id` int(11) NOT NULL AUTO_INCREMENT,"
					+ "`location_id` varchar(64) NOT NULL,"
					+ "`identity_id` varchar(64) NOT NULL,"
					+ "`wasabi_type` enum('ROOM' ,'CONTAINER' ,'DOCUMENT' , 'LINK', 'ATTRIBUTE', 'USER', 'GROUP', 'ALL') NOT NULL,"
					+ "`view` tinyint(2) NOT NULL," + "`read` tinyint(2) NOT NULL," + "`insert` tinyint(2) NOT NULL,"
					+ "`write` tinyint(2) NOT NULL," + "`comment` tinyint(2) NOT NULL,"
					+ "`execute` tinyint(2) NOT NULL," + "`grant` tinyint(2) NOT NULL,"
					+ "`start_time` float NOT NULL DEFAULT '0'," + "`end_time` float NOT NULL DEFAULT '0',"
					+ "PRIMARY KEY (`id`)" + ") ENGINE = InnoDB ;";

			run.update(dropWasabiTemplateRightsTable);
			run.update(createWasabiTeplateRightsTable);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			sqlConnector.close();
		}
	}

	/**
	 * Initializes schedule to cleanup ACL time entries.
	 * 
	 * @param earName
	 *            name of the ear-file which is used to deploy Wasabi
	 */
	public static void initACLTimeEntryCleaner(String earName) {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		try {
			String lookupPrefix = earName != null ? earName + "/" : "";
			ACLTimeEntryCleanerLocal eventAuthChecker = (ACLTimeEntryCleanerLocal) jndi.lookupLocal(lookupPrefix
					+ "ACLTimeEntryCleaner");
			eventAuthChecker.startACLTimeEntryCleaner();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			jndi.close();
		}
	}

	/**
	 * Initializes scheduled background tasks.
	 * 
	 * @param earName
	 *            name of the ear-file which is used to deploy Wasabi
	 */
	public static void initScheduledTasks(String earName) {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		try {
			String lookupPrefix = earName != null ? earName + "/" : "";
			EventAuthorizationCheckerLocal eventAuthChecker = (EventAuthorizationCheckerLocal) jndi
					.lookupLocal(lookupPrefix + "EventAuthorizationChecker");
			eventAuthChecker.startEventAuthorizationChecker();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			jndi.close();
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
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			Session s = jcr.getJCRSession();

			// register wasabi nodetypes (also registers the wasabi jcr namespaces) in case a path to a .cnd file has
			// been given
			if (jcrNodeTypesResourcePath != null) {
				InputStream in = WasabiManager.class.getClassLoader().getResourceAsStream(jcrNodeTypesResourcePath);
				Reader r = new InputStreamReader(in, "utf-8");
				CndImporter.registerNodeTypes(r, s);
			}

			if (resetContent) {
				// clear existing wasabi content
				logger.info("Resetting wasabi content: Deletion of existing content started.");
				NodeIterator ni = s.getRootNode().getNodes();
				while (ni.hasNext()) {
					Node aNode = ni.nextNode();
					if (!aNode.getName().equals("jcr:system")) {
						aNode.remove();
						// to automatically remove version histories is not a use-case the jackrabbit developers
						// support. the workaround implemented by removeNodeAndItsVersionHistoryRecursively() is not a
						// stable solution and causes exceptions from time to time. thus the version histories cannot be
						// deleted automatically. the jackrabbit version store (configured in the
						// JACKRABBIT_HOME/repository.xml; by default: JACKRABBIT_HOME/version) must be deleted manually
						// from to time if there is need to clear it (e.g. for testing).
						// removeNodeAndItsVersionHistoryRecursively(aNode, s.getWorkspace().getVersionManager(), s);
					}
				}
				logger.info("Resetting wasabi content: Deletion of existing content finished.");

				// create basic wasabi content
				Node workspaceRoot = s.getRootNode();
				// initial rooms
				Node wasabiRoot = workspaceRoot.addNode(WasabiConstants.ROOT_ROOM_NAME, WasabiNodeType.ROOM);
				ObjectServiceImpl.created(wasabiRoot, s, false, null, true);
				RoomServiceImpl.create(WasabiConstants.HOME_ROOM_NAME, wasabiRoot, s, false,
						WasabiConstants.ROOT_USER_NAME);
				// root node for wasabi groups and initial groups
				workspaceRoot.addNode(WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME, WasabiNodeType.OBJECT_COLLECTION);
				Node adminGroup = GroupServiceImpl.create(WasabiConstants.ADMINS_GROUP_NAME, null, s, false,
						WasabiConstants.ROOT_USER_NAME);
				Node pafUserGroup = GroupServiceImpl.create(WasabiConstants.PAF_GROUP_NAME, null, s, false,
						WasabiConstants.ROOT_USER_NAME);
				GroupServiceImpl.create(WasabiConstants.WASABI_GROUP_NAME, null, s, false,
						WasabiConstants.ROOT_USER_NAME);
				// root node for wasabi users and initial users
				workspaceRoot.addNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME, WasabiNodeType.OBJECT_COLLECTION);
				Node rootUser = UserServiceImpl.create(WasabiConstants.ROOT_USER_NAME,
						WasabiConstants.ROOT_USER_PASSWORD, s, false, WasabiConstants.ROOT_USER_NAME);
				Node adminUser = UserServiceImpl.create(WasabiConstants.ADMIN_USER_NAME,
						WasabiConstants.ADMIN_USER_PASSWORD, s, false, WasabiConstants.ROOT_USER_NAME);
				GroupServiceImpl.addMember(adminGroup, rootUser, s, false);
				GroupServiceImpl.addMember(adminGroup, adminUser, s, false);
				GroupServiceImpl.addMember(pafUserGroup, rootUser, s, false);
				GroupServiceImpl.addMember(pafUserGroup, adminUser, s, false);
				// root for wasabi pipelines
				workspaceRoot.addNode(WasabiConstants.JCR_ROOT_FOR_PIPELINES, WasabiNodeType.OBJECT_COLLECTION);
				logger.info("Resetting wasabi content: Initial wasabi content created.");
			}

			s.save();
			return s.getRootNode().getNode(WasabiConstants.ROOT_ROOM_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	/**
	 * Removes all nodes and their version histories that belong to the subtree of which the given {@code node} is the
	 * root.
	 * 
	 * @param node
	 * @param versionManager
	 * @param s
	 * @throws Exception
	 * @see https://issues.apache.org/jira/browse/JCR-134
	 */
	private static void removeNodeAndItsVersionHistoryRecursively(Node node, VersionManager versionManager, Session s)
			throws Exception {
		String nodePath = node.getPath();
		String nodeName = node.getName();
		boolean isVersionable = true;
		try {
			// checkout node if versionable -> without checkout child-nodes could not be removed
			versionManager.checkout(nodePath);
		} catch (UnsupportedRepositoryOperationException uroe) {
			isVersionable = false;
		}

		// deal with child-nodes first, otherwise not all version histories could be cleared
		for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
			removeNodeAndItsVersionHistoryRecursively(ni.nextNode(), versionManager, s);
		}

		if (!isVersionable) {
			// not versionable: just remove the node
			node.remove();
			s.save();
		} else {
			// versionable: first remove the node, then clear its version history (see
			// https://issues.apache.org/jira/browse/JCR-134)
			VersionHistory versionHistory = versionManager.getVersionHistory(nodePath);

			node.remove();
			s.save();

			// logger.info("Removing versions of node " + nodeName);
			for (String versionLabel : versionHistory.getVersionLabels()) {
				versionHistory.removeVersionLabel(versionLabel);
			}
			String rootVersionName = versionHistory.getRootVersion().getName();
			for (VersionIterator vi = versionHistory.getAllVersions(); vi.hasNext();) {
				String versionName = vi.nextVersion().getName();
				if (!versionName.equals(rootVersionName)) {
					versionHistory.removeVersion(versionName);
				}
			}
			// logger.info("All versions of node " + nodeName + " removed.");
		}
	}
}
