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
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

public class WasabiAuthorizer {

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

	// public static Vector<String> authorizeVIEW(Node objectNode, String callerPrincipal, Session s)
	// throws UnexpectedInternalProblemException {
	// try {
	// String objectUUID = ObjectServiceImpl.getUUID(objectNode);
	// Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
	// String userUUID = userNode.getIdentifier();
	// Vector<String> UserFilter = UserViewFilter(objectUUID, userUUID);
	// Vector<String> GroupFilter = GroupViewFilter(objectUUID, userUUID, userNode, s);
	// for (String string : GroupFilter) {
	// if (!UserFilter.contains(string))
	// UserFilter.add(string);
	// }
	// return UserFilter;
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// }

	private static String getGroupMembershipQuery(Vector<String> allGroups) {
		String groupMembershipQuery = "(";

		for (String group : allGroups) {
			groupMembershipQuery = groupMembershipQuery + "`group_id`='" + group + "' OR ";
		}

		groupMembershipQuery = groupMembershipQuery + "`group_id`='placeholderValueGroup42') ";

		return groupMembershipQuery;
	}

	// private static boolean checkParentGroup(String objectUUID, String groupUUID, int permission, Session s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// boolean groupTimeRightAllow = false;
	//
	// try {
	// try {
	// String getACLEntries =
	// "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority`"
	// + " FROM `wasabi_rights` "
	// + "WHERE `object_id`=? AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + " AND `group_id`=?" + " AND " + getRightQuery(permission) + " ORDER BY `priority`";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time, groupUUID);
	//
	// int counter = 0;
	// int size = result.size();
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int right = getRight(wasabiACLEntry, permission);
	// int prio = wasabiACLEntry.getPriority();
	// counter++;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == -1)
	// return false;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == 1)
	// groupTimeRightAllow = true;
	//
	// if ((counter == size || prio != WasabiACLPriority.GROUP_TIME_RIGHT) && groupTimeRightAllow) {
	// if (groupNode == null)
	// return true;
	// else if (!checkParentGroupForTimeRightsDeny(objectUUID, groupNode.getIdentifier(), permission,
	// s))
	// return false;
	// }
	//
	// if (prio == WasabiACLPriority.GROUP_RIGHT && right == -1) {
	// if (groupNode == null)
	// return false;
	// else if (checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s))
	// return true;
	// }
	// }
	//
	// if (groupNode == null)
	// return true;
	// else if (checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s))
	// return true;
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return groupTimeRightAllow;
	// }

	// private static boolean checkParentGroupForAllowance(String objectUUID, String groupUUID, int permission, Session
	// s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// boolean groupTimeRightAllow = false;
	//
	// try {
	// try {
	// String getACLEntries =
	// "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority`"
	// + " FROM `wasabi_rights` "
	// + "WHERE `object_id`=? AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + " AND `group_id`=?" + " AND " + getRightQuery(permission) + " ORDER BY `priority`";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time, groupUUID);
	//
	// int counter = 0;
	// int size = result.size();
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int right = getRight(wasabiACLEntry, permission);
	// int prio = wasabiACLEntry.getPriority();
	// counter++;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == -1)
	// return false;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == 1)
	// groupTimeRightAllow = true;
	//
	// if ((counter == size || prio != WasabiACLPriority.GROUP_TIME_RIGHT) && groupTimeRightAllow) {
	// if (groupNode == null)
	// return true;
	// else if (checkParentGroupForTimeRightsDeny(objectUUID, groupNode.getIdentifier(), permission, s))
	// return true;
	// }
	//
	// if (prio == WasabiACLPriority.GROUP_RIGHT && right == -1) {
	// if (groupNode == null)
	// return false;
	// else if (checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s))
	// return true;
	// }
	// }
	//
	// if (groupNode == null)
	// return true;
	// else if (checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s))
	// return true;
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return groupTimeRightAllow;
	// }

	// private static boolean checkParentGroupForTimeRights(String objectUUID, String groupUUID, int permission, Session
	// s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// boolean ret = false;
	//
	// try {
	// try {
	// // TODO: Kann eventuell noch optimiert werden durch NOT IN() in Verbindung mit DESTINCT oder GROUP BY
	// String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` FROM"
	// + " FROM `wasabi_rights` "
	// + "WHERE `object_id`=? AND `start_time`<=? AND `end_time`>=? "
	// + " AND `group_id`=?" + " AND `priority`=?" + " AND " + getRightQuery(permission);
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, groupUUID, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int right = getRight(wasabiACLEntry, permission);
	//
	// if (right == -1)
	// return false;
	// if (right == 1)
	// ret = true;
	// }
	//
	// if (ret) {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// return checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s);
	// }
	//
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return ret;
	// }

	// private static boolean checkParentGroupForTimeRightsDeny(String objectUUID, String groupUUID, int permission,
	// Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// try {
	// String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
	// + "FROM `wasabi_rights` "
	// + "WHERE `object_id`=? "
	// + "AND `start_time`<=? "
	// + "AND `end_time`>=? "
	// + "AND `group_id`=? "
	// + "AND `priority`=? "
	// + "AND "
	// + getRightQueryDeny(permission);
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time,
	// WasabiACLPriority.GROUP_TIME_RIGHT);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int right = getRight(wasabiACLEntry, permission);
	//
	// if (right == -1)
	// return false;
	// }
	//
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// return checkParentGroupForTimeRightsDeny(objectUUID, groupUUID, permission, s);
	//
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }

	// private static boolean checkParentGroupForViewFilter(String objectUUID, String groupUUID, Session s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// boolean ret = false;
	//
	// try {
	// try {
	// String getACLEntries = "SELECT DISTINCT `priority`, `view` FROM `wasabi_rights` "
	// + "WHERE `group_id`=? AND `object_id`=? "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) AND "
	// + "(`view`=-1 OR (`view`=1 AND `priority`=5)) ORDER BY `priority`";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, groupUUID, objectUUID, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int priority = wasabiACLEntry.getPriority();
	// int view = wasabiACLEntry.getView();
	// if (priority == WasabiACLPriority.GROUP_TIME_RIGHT && view == -1)
	// return false;
	// else if (priority == WasabiACLPriority.GROUP_TIME_RIGHT && view == 1)
	// ret = true;
	// else if (priority == WasabiACLPriority.GROUP_RIGHT && ret == false && view == -1)
	// return false;
	// else if (priority == WasabiACLPriority.GROUP_RIGHT && ret == true && view == -1) {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// checkParentGroupForViewFilterTR(objectUUID, groupUUID, s);
	// } else {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// checkParentGroupForViewFilter(objectUUID, groupUUID, s);
	// }
	// }
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return ret;
	// }

	// private static boolean checkParentGroupForViewFilterTR(String objectUUID, String groupUUID, Session s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// try {
	// String getACLEntries = "SELECT DISTINCT `priority` FROM `wasabi_rights` "
	// + "WHERE `group_id`=? AND `object_id`=? " + "((`start_time`<=? AND `end_time`>=?) AND "
	// + "`view`=-1";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, groupUUID, objectUUID, time, time);
	//
	// if (result.isEmpty()) {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// checkParentGroupForViewFilterTR(objectUUID, groupUUID, s);
	// } else
	// return false;
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static int checkUserPriority(List<WasabiACLEntry> ACLEntries, int permission) {
	// int result = 0;
	//
	// for (WasabiACLEntry wasabiACLEntry : ACLEntries) {
	// switch (permission) {
	// case WasabiPermission.VIEW:
	// int view = wasabiACLEntry.getView();
	// if (view == -1)
	// return -1;
	// else if (view == 1)
	// result = 1;
	// break;
	// case WasabiPermission.READ:
	// int read = wasabiACLEntry.getRead();
	// if (read == -1)
	// return -1;
	// else if (read == 1)
	// result = 1;
	// break;
	// case WasabiPermission.EXECUTE:
	// int execute = wasabiACLEntry.getExecute();
	// if (execute == -1)
	// return -1;
	// else if (execute == 1)
	// result = 1;
	// break;
	// case WasabiPermission.COMMENT:
	// int comment = wasabiACLEntry.getComment();
	// if (comment == -1)
	// return -1;
	// else if (comment == 1)
	// result = 1;
	// break;
	// case WasabiPermission.INSERT:
	// int insert = wasabiACLEntry.getInsert();
	// if (insert == -1)
	// return -1;
	// else if (insert == 1)
	// result = 1;
	// break;
	// case WasabiPermission.WRITE:
	// int write = wasabiACLEntry.getWrite();
	// if (write == -1)
	// return -1;
	// else if (write == 1)
	// result = 1;
	// break;
	// case WasabiPermission.GRANT:
	// int grant = wasabiACLEntry.getGrant();
	// if (grant == -1)
	// return -1;
	// else if (grant == 1)
	// result = 1;
	// break;
	// }
	//
	// }
	// return result;
	// }

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

	private static String getPriorityQuery(int[] priority) {
		String priorityQuery = "(";

		for (int i = 0; i < priority.length; i++) {
			priorityQuery = priorityQuery + "`priority`=" + priority[i] + " OR ";
		}

		priorityQuery = priorityQuery + "`priority`=42) ";

		return priorityQuery;
	}

	private static int getRight(WasabiACLEntry wasabiACLEntry, int permission) {
		switch (permission) {
		case WasabiPermission.VIEW:
			return wasabiACLEntry.getView();
		case WasabiPermission.READ:
			return wasabiACLEntry.getRead();
		case WasabiPermission.EXECUTE:
			return wasabiACLEntry.getExecute();
		case WasabiPermission.COMMENT:
			return wasabiACLEntry.getComment();
		case WasabiPermission.INSERT:
			return wasabiACLEntry.getInsert();
		case WasabiPermission.WRITE:
			return wasabiACLEntry.getWrite();
		case WasabiPermission.GRANT:
			return wasabiACLEntry.getGrant();
		}
		return 0;
	}

	private static String getRightQuery(int permission) {
		String rightQuery = "(`view`=1 OR `view`=-1) ";

		switch (permission) {
		case WasabiPermission.VIEW:
			rightQuery = "(`view`=1 OR `view`=-1) ";
			return rightQuery;
		case WasabiPermission.READ:
			rightQuery = "(`read`=1 OR `read`=-1) ";
			return rightQuery;
		case WasabiPermission.EXECUTE:
			rightQuery = "(`execute`=1 OR `execute`=-1) ";
			return rightQuery;
		case WasabiPermission.COMMENT:
			rightQuery = "(`comment`=1 OR `comment`=-1) ";
			return rightQuery;
		case WasabiPermission.INSERT:
			rightQuery = "(`insert`=1 OR `insert`=-1) ";
			return rightQuery;
		case WasabiPermission.WRITE:
			rightQuery = "(`write`=1 OR `write`=-1) ";
			return rightQuery;
		case WasabiPermission.GRANT:
			rightQuery = "(`grant`=1 OR `grant`=-1) ";
			return rightQuery;
		}
		return rightQuery;
	}

	// private static Vector<String> GroupViewFilter(String parentUUID, String userUUID, Node userNode, Session s)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> results = new Vector<String>();
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	//
	// String getACLEntries = "SELECT DISTINCT `object_id` FROM `wasabi_rights` WHERE "
	// + "(`parent_id`=? AND `view`=1 AND "
	// + allGroupsQuery
	// + "AND `priority`=7 "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=7 OR `priority`=6 OR `priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND `view`=1 AND "
	// + allGroupsQuery
	// + "AND `priority`=6 AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=6 OR `priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND `view`=1 AND "
	// + allGroupsQuery
	// + "AND `priority`=5 "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND `view`=1 AND "
	// + allGroupsQuery
	// + "AND `priority`=4 AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `priority`=4))";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID, time, time, parentUUID, time, time,
	// time, time, parentUUID, time, time, time, time, parentUUID, time, time, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// String objectUUID = wasabiACLEntry.getObject_Id();
	// if (priorityGroupCheck(objectUUID, userUUID, userNode, WasabiPermission.VIEW, s) == 1)
	// ;
	// results.add(objectUUID);
	// }
	//
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return results;
	// }

	// private static Vector<String> GroupPermissionFilter(String parentUUID, String userUUID, Node userNode,
	// int permission, Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> results = new Vector<String>();
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	//
	// String getACLEntries = "SELECT DISTINCT `object_id` FROM `wasabi_rights` WHERE " + "(`parent_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND `priority`=7 "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=7 OR `priority`=6 OR `priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND `priority`=6 AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=6 OR `priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND `priority`=5 "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=5 OR `priority`=4))) "
	// + "OR "
	// + "(`parent_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND "
	// + allGroupsQuery
	// + "AND `priority`=4 AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` " + "WHERE "
	// + getRightQueryDeny(permission) + "AND " + allGroupsQuery
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `priority`=4))";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID, time, time, parentUUID, time, time,
	// time, time, parentUUID, time, time, time, time, parentUUID, time, time, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// String objectUUID = wasabiACLEntry.getObject_Id();
	// results.add(objectUUID);
	// }
	//
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return results;
	// }

	// private static boolean priorityCheck1(String objectUUID, String userUUID, int permission)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// // explicit user time rights
	// String getExplicitUserTimeRights = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `user_id`=? AND `start_time`<=? AND `end_time`>=? AND `inheritance_id`=?";
	// try {
	// ResultSetHandler<List<WasabiACLEntry>> h1 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result1 = run.query(getExplicitUserTimeRights, h1, objectUUID, userUUID, time, time,
	// "");
	//
	// // Allowance check
	// if (checkUserPriority(result1, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(result1, permission) == 1) {
	// return true;
	// } else {
	// // inherited user time rights
	// String getInheritedUserTimeRights = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `user_id`=? AND `start_time`<=? AND `end_time`>=? AND `inheritance_id`!=?";
	// try {
	// ResultSetHandler<List<WasabiACLEntry>> h2 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result2 = run.query(getInheritedUserTimeRights, h2, objectUUID, userUUID,
	// time, time, "");
	//
	// // Allowance check
	// if (checkUserPriority(result2, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(result2, permission) == 1) {
	// return true;
	// } else {
	// // explicit user rights
	// String getExplicitUserRights = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// try {
	// ResultSetHandler<List<WasabiACLEntry>> h3 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result3 = run.query(getExplicitUserRights, h3, objectUUID, userUUID,
	// 0, 0, "");
	//
	// // Allowance check
	// if (checkUserPriority(result3, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(result3, permission) == 1) {
	// return true;
	// } else {
	// String getInheritedUserRights = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`!=?";
	//
	// try {
	// ResultSetHandler<List<WasabiACLEntry>> h4 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result4 = run.query(getInheritedUserRights, h4, objectUUID,
	// userUUID, 0, 0, "");
	//
	// // Allowance check
	// if (checkUserPriority(result4, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(result4, permission) == 1) {
	// return true;
	// } else {
	// // TODO: Group check mechanism
	// return false;
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }

	// private static boolean priorityCheck2(String objectUUID, String userUUID, int permission)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// String getACLEntries = "SELECT * FROM wasabi_rights WHERE `object_id`=? AND `user_id`=?";
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, userUUID);
	//
	// // Filter and check explicit user time rights
	// List<WasabiACLEntry> explicitUserTimeRights = new Vector<WasabiACLEntry>();
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// if (wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() <= time
	// && wasabiACLEntry.getEnd_Time() >= time) {
	// explicitUserTimeRights.add(wasabiACLEntry);
	// }
	// }
	//
	// // Allowance check
	// if (checkUserPriority(explicitUserTimeRights, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(explicitUserTimeRights, permission) == 1) {
	// return true;
	// } else {
	// // Filter inherited user time rights
	// List<WasabiACLEntry> inheritedUserTimeRights = new Vector<WasabiACLEntry>();
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// if (!wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() <= time
	// && wasabiACLEntry.getEnd_Time() >= time) {
	// inheritedUserTimeRights.add(wasabiACLEntry);
	// }
	// }
	//
	// // Allowance check
	// if (checkUserPriority(inheritedUserTimeRights, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(inheritedUserTimeRights, permission) == 1) {
	// return true;
	// } else {
	// // Filter explicit user rights
	// List<WasabiACLEntry> explicitUserRights = new Vector<WasabiACLEntry>();
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// if (wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() == 0
	// && wasabiACLEntry.getEnd_Time() == 0) {
	// explicitUserRights.add(wasabiACLEntry);
	// }
	// }
	//
	// // Allowance check
	// if (checkUserPriority(explicitUserRights, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(explicitUserRights, permission) == 1) {
	// return true;
	// } else {
	// // Filter inherited user rights
	// List<WasabiACLEntry> inheritedUserRights = new Vector<WasabiACLEntry>();
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// if (!wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() == 0
	// && wasabiACLEntry.getEnd_Time() == 0) {
	// inheritedUserRights.add(wasabiACLEntry);
	// }
	// }
	//
	// if (checkUserPriority(inheritedUserRights, permission) == -1) {
	// return false;
	// } else if (checkUserPriority(inheritedUserRights, permission) == 1) {
	// return true;
	// } else {
	// return false;
	// // TODO: Group check mechanism
	// }
	// }
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }

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

	private static boolean parentGroupAllowanceCheck(String objectUUID, Vector<String> allGroups, int[] priority,
			int permission) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();

		try {
			String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
					+ "FROM `wasabi_rights` "
					+ "WHERE `object_id`=? "
					+ "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`<=0 AND `end_time`>=0)) "
					+ "AND "
					+ getGroupMembershipQuery(allGroups)
					+ "AND "
					+ getRightQueryAllow(permission)
					+ "AND "
					+ getPriorityQuery(priority) + "LIMIT 1";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time);

			if (result.size() > 0)
				return true;
			else
				return false;

		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static boolean parentGroupFobiddanceCheck(String objectUUID, Vector<String> allGroups, int[] priority,
			int permission) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();

		try {
			String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
					+ "FROM `wasabi_rights` "
					+ "WHERE `object_id`=? "
					+ "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`<=0 AND `end_time`>=0)) "
					+ "AND "
					+ getGroupMembershipQuery(allGroups)
					+ "AND "
					+ getRightQueryDeny(permission)
					+ "AND "
					+ getPriorityQuery(priority) + "LIMIT 1";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time);

			if (result.size() > 0)
				return true;
			else
				return false;

		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
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

	private static int priorityGroupCheck(String objectUUID, String userUUID, Node userNode, int permission, Session s)
			throws UnexpectedInternalProblemException {

		Vector<String> allGroups = getGroupMemberships(userNode, s);

		// Check explicit group time rights
		if (parentGroupFobiddanceCheck(objectUUID, allGroups,
				new int[] { WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT }, permission))
			return -1;
		else if (parentGroupAllowanceCheck(objectUUID, allGroups,
				new int[] { WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT }, permission))
			return 1;

		// Check explicit group rights
		if (parentGroupFobiddanceCheck(objectUUID, allGroups, new int[] { WasabiACLPriority.EXPLICIT_GROUP_RIGHT },
				permission))
			return -1;
		else if (parentGroupAllowanceCheck(objectUUID, allGroups, new int[] { WasabiACLPriority.EXPLICIT_GROUP_RIGHT },
				permission))
			return 1;

		// Check inherited group time rights
		if (parentGroupFobiddanceCheck(objectUUID, allGroups,
				new int[] { WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT }, permission))
			return -1;
		else if (parentGroupAllowanceCheck(objectUUID, allGroups,
				new int[] { WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT }, permission))
			return 1;

		// Check inherited group rights
		if (parentGroupFobiddanceCheck(objectUUID, allGroups, new int[] { WasabiACLPriority.INHERITED_GROUP_RIGHT },
				permission))
			return -1;
		else if (parentGroupAllowanceCheck(objectUUID, allGroups,
				new int[] { WasabiACLPriority.INHERITED_GROUP_RIGHT }, permission))
			return 1;

		return 0;
	}

	// private static boolean checkParentGroupExplicitTimeAllowance(String objectUUID, String groupUUID, int permission,
	// Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// boolean explicitGroupTimeRight = false;
	//
	// try {
	// try {
	// String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
	// + "FROM `wasabi_rights` "
	// + "WHERE `object_id`=? "
	// + "AND `start_time`<=? AND `end_time`>=? "
	// + "AND `group_id`=? " + "AND " + getRightQuery(permission);
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time, groupUUID);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int right = getRight(wasabiACLEntry, permission);
	//
	// if (right == -1)
	// return false;
	// else
	// explicitGroupTimeRight = true;
	// }
	//
	// if (explicitGroupTimeRight) {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return true;
	// else
	// return checkParentGroupExplicitTimeAllowance(objectUUID, groupUUID, permission, s);
	// }
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static boolean checkParentGroupExplicitRightForbiddance(String objectUUID, String groupUUID,
	// int permission, Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// try {
	// String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
	// + "FROM `wasabi_rights` "
	// + "WHERE `object_id`=? "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `group_id`=? " + "AND " + getRightQueryDeny(permission) + "ORDER BY `priority`";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time, groupUUID);
	//
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int prio = wasabiACLEntry.getPriority();
	//
	// if (prio == WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT)
	// return false;
	//
	// if (prio == WasabiACLPriority.EXPLICIT_GROUP_RIGHT)
	// return false;
	// }
	//
	// if (groupNode == null)
	// return true;
	// else
	// return checkParentGroupExplicitRightForbiddance(objectUUID, groupNode.getIdentifier(), permission,
	// s);
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }

	// private static boolean checkParentGroupExplicitTimeForbiddance(String objectUUID, String groupUUID, int
	// permission,
	// Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// try {
	// String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` "
	// + "FROM `wasabi_rights` "
	// + "WHERE `object_id`=? "
	// + "AND `start_time`<=? "
	// + "AND `end_time`>=? "
	// + "AND `priority`=?"
	// + "AND `group_id`=? "
	// + "AND "
	// + getRightQueryDeny(permission);
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time,
	// WasabiACLPriority.GROUP_TIME_RIGHT, groupUUID);
	//
	// if (result.size() > 0)
	// return true;
	// else {
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// if (groupNode == null)
	// return false;
	// else
	// return checkParentGroupForTimeForbiddance(objectUUID, groupNode.getIdentifier(), permission, s);
	// }
	//
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }

	// private static int priorityGroupCheckWithAllowance(String objectUUID, String userUUID, Node userNode,
	// int permission, Session s) throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// long time = java.lang.System.currentTimeMillis();
	// Vector<String> groups = new Vector<String>();
	//
	// try {
	// try {
	// NodeIterator ni = UserServiceImpl.getMemberships(userNode);
	// while (ni.hasNext()) {
	// Node groupRef = ni.nextNode();
	// Node group = null;
	// try {
	// group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
	// } catch (ItemNotFoundException infe) {
	// groupRef.remove();
	// }
	// if (group != null) {
	// groups.add(group.getIdentifier());
	// }
	// }
	//
	// for (String groupUUID : groups) {
	// boolean groupTimeRightAllow = false;
	//
	// String getACLEntries =
	// "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority`"
	// + " FROM `wasabi_rights` "
	// + "WHERE `object_id`=? AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
	// + " AND `group_id`=?" + " AND " + getRightQuery(permission) + " ORDER BY `priority`";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time, groupUUID);
	//
	// Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
	// String groupName = s.getNodeByIdentifier(groupUUID).getName();
	// if (groupNode != null) {
	// String parentGroupName = groupNode.getName();
	// }
	// int counter = 0;
	// int size = result.size();
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// int prio = wasabiACLEntry.getPriority();
	// int right = getRight(wasabiACLEntry, permission);
	// counter++;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == -1)
	// break;
	//
	// if (prio == WasabiACLPriority.GROUP_TIME_RIGHT && right == 1)
	// groupTimeRightAllow = true;
	//
	// if ((counter == size || prio != WasabiACLPriority.GROUP_TIME_RIGHT) && groupTimeRightAllow) {
	// if (groupNode == null)
	// return 1;
	// else if (checkParentGroupForTimeRightsDeny(objectUUID, groupNode.getIdentifier(),
	// permission, s))
	// return 1;
	// }
	//
	// if (prio == WasabiACLPriority.GROUP_RIGHT && right == -1) {
	// if (groupNode == null)
	// return -1;
	// else if (checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s))
	// return 1;
	// }
	//
	// if (prio == WasabiACLPriority.GROUP_RIGHT && right == 1) {
	// if (groupNode == null)
	// return 1;
	// else if (checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s))
	// return 1;
	// }
	// }
	//
	// if (groupNode != null)
	// if (checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s))
	// return 1;
	// }
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return 0;
	// }

	private static int priorityUserCheck(String objectUUID, String userUUID, int permission)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();
		boolean explicitUserTimeRight = false;
		boolean inheritedUserTimeRight = false;
		boolean inheritedUserRight = false;

		try {
			String getACLEntries = "SELECT `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority` "
					+ "FROM `wasabi_rights` "
					+ "WHERE `object_id`=? "
					+ "AND `user_id`=? "
					+ "AND "
					+ getRightQuery(permission)
					+ "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
					+ "ORDER BY `priority`";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, userUUID, time, time);

			int counter = 0;
			int size = result.size();

			for (WasabiACLEntry wasabiACLEntry : result) {
				int right = getRight(wasabiACLEntry, permission);
				int prio = wasabiACLEntry.getPriority();
				counter++;

				if (prio == WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT) {
					if (right == -1)
						return -1;
					else if (right == 1)
						explicitUserTimeRight = true;
				}
				if ((counter == size || prio != WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT) && explicitUserTimeRight)
					return 1;

				if (prio == WasabiACLPriority.EXPLICIT_USER_RIGHT) {
					if (right == 1)
						return 1;
					else if (right == -1)
						return -1;
				}

				if (prio == WasabiACLPriority.INHERITED_USER_TIME_RIGHT) {
					if (right == -1)
						return -1;
					else if (right == 1)
						inheritedUserTimeRight = true;
				}

				if ((counter == size || prio != WasabiACLPriority.INHERITED_USER_TIME_RIGHT) && inheritedUserTimeRight)
					return 1;

				if (prio == WasabiACLPriority.INHERITED_USER_RIGHT) {
					if (right == -1)
						return -1;
					else if (right == 1)
						inheritedUserRight = true;
				}

				if ((counter == size || prio != WasabiACLPriority.INHERITED_USER_RIGHT) && inheritedUserRight)
					return 1;
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return 0;
	}

	// private static Vector<String> UserViewFilter(String parentUUID, String userUUID)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> results = new Vector<String>();
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// String getACLEntries = "SELECT DISTINCT `object_id` " + "FROM `wasabi_rights` " + "WHERE "
	// + "(`parent_id`=? AND`user_id`=? AND `view`=1 AND `priority`=3 AND `object_id` "
	// + "NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3) "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))" + "OR "
	// + "(`parent_id`=? AND`user_id`=? AND `view`=1 AND `priority`=2 AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) AND"
	// + "`object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND (`priority`=0 OR `priority`=1 OR `priority`=2) "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))" + "OR "
	// + "(`parent_id`=? AND`user_id`=? AND `view`=1 AND " + "(`priority`=1 OR `priority`=0) AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) AND"
	// + "`object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE `view`=-1 AND `priority`=0 "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID, userUUID, time, time, parentUUID,
	// userUUID, time, time, time, time, parentUUID, userUUID, time, time, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// results.add(wasabiACLEntry.getObject_Id());
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return results;
	// }
	//
	// private static Vector<String> UserPermissionFilter(String parentUUID, String userUUID, int permission)
	// throws UnexpectedInternalProblemException {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> results = new Vector<String>();
	// long time = java.lang.System.currentTimeMillis();
	//
	// try {
	// String getACLEntries = "SELECT DISTINCT `object_id` " + "FROM `wasabi_rights` " + "WHERE "
	// + "(`parent_id`=? AND`user_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND `priority`=3 AND `object_id` "
	// + "NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3) "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))"
	// + "OR "
	// + "(`parent_id`=? AND`user_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND `priority`=2 AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) AND"
	// + "`object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2) "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))"
	// + "OR "
	// + "(`parent_id`=? AND`user_id`=? AND "
	// + getRightQueryAllow(permission)
	// + "AND "
	// + "(`priority`=1 OR `priority`=0) AND "
	// + "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) AND"
	// + "`object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` "
	// + "WHERE "
	// + getRightQueryDeny(permission)
	// + "AND `priority`=0 "
	// + "AND ((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))))";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID, userUUID, time, time, parentUUID,
	// userUUID, time, time, time, time, parentUUID, userUUID, time, time, time, time);
	//
	// for (WasabiACLEntry wasabiACLEntry : result) {
	// results.add(wasabiACLEntry.getObject_Id());
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return results;
	// }
}
