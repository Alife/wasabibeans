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

	private static long maxLifeTime = 0;

	/**
	 * Checks if a requesting user is allowed to access a given objectNode by given permission. If WasabiCertificate is
	 * enable and result of check is true, a certificate for objectNode and requesting user will be created.
	 * 
	 * @param objectNode
	 * @param callerPrincipal
	 * @param permission
	 * @param s
	 * @return true if a requesting user has given permission on given object, otherwise false
	 * @throws UnexpectedInternalProblemException
	 */
	public static boolean authorize(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {

		/* WasabiCertificate - Begin */
		if (WasabiConstants.ACL_CERTIFICATE_ENABLE) {
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = ObjectServiceImpl.getUUID(userNode);
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);

			boolean cert = WasabiCertificate.getCertificate(userUUID, objectUUID, permission);

			if (cert) {
				return true;
			} else if (checkCalcRights(objectUUID, userUUID, userNode, permission, s)) {
				WasabiCertificate.setCertificate(userUUID, objectUUID, permission, maxLifeTime);
				return true;
			} else
				return false;
		}
		/* WasabiCertificate - End */
		else {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = ObjectServiceImpl.getUUID(userNode);

			// Variante 1
			// return checkRights(objectUUID, userUUID, userNode, permission, s);

			// Variante 2
			// if (existsTimeRights(objectUUID, userUUID, userNode, permission, s)) {
			// return checkTimeRights(objectUUID, userUUID, userNode, permission, s);
			// } else if (existsNormalRights(objectUUID, userUUID, userNode, permission, s)) {
			// return checkNormalRights(objectUUID, userUUID, userNode, permission, s);
			// } else
			// return false;

			// Variante 3
			return checkCalcRights(objectUUID, userUUID, userNode, permission, s);
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

	/**
	 * Checks recursive if children has permission. If not the result is 1. The count of recursive calls is a sum over
	 * this calls -> If one child has not the permission, the sum is greater than 0.
	 * 
	 * @param objectNode
	 * @param callerPrincipal
	 * @param permission
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
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

	/**
	 * Checks if a requesting user is allowed to access a given objectNode and its children by given permission.
	 * Recursion at {@link authorizeChild}
	 * 
	 * @param objectNode
	 * @param callerPrincipal
	 * @param permission
	 * @param s
	 * @return true if a requesting user has given permission on given object and children, otherwise false
	 * @throws UnexpectedInternalProblemException
	 */
	public static boolean authorizeChildreen(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		if (authorizeChild(objectNode, callerPrincipal, permission, s) > 0)
			return false;
		else
			return true;
	}

	/**
	 * Returns a set of objectUUIDs with given permission and which parent is objectNode. The result set is provides by
	 * {@link permissionFilter}.
	 * 
	 * @param objectNode
	 * @param callerPrincipal
	 * @param permission
	 * @param wasabiType
	 * @param s
	 * @return a set of objectUUIDs with given permission and which parent is objectNode
	 * @throws UnexpectedInternalProblemException
	 */
	public static Vector<String> authorizePermission(Node objectNode, String callerPrincipal, int permission,
			WasabiType wasabiType, Session s) throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = userNode.getIdentifier();

			return permissionFilter1(objectUUID, userUUID, userNode, wasabiType, permission, s);
			// return permissionFilter2(objectUUID, userUUID, userNode, wasabiType, permission, s);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Core method for {@link authorize}, returns if permission on a given objectNode is allowed. First step a query
	 * selects all rights for a given objectNode filtered by permission and ordered by priority. Second step a loop
	 * checks for each right its permission.
	 * 
	 * @param objectUUID
	 * @param userUUID
	 * @param userNode
	 * @param permission
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	private static boolean checkCalcRights(String objectUUID, String userUUID, Node userNode, int permission, Session s)
			throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			Vector<String> allGroups = getGroupMemberships(userNode, s);
			String allGroupsQuery = getGroupMembershipQuery(allGroups);
			String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
			long time = java.lang.System.currentTimeMillis();
			int[] rights = new int[8];
			maxLifeTime = 0;

			String getRights = "SELECT `object_id`, "
					+ "`view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, " + "`priority`,`end_time` "
					+ "FROM `wasabi_rights` WHERE " + "`object_id`=? " + " AND ((`start_time`<=" + time
					+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) " + "AND " + identityCheck
					+ "ORDER BY `priority`,`distance`";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getRights, h, objectUUID);

			if (result.size() == 0)
				return false;
			else {
				for (WasabiACLEntry wasabiACLEntry : result) {
					int prio = wasabiACLEntry.getPriority();
					int right = getRight(wasabiACLEntry, permission);
					long endTime = wasabiACLEntry.getEnd_Time();

					switch (prio) {
					case WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT:
						if (right == -1)
							return false;
						else if (right == 1) {
							rights[WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT] = 1;
							if (endTime > maxLifeTime)
								maxLifeTime = endTime;
						}
						break;
					case WasabiACLPriority.INHERITED_USER_TIME_RIGHT:
						if (WasabiConstants.ACL_DISTANCE_CHECK) {
							if (rights[WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1)
								return true;
						} else {
							if (rights[WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1) {
								rights[WasabiACLPriority.INHERITED_USER_TIME_RIGHT] = 1;
								if (endTime > maxLifeTime)
									maxLifeTime = endTime;
							}
						}
						break;
					case WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT:
						if (rights[WasabiACLPriority.INHERITED_USER_TIME_RIGHT] == 1)
							return true;
						else if (right == -1)
							return false;
						else if (right == 1) {
							rights[WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT] = 1;
							if (endTime > maxLifeTime)
								maxLifeTime = endTime;
						}
						break;
					case WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT:
						if (WasabiConstants.ACL_DISTANCE_CHECK) {
							if (rights[WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1)
								return true;
						} else {
							if (rights[WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1) {
								rights[WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT] = 1;
								if (endTime > maxLifeTime)
									maxLifeTime = endTime;
							}
						}
						break;
					case WasabiACLPriority.EXPLICIT_USER_RIGHT:
						if (rights[WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT] == 1)
							return true;
						else if (right == -1)
							return false;
						else if (right == 1)
							return true;
						break;
					case WasabiACLPriority.INHERITED_USER_RIGHT:
						if (WasabiConstants.ACL_DISTANCE_CHECK) {
							if (right == -1)
								return false;
							else if (right == 1)
								return true;
						} else {
							if (right == -1)
								return false;
							else if (right == 1)
								rights[WasabiACLPriority.INHERITED_USER_RIGHT] = 1;
						}
						break;
					case WasabiACLPriority.EXPLICIT_GROUP_RIGHT:
						if (rights[WasabiACLPriority.INHERITED_USER_RIGHT] == 1) {
							return true;
						} else if (right == -1)
							return false;
						else if (right == 1)
							rights[WasabiACLPriority.EXPLICIT_GROUP_RIGHT] = 1;
						break;
					case WasabiACLPriority.INHERITED_GROUP_RIGHT:
						if (WasabiConstants.ACL_DISTANCE_CHECK) {
							if (rights[WasabiACLPriority.EXPLICIT_GROUP_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1)
								return true;
						} else {
							if (rights[WasabiACLPriority.EXPLICIT_GROUP_RIGHT] == 1)
								return true;
							else if (right == -1)
								return false;
							else if (right == 1)
								rights[WasabiACLPriority.INHERITED_GROUP_RIGHT] = 1;
						}
						break;
					}
				}

				if (rights[WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.INHERITED_USER_TIME_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.INHERITED_USER_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.EXPLICIT_GROUP_RIGHT] == 1)
					return true;
				if (rights[WasabiACLPriority.INHERITED_GROUP_RIGHT] == 1)
					return true;
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
		return false;
	}

	// private static boolean checkNormalRights(String objectUUID, String userUUID, Node userNode, int permission,
	// Session s) throws UnexpectedInternalProblemException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	// String rightQueryAllow = getRightQueryAllow(permission);
	// String rightQueryDeny = getRightQueryDeny(permission);
	// String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
	//
	// String explicitUserRights = "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryAllow + "AND " + "`priority`=" + WasabiACLPriority.EXPLICIT_USER_RIGHT
	// + " AND `start_time`=0 AND `end_time`=0 " + "AND " + identityCheck;
	//
	// ResultSetHandler<List<WasabiACLEntry>> h1 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result1 = run.query(explicitUserRights, h1, objectUUID);
	//
	// if (result1.size() > 0)
	// return true;
	// else {
	// String inheritedUserRights = "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryAllow + "AND " + "`priority`=" + WasabiACLPriority.INHERITED_USER_RIGHT
	// + " AND `start_time`=0 AND `end_time`=0" + " AND " + identityCheck + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND " + rightQueryDeny
	// + "AND " + "`priority`=" + WasabiACLPriority.INHERITED_USER_RIGHT
	// + " AND `start_time`=0 AND `end_time`=0 " + " AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h2 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result2 = run.query(inheritedUserRights, h2, objectUUID, objectUUID);
	//
	// if (result2.size() > 0)
	// return true;
	// else {
	// String explicitGroupRights = "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND " + rightQueryAllow + "AND" + "`priority`="
	// + WasabiACLPriority.EXPLICIT_GROUP_RIGHT + " AND `start_time`=0 AND `end_time`=0 " + "AND "
	// + identityCheck + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND " + rightQueryDeny
	// + "AND " + "`priority`=" + WasabiACLPriority.EXPLICIT_GROUP_RIGHT
	// + " AND `start_time`=0 AND `end_time`=0 " + "AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h3 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result3 = run.query(explicitGroupRights, h3, objectUUID, objectUUID);
	//
	// if (result3.size() > 0)
	// return true;
	// else {
	// String inheritedGroupRights = "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND " + rightQueryAllow + "AND " + "`priority`="
	// + WasabiACLPriority.INHERITED_GROUP_RIGHT + " AND `start_time`=0 AND `end_time`=0 "
	// + "AND " + identityCheck + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryDeny + "AND " + "`priority`=" + WasabiACLPriority.INHERITED_GROUP_RIGHT
	// + " AND `start_time`=0 AND `end_time`=0 " + "AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h4 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result4 = run.query(inheritedGroupRights, h4, objectUUID, objectUUID);
	//
	// if (result4.size() > 0)
	// return true;
	// }
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static boolean checkRights(String objectUUID, String userUUID, Node userNode, int permission, Session s)
	// throws UnexpectedInternalProblemException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	//
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	// String rightQueryAllow = getRightQueryAllow(permission);
	// String rightQueryDeny = getRightQueryDeny(permission);
	// String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
	// String userCheck = "`user_id`='" + userUUID + "' ";
	//
	// long time = java.lang.System.currentTimeMillis();
	//
	// String getACLEntries =
	// "SELECT `object_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority` "
	// + "FROM `wasabi_rights` WHERE " + "(`object_id`='"
	// + objectUUID
	// + "' AND `priority`="
	// + WasabiACLPriority.INHERITED_GROUP_RIGHT
	// + " AND "
	// + identityCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// +
	// "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6 OR `priority`=7))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.EXPLICIT_GROUP_RIGHT
	// + " AND "
	// + identityCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// +
	// "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5 OR `priority`=6))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.INHERITED_USER_RIGHT
	// + " AND "
	// + userCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4 OR `priority`=5))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.EXPLICIT_USER_RIGHT
	// + " AND "
	// + userCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3 OR `priority`=4))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT
	// + " AND "
	// + identityCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2 OR `priority`=3))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT
	// + " AND "
	// + identityCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=0 OR `priority`=1 OR `priority`=2))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.INHERITED_USER_TIME_RIGHT
	// + " AND "
	// + userCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND (`priority`=0 OR `priority`=1))) "
	// + "OR "
	// + "(`object_id`='"
	// + objectUUID
	// + "' "
	// + " AND `priority`="
	// + WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT
	// + " AND "
	// + userCheck
	// + " AND "
	// + rightQueryAllow
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + ") OR (`start_time`=0 AND `end_time`=0)) "
	// + "AND `object_id` NOT IN(SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + rightQueryDeny
	// + "AND `object_id`='"
	// + objectUUID
	// + "' "
	// + "AND "
	// + identityCheck
	// + "AND ((`start_time`<="
	// + time
	// + " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) " + "AND `priority`=0)) ";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getACLEntries, h);
	//
	// if (result.size() > 0)
	// return true;
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static boolean checkTimeRights(String objectUUID, String userUUID, Node userNode, int permission, Session
	// s)
	// throws UnexpectedInternalProblemException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	// String rightQueryAllow = getRightQueryAllow(permission);
	// String rightQueryDeny = getRightQueryDeny(permission);
	// String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
	// long time = java.lang.System.currentTimeMillis();
	//
	// String explicitUserTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryAllow + "AND" + "`priority`=" + WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT
	// + " AND `start_time`<=" + time + " AND `end_time`>=" + time + " AND " + identityCheck
	// + "AND `object_id` NOT IN(" + "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND " + rightQueryDeny + "AND" + "`priority`="
	// + WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT + " AND `start_time`<=" + time + " AND `end_time`>="
	// + time + "AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h1 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result1 = run.query(explicitUserTimeRights, h1, objectUUID, objectUUID);
	//
	// if (result1.size() > 0)
	// return true;
	// else {
	// String inheritedUserTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryAllow
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.INHERITED_USER_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + " AND "
	// + identityCheck
	// + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryDeny
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.INHERITED_USER_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>=" + time + "AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h2 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result2 = run.query(inheritedUserTimeRights, h2, objectUUID, objectUUID);
	//
	// if (result2.size() > 0)
	// return true;
	// else {
	// String explicitGroupTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryAllow
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + "AND "
	// + identityCheck
	// + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryDeny
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>=" + time + "AND " + identityCheck + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h3 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result3 = run.query(explicitGroupTimeRights, h3, objectUUID, objectUUID);
	//
	// if (result3.size() > 0)
	// return true;
	// else {
	// String inheritedGroupTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryAllow
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + " AND "
	// + identityCheck
	// + "AND `object_id` NOT IN("
	// + "SELECT `object_id` FROM `wasabi_rights` WHERE "
	// + "`object_id`=? AND "
	// + rightQueryDeny
	// + "AND"
	// + "`priority`="
	// + WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT
	// + " AND `start_time`<="
	// + time
	// + " AND `end_time`>="
	// + time
	// + "AND "
	// + identityCheck
	// + ")";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h4 = new BeanListHandler<WasabiACLEntry>(
	// WasabiACLEntry.class);
	// List<WasabiACLEntry> result4 = run.query(inheritedGroupTimeRights, h4, objectUUID, objectUUID);
	//
	// if (result4.size() > 0)
	// return true;
	// }
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static boolean existsNormalRights(String objectUUID, String userUUID, Node userNode, int permission,
	// Session s) throws UnexpectedInternalProblemException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	// String rightQueryAllow = getRightQueryAllow(permission);
	// String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
	//
	// String existTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryAllow + "AND " + "(`priority`=" + WasabiACLPriority.EXPLICIT_USER_RIGHT + " OR "
	// + "`priority`=" + WasabiACLPriority.EXPLICIT_GROUP_RIGHT + " OR " + "`priority`="
	// + WasabiACLPriority.INHERITED_USER_RIGHT + " OR " + "`priority`="
	// + WasabiACLPriority.INHERITED_GROUP_RIGHT + ") " + "AND " + identityCheck;
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(existTimeRights, h, objectUUID);
	//
	// if (result.size() > 0)
	// return true;
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	// private static boolean existsTimeRights(String objectUUID, String userUUID, Node userNode, int permission,
	// Session s)
	// throws UnexpectedInternalProblemException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// Vector<String> allGroups = getGroupMemberships(userNode, s);
	// String allGroupsQuery = getGroupMembershipQuery(allGroups);
	// String rightQueryAllow = getRightQueryAllow(permission);
	// String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";
	//
	// String existTimeRights = "SELECT `object_id` FROM `wasabi_rights` WHERE " + "`object_id`=? AND "
	// + rightQueryAllow + "AND " + "(`priority`=" + WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT + " OR "
	// + "`priority`=" + WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT + " OR " + "`priority`="
	// + WasabiACLPriority.INHERITED_USER_TIME_RIGHT + " OR " + "`priority`="
	// + WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT + ") " + "AND " + identityCheck;
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(existTimeRights, h, objectUUID);
	//
	// if (result.size() > 0)
	// return true;
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// return false;
	// }

	/**
	 * Build a part of a SQL query, that contains all group memberships.
	 * 
	 * @param allGroups
	 * @return part of a SQL query, that contains all group memberships
	 */
	private static String getGroupMembershipQuery(Vector<String> allGroups) {
		String groupMembershipQuery = "(";

		for (String group : allGroups) {
			groupMembershipQuery = groupMembershipQuery + "`group_id`='" + group + "' OR ";
		}

		groupMembershipQuery = groupMembershipQuery + "`group_id`='placeholderValueGroup42') ";

		return groupMembershipQuery;
	}

	/**
	 * Builds a {@code Vector<String>}, that contains all group memberships of a given userNode. {@code Vector<String>}
	 * of groups is needed by {@link getGroupMembershipQuery}.
	 * 
	 * @param userNode
	 * @param s
	 * @return all group memberships
	 * @throws UnexpectedInternalProblemException
	 */
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
					try {
						groupRef.remove();
						s.save();
					} catch (RepositoryException re) {
						/*
						 * do nothing -> remove failed -> reference already removed by another thread concurrently or
						 * currently locked
						 */
					}
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

	/**
	 * Returns right of a given ACL entry by given permission.
	 * 
	 * @param wasabiACLEntry
	 * @param permission
	 * @return right of a given ACL entry by given permission
	 */
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

	/**
	 * Build a part of a SQL query.
	 * 
	 * @param permission
	 * @return
	 */
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

	/**
	 * Build a part of a SQL query.
	 * 
	 * @param permission
	 * @return
	 */
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

	/**
	 * Checks if a requesting user is an administrator.
	 * 
	 * @param callerPrincipal
	 * @param s
	 * @return true is requesting user is an administrator, false if not
	 * @throws UnexpectedInternalProblemException
	 */
	public static boolean isAdminUser(String callerPrincipal, Session s) throws UnexpectedInternalProblemException {
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		if (GroupServiceImpl.isMember(GroupServiceImpl.getAdminGroup(s), userNode))
			return true;
		return false;
	}

	/**
	 * Checks if a requesting user is member at group "pipes and filter".
	 * 
	 * @param callerPrincipal
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	public static boolean isPafUser(String callerPrincipal, Session s) throws UnexpectedInternalProblemException {
		Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
		if (GroupServiceImpl.isMember(GroupServiceImpl.getGroupByName(WasabiConstants.PAF_GROUP_NAME, s), userNode))
			return true;
		return false;
	}

	/**
	 * Filters objects by permission
	 * 
	 * @param parentUUID
	 * @param userUUID
	 * @param userNode
	 * @param wasabiType
	 * @param permission
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	private static Vector<String> permissionFilter1(String parentUUID, String userUUID, Node userNode,
			WasabiType wasabiType, int permission, Session s) throws UnexpectedInternalProblemException {
		try {
			SqlConnector sqlConnector = new SqlConnector();
			QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

			try {
				Vector<String> results = new Vector<String>();
				Vector<String> allGroups = getGroupMemberships(userNode, s);

				String allGroupsQuery = getGroupMembershipQuery(allGroups);
				String rightQueryAllow = getRightQueryAllow(permission);
				String identityCheck = "(`user_id`='" + userUUID + "' OR " + allGroupsQuery + ") ";

				long time = java.lang.System.currentTimeMillis();

				String getACLEntries = "SELECT DISTINCT `object_id` " + "FROM `wasabi_rights` WHERE " + "`parent_id`='"
						+ parentUUID + "'" + " AND `wasabi_type`='" + wasabiType.toString() + "'" + " AND "
						+ identityCheck + " AND " + rightQueryAllow + "AND ((`start_time`<=" + time
						+ " AND `end_time`>=" + time + ") OR (`start_time`=0 AND `end_time`=0)) ";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getACLEntries, h);

				for (WasabiACLEntry wasabiACLEntry : result) {
					String objectUUID = wasabiACLEntry.getObject_Id();
					if (authorize(s.getNodeByIdentifier(objectUUID), userNode.getName(), permission, s))
						results.add(wasabiACLEntry.getObject_Id());
				}

				return results;
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			} finally {
				sqlConnector.close();
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Filters objects by permission
	 * 
	 * @param parentUUID
	 * @param userUUID
	 * @param userNode
	 * @param wasabiType
	 * @param permission
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 */
	private static Vector<String> permissionFilter2(String parentUUID, String userUUID, Node userNode,
			WasabiType wasabiType, int permission, Session s) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
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
		} finally {
			sqlConnector.close();
		}
	}

}
