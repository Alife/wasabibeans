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
			if (priorityUserCheck(objectUUID, userUUID, permission))
				return true;
			else if (priorityGroupCheck(objectUUID, userUUID, userNode, permission, s))
				return true;
			else
				return false;
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

	public static boolean authorize1(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			String userUUID = UserServiceImpl.getUserByName(callerPrincipal, s).getIdentifier();
			return priorityCheck1(objectUUID, userUUID, permission);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean authorize1(Node objectNode, String callerPrincipal, int[] permission, Session s)
			throws UnexpectedInternalProblemException {
		for (int i = 0; i < permission.length; i++) {
			if (authorize1(objectNode, callerPrincipal, permission[i], s))
				return true;
		}
		return false;
	}

	public static boolean authorize2(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			String userUUID = UserServiceImpl.getUserByName(callerPrincipal, s).getIdentifier();
			return priorityCheck2(objectUUID, userUUID, permission);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean authorize2(Node objectNode, String callerPrincipal, int[] permission, Session s)
			throws UnexpectedInternalProblemException {
		for (int i = 0; i < permission.length; i++) {
			if (authorize2(objectNode, callerPrincipal, permission[i], s))
				return true;
		}
		return false;
	}

	public static Vector<String> authorizeVIEW(Node objectNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node userNode = UserServiceImpl.getUserByName(callerPrincipal, s);
			String userUUID = userNode.getIdentifier();
			Vector<String> UserFilter = UserViewFilter(objectUUID, userUUID);
			Vector<String> GroupFilter = GroupViewFilter(objectUUID, userUUID, userNode, s);
			for (String string : GroupFilter) {
				if (!UserFilter.contains(string))
					UserFilter.add(string);
			}
			return UserFilter;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static boolean checkParentGroup(String objectUUID, String groupUUID, int permission, Session s)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		try {
			try {
				//TODO: Optimierung der Ergebnismenge durch permission filter
				String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant` FROM `wasabi_rights` "
						+ "WHERE `object_id`=? AND `group_id`=? AND `start_time`=0 AND `end_time`=0 AND "
						+ "(`view`=-1 OR `read`=-1 OR `comment`=-1 OR `execute`=-1 OR `insert`=-1 OR `write`=-1 OR `grant`=-1)";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, groupUUID);

				for (WasabiACLEntry wasabiACLEntry : result) {
					switch (permission) {
					case WasabiPermission.VIEW:
						int view = wasabiACLEntry.getView();
						if (view == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.READ:
						int read = wasabiACLEntry.getRead();
						if (read == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.EXECUTE:
						int execute = wasabiACLEntry.getExecute();
						if (execute == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.COMMENT:
						int comment = wasabiACLEntry.getComment();
						if (comment == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.INSERT:
						int insert = wasabiACLEntry.getInsert();
						if (insert == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.WRITE:
						int write = wasabiACLEntry.getWrite();
						if (write == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					case WasabiPermission.GRANT:
						int grant = wasabiACLEntry.getGrant();
						if (grant == -1)
							return false;
						else {
							Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
									.getGroup_Id()));
							if (groupNode != null)
								return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
							else
								return true;
						}
					}
				}
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return true;
	}

	private static boolean checkParentGroupForTimeRights(String objectUUID, String groupUUID, int permission, Session s)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();
		boolean ret = true;

		try {
			try {
				// TODO: Optimierung durch permission filter
				String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority` FROM `wasabi_rights` "
						+ "WHERE `object_id`=? AND `group_id`=? AND `start_time`<=? AND `end_time`>=? AND `start_time`!=0 AND `end_time`!=0 AND "
						+ "(`view`=-1 OR `read`=-1 OR `comment`=-1 OR `execute`=-1 OR `insert`=-1 OR `write`=-1 OR `grant`=-1)";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, groupUUID, time, time);

				for (WasabiACLEntry wasabiACLEntry : result) {
					switch (permission) {
					case WasabiPermission.VIEW:
						int view = wasabiACLEntry.getView();
						if (view == -1)
							return false;
					case WasabiPermission.READ:
						int read = wasabiACLEntry.getRead();
						if (read == -1)
							return false;
					case WasabiPermission.EXECUTE:
						int execute = wasabiACLEntry.getExecute();
						if (execute == -1)
							return false;
					case WasabiPermission.COMMENT:
						int comment = wasabiACLEntry.getComment();
						if (comment == -1)
							return false;
					case WasabiPermission.INSERT:
						int insert = wasabiACLEntry.getInsert();
						if (insert == -1)
							return false;
					case WasabiPermission.WRITE:
						int write = wasabiACLEntry.getWrite();
						if (write == -1)
							return false;
					case WasabiPermission.GRANT:
						int grant = wasabiACLEntry.getGrant();
						if (grant == -1)
							return false;
					}

					if (ret) {
						Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupUUID));
						if (groupNode == null)
							return true;
						else
							return checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s);
					}
				}
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return ret;
	}

	private static int checkUserPriority(List<WasabiACLEntry> ACLEntries, int permission) {
		int result = 0;

		for (WasabiACLEntry wasabiACLEntry : ACLEntries) {
			switch (permission) {
			case WasabiPermission.VIEW:
				int view = wasabiACLEntry.getView();
				if (view == -1)
					return -1;
				else if (view == 1)
					result = 1;
				break;
			case WasabiPermission.READ:
				int read = wasabiACLEntry.getRead();
				if (read == -1)
					return -1;
				else if (read == 1)
					result = 1;
				break;
			case WasabiPermission.EXECUTE:
				int execute = wasabiACLEntry.getExecute();
				if (execute == -1)
					return -1;
				else if (execute == 1)
					result = 1;
				break;
			case WasabiPermission.COMMENT:
				int comment = wasabiACLEntry.getComment();
				if (comment == -1)
					return -1;
				else if (comment == 1)
					result = 1;
				break;
			case WasabiPermission.INSERT:
				int insert = wasabiACLEntry.getInsert();
				if (insert == -1)
					return -1;
				else if (insert == 1)
					result = 1;
				break;
			case WasabiPermission.WRITE:
				int write = wasabiACLEntry.getWrite();
				if (write == -1)
					return -1;
				else if (write == 1)
					result = 1;
				break;
			case WasabiPermission.GRANT:
				int grant = wasabiACLEntry.getGrant();
				if (grant == -1)
					return -1;
				else if (grant == 1)
					result = 1;
				break;
			}

		}
		return result;
	}

	private static Vector<String> GroupViewFilter(String parentUUID, String userUUID, Node userNode, Session s)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		Vector<String> results = new Vector<String>();

		try {
			try {
				NodeIterator ni = UserServiceImpl.getMemberships(userNode);
				String SQLdisjunctor = "";
				while (ni.hasNext()) {
					Node groupRef = ni.nextNode();
					Node group = null;
					try {
						group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
					} catch (ItemNotFoundException infe) {
						groupRef.remove();
					}
					if (group != null) {
						SQLdisjunctor = SQLdisjunctor + "`group_id`='" + group.getIdentifier() + "' OR ";
					}
				}

				SQLdisjunctor = SQLdisjunctor + "`group_id`='placeholderValue' ";
				String getACLEntries = "SELECT `object_id`, `group_id`, `view`, priority, start_time, end_time FROM `wasabi_rights` "
					+ "WHERE `parent_id`=? AND "
					+ "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0)) "
					+ "AND `view`=1 AND ("
					+ SQLdisjunctor
					+ ") AND object_id NOT IN(SELECT object_id FROM `wasabi_rights` WHERE `parent_id`=? AND "
					+ "((`start_time`<=? AND `end_time`>=?) OR (`start_time`=0 AND `end_time`=0))"
					+ " `view`=-1 AND `priority`="
					+ WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT
					+ ") GROUP BY object_id";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID);

				for (WasabiACLEntry wasabiACLEntry : result) {
					String objectUUID = wasabiACLEntry.getObject_Id();
					String groupUUID = wasabiACLEntry.getGroup_Id();
					if (checkParentGroup(objectUUID, groupUUID, WasabiPermission.VIEW, s))
						results.add(objectUUID);
				}
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

		return results;
	}

	private static boolean priorityCheck1(String objectUUID, String userUUID, int permission)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();

		// explicit user time rights
		String getExplicitUserTimeRights = "SELECT * FROM wasabi_rights "
				+ "WHERE `object_id`=? AND `user_id`=? AND `start_time`<=? AND `end_time`>=? AND `inheritance_id`=?";
		try {
			ResultSetHandler<List<WasabiACLEntry>> h1 = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result1 = run.query(getExplicitUserTimeRights, h1, objectUUID, userUUID, time, time,
					"");

			// Allowance check
			if (checkUserPriority(result1, permission) == -1) {
				return false;
			} else if (checkUserPriority(result1, permission) == 1) {
				return true;
			} else {
				// inherited user time rights
				String getInheritedUserTimeRights = "SELECT * FROM wasabi_rights "
						+ "WHERE `object_id`=? AND `user_id`=? AND `start_time`<=? AND `end_time`>=? AND `inheritance_id`!=?";
				try {
					ResultSetHandler<List<WasabiACLEntry>> h2 = new BeanListHandler<WasabiACLEntry>(
							WasabiACLEntry.class);
					List<WasabiACLEntry> result2 = run.query(getInheritedUserTimeRights, h2, objectUUID, userUUID,
							time, time, "");

					// Allowance check
					if (checkUserPriority(result2, permission) == -1) {
						return false;
					} else if (checkUserPriority(result2, permission) == 1) {
						return true;
					} else {
						// explicit user rights
						String getExplicitUserRights = "SELECT * FROM wasabi_rights "
								+ "WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
						try {
							ResultSetHandler<List<WasabiACLEntry>> h3 = new BeanListHandler<WasabiACLEntry>(
									WasabiACLEntry.class);
							List<WasabiACLEntry> result3 = run.query(getExplicitUserRights, h3, objectUUID, userUUID,
									0, 0, "");

							// Allowance check
							if (checkUserPriority(result3, permission) == -1) {
								return false;
							} else if (checkUserPriority(result3, permission) == 1) {
								return true;
							} else {
								String getInheritedUserRights = "SELECT * FROM wasabi_rights "
										+ "WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`!=?";

								try {
									ResultSetHandler<List<WasabiACLEntry>> h4 = new BeanListHandler<WasabiACLEntry>(
											WasabiACLEntry.class);
									List<WasabiACLEntry> result4 = run.query(getInheritedUserRights, h4, objectUUID,
											userUUID, 0, 0, "");

									// Allowance check
									if (checkUserPriority(result4, permission) == -1) {
										return false;
									} else if (checkUserPriority(result4, permission) == 1) {
										return true;
									} else {
										// TODO: Group check mechanism
										return false;
									}
								} catch (SQLException e) {
									throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
								}
							}
						} catch (SQLException e) {
							throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
						}
					}
				} catch (SQLException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
				}
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static boolean priorityCheck2(String objectUUID, String userUUID, int permission)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();

		try {
			String getACLEntries = "SELECT * FROM wasabi_rights WHERE `object_id`=? AND `user_id`=?";
			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, userUUID);

			// Filter and check explicit user time rights
			List<WasabiACLEntry> explicitUserTimeRights = new Vector<WasabiACLEntry>();

			for (WasabiACLEntry wasabiACLEntry : result) {
				if (wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() <= time
						&& wasabiACLEntry.getEnd_Time() >= time) {
					explicitUserTimeRights.add(wasabiACLEntry);
				}
			}

			// Allowance check
			if (checkUserPriority(explicitUserTimeRights, permission) == -1) {
				return false;
			} else if (checkUserPriority(explicitUserTimeRights, permission) == 1) {
				return true;
			} else {
				// Filter inherited user time rights
				List<WasabiACLEntry> inheritedUserTimeRights = new Vector<WasabiACLEntry>();

				for (WasabiACLEntry wasabiACLEntry : result) {
					if (!wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() <= time
							&& wasabiACLEntry.getEnd_Time() >= time) {
						inheritedUserTimeRights.add(wasabiACLEntry);
					}
				}

				// Allowance check
				if (checkUserPriority(inheritedUserTimeRights, permission) == -1) {
					return false;
				} else if (checkUserPriority(inheritedUserTimeRights, permission) == 1) {
					return true;
				} else {
					// Filter explicit user rights
					List<WasabiACLEntry> explicitUserRights = new Vector<WasabiACLEntry>();

					for (WasabiACLEntry wasabiACLEntry : result) {
						if (wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() == 0
								&& wasabiACLEntry.getEnd_Time() == 0) {
							explicitUserRights.add(wasabiACLEntry);
						}
					}

					// Allowance check
					if (checkUserPriority(explicitUserRights, permission) == -1) {
						return false;
					} else if (checkUserPriority(explicitUserRights, permission) == 1) {
						return true;
					} else {
						// Filter inherited user rights
						List<WasabiACLEntry> inheritedUserRights = new Vector<WasabiACLEntry>();

						for (WasabiACLEntry wasabiACLEntry : result) {
							if (!wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() == 0
									&& wasabiACLEntry.getEnd_Time() == 0) {
								inheritedUserRights.add(wasabiACLEntry);
							}
						}

						if (checkUserPriority(inheritedUserRights, permission) == -1) {
							return false;
						} else if (checkUserPriority(inheritedUserRights, permission) == 1) {
							return true;
						} else {
							return false;
							// TODO: Group check mechanism
						}
					}
				}
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static boolean priorityGroupCheck(String objectUUID, String userUUID, Node userNode, int permission,
			Session s) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String InitGroupID = "";
		int initPrio = -1;
		boolean ret = false;
		long time = java.lang.System.currentTimeMillis();

		try {
			try {
				NodeIterator ni = UserServiceImpl.getMemberships(userNode);
				String SQLdisjunctor = "";
				while (ni.hasNext()) {
					Node groupRef = ni.nextNode();
					Node group = null;
					try {
						group = groupRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
					} catch (ItemNotFoundException infe) {
						groupRef.remove();
					}
					if (group != null) {
						SQLdisjunctor = SQLdisjunctor + "`group_id`='" + group.getIdentifier() + "' OR ";
					}
				}

				SQLdisjunctor = SQLdisjunctor + "`group_id`='placeholderValue' ";
				// TODO: Optimierung in Abhängigkeit vom Recht
				String getACLEntries = "SELECT `group_id`, `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, `priority` FROM `wasabi_rights` "
						+ "WHERE `object_id`=? AND `start_time`<=? AND `end_time`>=? AND ("
						+ SQLdisjunctor
						+ ") ORDER BY `priority`, `group_id`";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, time, time);

				for (WasabiACLEntry wasabiACLEntry : result) {
					String groupID = wasabiACLEntry.getGroup_Id();
					int prio = wasabiACLEntry.getPriority();

					// Initialize groupID and priority
					if (InitGroupID == "" && initPrio == -1) {
						InitGroupID = groupID;
						initPrio = prio;
					}

					// If end of rights bundle is reached and all rights at this level are true, check next level.
					if ((groupID != InitGroupID || initPrio != prio) && ret == true) {
						Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(wasabiACLEntry
								.getGroup_Id()));
						if (groupNode == null)
							return true;
						else
							ret = checkParentGroupForTimeRights(objectUUID, groupNode.getIdentifier(), permission, s);
						if (ret == true)
							return true;
						else {
							InitGroupID = groupID;
							initPrio = prio;
						}
					}

					// Collecting rights of actual rights bundle (time rights).
					if (groupID == InitGroupID && initPrio == prio && prio == WasabiACLPriority.GROUP_TIME_RIGHT) {
						switch (permission) {
						case WasabiPermission.VIEW:
							int view = wasabiACLEntry.getView();
							if (view == -1)
								return false;
							if (view == 1)
								ret = true;
						case WasabiPermission.READ:
							int read = wasabiACLEntry.getRead();
							if (read == -1)
								return false;
							if (read == 1)
								ret = true;
						case WasabiPermission.EXECUTE:
							int execute = wasabiACLEntry.getExecute();
							if (execute == -1)
								return false;
							if (execute == 1)
								ret = true;
						case WasabiPermission.COMMENT:
							int comment = wasabiACLEntry.getComment();
							if (comment == -1)
								return false;
							if (comment == 1)
								ret = true;
						case WasabiPermission.INSERT:
							int insert = wasabiACLEntry.getInsert();
							if (insert == -1)
								return false;
							if (insert == 1)
								ret = true;
						case WasabiPermission.WRITE:
							int write = wasabiACLEntry.getWrite();
							if (write == -1)
								return false;
							if (write == 1)
								ret = true;
						case WasabiPermission.GRANT:
							int grant = wasabiACLEntry.getGrant();
							if (grant == -1)
								return false;
							if (grant == 1)
								ret = true;
						}
					}

					// Check normal group rights
					if (prio == WasabiACLPriority.GROUP_RIGHT) {
						switch (permission) {
						case WasabiPermission.VIEW:
							int view = wasabiACLEntry.getView();
							if (view == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.READ:
							int read = wasabiACLEntry.getRead();
							if (read == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.EXECUTE:
							int execute = wasabiACLEntry.getExecute();
							if (execute == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.COMMENT:
							int comment = wasabiACLEntry.getComment();
							if (comment == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.INSERT:
							int insert = wasabiACLEntry.getInsert();
							if (insert == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.WRITE:
							int write = wasabiACLEntry.getWrite();
							if (write == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						case WasabiPermission.GRANT:
							int grant = wasabiACLEntry.getGrant();
							if (grant == 1) {
								Node groupNode = GroupServiceImpl.getParentGroup(s.getNodeByIdentifier(groupID));
								if (groupNode != null)
									return checkParentGroup(objectUUID, groupNode.getIdentifier(), permission, s);
								else
									return true;
							}
						}
					}
				}
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return false;
	}

	private static boolean priorityUserCheck(String objectUUID, String userUUID, int permission)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();
		boolean ret = false;
		int priority1 = -1;

		try {
			// TODO: Optimierung durch Query in Abhängigkeit vom angeforderten Recht
			String getACLEntries = "SELECT `view`, `read`, `comment`, `execute`, `insert`, `write`, `grant`, MIN(priority) FROM `wasabi_rights` "
					+ "WHERE `object_id`=? AND `user_id`=? AND `start_time`<=? AND `end_time`>=? GROUP BY `object_id`";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID, userUUID, time, time);

			for (WasabiACLEntry wasabiACLEntry : result) {
				int prio = wasabiACLEntry.getPriority();

				if (priority1 != prio) {
					if (ret)
						return true;
					priority1 = prio;
				}

				switch (permission) {
				case WasabiPermission.VIEW:
					int view = wasabiACLEntry.getView();
					if (view == 1)
						ret = true;
					else if (view == -1)
						return false;
				case WasabiPermission.READ:
					int read = wasabiACLEntry.getRead();
					if (read == 1)
						ret = true;
					else if (read == -1)
						return false;
				case WasabiPermission.COMMENT:
					int comment = wasabiACLEntry.getComment();
					if (comment == 1)
						ret = true;
					else if (comment == -1)
						return false;
				case WasabiPermission.EXECUTE:
					int execute = wasabiACLEntry.getExecute();
					if (execute == 1)
						ret = true;
					else if (execute == -1)
						return false;
				case WasabiPermission.INSERT:
					int insert = wasabiACLEntry.getInsert();
					if (insert == 1)
						ret = true;
					else if (insert == -1)
						return false;
				case WasabiPermission.WRITE:
					int write = wasabiACLEntry.getWrite();
					if (write == 1)
						ret = true;
					else if (write == -1)
						return false;
				case WasabiPermission.GRANT:
					int grant = wasabiACLEntry.getGrant();
					if (grant == 1)
						ret = true;
					else if (grant == -1)
						return false;
				}
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
		return false;
	}

	private static Vector<String> UserViewFilter(String parentUUID, String userUUID)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		Vector<String> resultsAllow = new Vector<String>();
		Vector<String> resultsDeny = new Vector<String>();

		long time = java.lang.System.currentTimeMillis();

		try {
			// TODO: Fix it
			String getACLEntries = "SELECT `object_id`,`view`,MIN(priority) FROM `wasabi_rights` "
					+ "WHERE `parent_id`=? AND `user_id`=? AND (`view`=1 OR `view`=-1) AND `start_time`<=? AND `end_time`>=? GROUP BY `object_id`";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, parentUUID, userUUID, time, time);

			for (WasabiACLEntry wasabiACLEntry : result) {
				if (wasabiACLEntry.getView() == 1)
					resultsAllow.add(wasabiACLEntry.getObject_Id());

				if (wasabiACLEntry.getView() == -1)
					resultsDeny.add(wasabiACLEntry.getObject_Id());
			}

			for (String string : resultsDeny) {
				resultsAllow.remove(string);
			}

		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

		return resultsAllow;
	}
}
