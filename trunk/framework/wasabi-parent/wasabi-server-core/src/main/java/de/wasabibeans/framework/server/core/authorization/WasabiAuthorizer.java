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

package de.wasabibeans.framework.server.core.authorization;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiACLPriority;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

public class WasabiAuthorizer {

	public static boolean isAdminUser(String callerPrincipal, Session s) throws UnexpectedInternalProblemException {
		if (callerPrincipal.equals("root") || callerPrincipal.equals("admin"))
			return true;
		
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		if (GroupServiceImpl.isMember(GroupServiceImpl.getGroupByName(WasabiConstants.ADMINS_GROUP_NAME, s), userNode))
			return true;
		return false;
	}

	public static boolean authorize(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = userNode.getIdentifier();

			return checkRights(objectUUID, userUUID, userNode, permission, s);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean authorize(Node objectNode, String callerPrincipal, int[] permission, Session s)
			throws UnexpectedInternalProblemException {
		for (int i = 0; i < permission.length; i++) {
			if (authorize(objectNode, callerPrincipal, permission[i], s))
				return true;
		}
		return false;
	}

	private static int authorizeChild(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		int sum = 0;
		if (authorize(objectNode, callerPrincipal, permission, s)) {
			Vector<Node> childreen = ACLServiceImpl.getChildren(objectNode);

			for (Node node : childreen) {
				sum = sum + authorizeChild(node, callerPrincipal, permission, s);
				if (sum > 0)
					return sum;
			}
		} else
			return 1;
		return sum;
	}

	public static boolean authorizeChildreen(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		if (authorizeChild(objectNode, callerPrincipal, permission, s) > 0)
			return false;
		else
			return true;
	}

	public static Vector<String> authorizePermission(Node objectNode, String callerPrincipal, int permission,
			WasabiType wasabiType, Session s) throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = userNode.getIdentifier();

			return permissionFilter(objectUUID, userUUID, userNode, wasabiType, permission, s);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static boolean checkRights(String objectUUID, String userUUID, Node userNode, int permission, Session s)
			throws UnexpectedInternalProblemException {
		try {
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			Vector<String> allGroups = getGroupMemberships(userNode, s);

			String allGroupsQuery = getGroupMembershipQuery(allGroups);
			String rightQueryAllow = getRightQueryAllow(permission);
			String rightQueryDeny = getRightQueryDeny(permission);
			String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
			String userCheck = "`user_id`='" + userUUID + "' ";

			long time = java.lang.System.currentTimeMillis();

			String getACLEntries = "SELECT `object_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority` "
					+ "FROM `wasabi_rights` WHERE " + "(`object_id`='"
					+ objectUUID
					+ "' AND `priority`="
					+ WasabiACLPriority.INHERITED_GROUP_RIGHT
					+ " AND "
					+ identityCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6 OR `priority`=7))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_GROUP_RIGHT
					+ " AND "
					+ identityCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.INHERITED_USER_RIGHT
					+ " AND "
					+ userCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_USER_RIGHT
					+ " AND "
					+ userCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT
					+ " AND "
					+ identityCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT
					+ " AND "
					+ identityCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.INHERITED_USER_TIME_RIGHT
					+ " AND "
					+ userCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1))) "
					+ "OR "
					+ "(`object_id`='"
					+ objectUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT
					+ " AND "
					+ userCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `object_id`='"
					+ objectUUID
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) " + "AND `priority`=0)) ";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h);

			if (result.size() > 0)
				return true;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return false;
	}

	private static String getGroupMembershipQuery(Vector<String> allGroups) {
		String groupMembershipQuery = "(";

		for (String group : allGroups) {
			groupMembershipQuery = groupMembershipQuery + "`group_id`='" + group + "' OR ";
		}

		groupMembershipQuery = groupMembershipQuery + "`group_id`='placeholderValueGroup42') ";

		return groupMembershipQuery;
	}

	private static Vector<String> getGroupMemberships(Node userNode, Session s)
			throws UnexpectedInternalProblemException {
		Vector<String> groups = new Vector<String>();
		Vector<String> allGroups = new Vector<String>();

		try {
			NodeIterator ni = UserServiceImpl.getMemberships(userNode);
			while (ni.hasNext()) {
				Node groupRef = ni.nextNode();
				Node group = null;
				try {
					group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
				} catch (ItemNotFoundException infe) {
					groupRef.remove();
				}
				if (group != null) {
					groups.add(group.getIdentifier());
				}

				allGroups.addAll(groups);

				for (String wasabiGroup : groups) {
					Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiGroup));
					while (groupNode != null && !allGroups.contains(groupNode)) {
						String groupUUID = groupNode.getIdentifier();
						allGroups.add(groupUUID);
						groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
					}
				}
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
		return allGroups;
	}

	private static String getRightQueryAllow(int permission) {
		String rightQuery = "`view`=1 ";

		switch (permission) {
		case WasabiPermission.VIEW:
			rightQuery = "`view`=1 ";
			return rightQuery;
		case WasabiPermission.READ:
			rightQuery = "`read`=1 ";
			return rightQuery;
		case WasabiPermission.EXECUTE:
			rightQuery = "`execute`=1 ";
			return rightQuery;
		case WasabiPermission.COMMENT:
			rightQuery = "`comment`=1 ";
			return rightQuery;
		case WasabiPermission.INSERT:
			rightQuery = "`insert`=1 ";
			return rightQuery;
		case WasabiPermission.WRITE:
			rightQuery = "`write`=1 ";
			return rightQuery;
		case WasabiPermission.GRANT:
			rightQuery = "`grant`=1 ";
			return rightQuery;
		}
		return rightQuery;
	}

	private static String getRightQueryDeny(int permission) {
		String rightQuery = "`view`=-1 ";

		switch (permission) {
		case WasabiPermission.VIEW:
			rightQuery = "`view`=-1 ";
			return rightQuery;
		case WasabiPermission.READ:
			rightQuery = "`read`=-1 ";
			return rightQuery;
		case WasabiPermission.EXECUTE:
			rightQuery = "`execute`=-1 ";
			return rightQuery;
		case WasabiPermission.COMMENT:
			rightQuery = "`comment`=-1 ";
			return rightQuery;
		case WasabiPermission.INSERT:
			rightQuery = "`insert`=-1 ";
			return rightQuery;
		case WasabiPermission.WRITE:
			rightQuery = "`write`=-1 ";
			return rightQuery;
		case WasabiPermission.GRANT:
			rightQuery = "`grant`=-1 ";
			return rightQuery;
		}
		return rightQuery;
	}

	private static Vector<String> permissionFilter(String parentUUID, String userUUID, Node userNode,
			WasabiType wasabiType, int permission, Session s) throws UnexpectedInternalProblemException {
		try {
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			Vector<String> results = new Vector<String>();
			Vector<String> allGroups = getGroupMemberships(userNode, s);

			String allGroupsQuery = getGroupMembershipQuery(allGroups);
			String rightQueryAllow = getRightQueryAllow(permission);
			String rightQueryDeny = getRightQueryDeny(permission);
			String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
			String userCheck = "`user_id`='" + userUUID + "' ";

			long time = java.lang.System.currentTimeMillis();

			String getACLEntries = "SELECT DISTINCT `object_id` " + "FROM `wasabi_rights` WHERE " + "(`parent_id`='"
					+ parentUUID
					+ "' AND `priority`="
					+ +WasabiACLPriority.INHERITED_GROUP_RIGHT
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ " AND "
					+ allGroupsQuery
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `parent_id`='"
					+ parentUUID
					+ "' "
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6 OR `priority`=7))) "
					+ "OR "
					+ "(`parent_id`='"
					+ parentUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_GROUP_RIGHT
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ " AND "
					+ allGroupsQuery
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `parent_id`='"
					+ parentUUID
					+ "' "
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6))) "
					+ "OR "
					+ "(`parent_id`='"
					+ parentUUID
					+ "' "
					+ " AND `priority`="
					+ WasabiACLPriority.INHERITED_USER_RIGHT
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ " AND "
					+ userCheck
					+ " AND "
					+ rightQueryAllow
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
					+ rightQueryDeny
					+ "AND `parent_id`='"
					+ parentUUID
					+ "' "
					+ " AND `wasabi_type`='"
					+ wasabiType.toString()
					+ "' "
					+ "AND "
					+ identityCheck
					+ "AND ((`start_time`<="
					+ time
					+ " AND `end_time`>="
					+ time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5))) "
					+ "OR " + "(`parent_id`='" + parentUUID + "' " + " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_USER_RIGHT + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ " AND " + userCheck + " AND " + rightQueryAllow + "AND ((`start_time`<=" + time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE " + rightQueryDeny
					+ "AND `parent_id`='" + parentUUID + "' " + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ "AND " + identityCheck + "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4))) " + "OR "
					+ "(`parent_id`='" + parentUUID + "' " + " AND `priority`="
					+ WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT + " AND `wasabi_type`='" + wasabiType.toString()
					+ "' " + " AND " + allGroupsQuery + " AND " + rightQueryAllow + "AND ((`start_time`<=" + time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE " + rightQueryDeny
					+ "AND `parent_id`='" + parentUUID + "' " + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ "AND " + identityCheck + "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3))) " + "OR "
					+ "(`parent_id`='" + parentUUID + "' " + " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT + " AND `wasabi_type`='" + wasabiType.toString()
					+ "' " + " AND " + allGroupsQuery + " AND " + rightQueryAllow + "AND ((`start_time`<=" + time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE " + rightQueryDeny
					+ "AND `parent_id`='" + parentUUID + "' " + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ "AND " + identityCheck + "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND (`priority`=0 OR `priority`=1 OR `priority`=2))) " + "OR " + "(`parent_id`='" + parentUUID
					+ "' " + " AND `priority`=" + WasabiACLPriority.INHERITED_USER_TIME_RIGHT + " AND `wasabi_type`='"
					+ wasabiType.toString() + "' " + " AND " + userCheck + " AND " + rightQueryAllow
					+ "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE " + rightQueryDeny
					+ "AND `parent_id`='" + parentUUID + "' " + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ "AND " + identityCheck + "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) " + "AND (`priority`=0 OR `priority`=1))) " + "OR "
					+ "(`parent_id`='" + parentUUID + "' " + " AND `priority`="
					+ WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT + " AND `wasabi_type`='" + wasabiType.toString()
					+ "' " + " AND " + userCheck + " AND " + rightQueryAllow + "AND ((`start_time`<=" + time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE " + rightQueryDeny
					+ "AND `parent_id`='" + parentUUID + "' " + " AND `wasabi_type`='" + wasabiType.toString() + "' "
					+ "AND " + identityCheck + "AND ((`start_time`<=" + time + " AND `end_time`>=" + time
					+ ") OR (`start_time`=0 AND `end_time`=0)) " + "AND `priority`=0)) ";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h);

			for (WasabiACLEntry wasabiACLEntry : result)
				results.add(wasabiACLEntry.getObject_Id());

			return results;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

}
