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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

public class WasabiAuthorizer {

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

	private static int checkUserPriority(List<WasabiACLEntry> ACLEntries, int permission) {
		int result = 0;

		for (WasabiACLEntry wasabiACLEntry : ACLEntries) {
			switch (permission) {
			case WasabiPermission.VIEW:
				int view = wasabiACLEntry.getInsert();
				if (view == -1)
					return -1;
				else if (view == 1)
					result = 1;
				break;
			case WasabiPermission.READ:
				int read = wasabiACLEntry.getInsert();
				if (read == -1)
					return -1;
				else if (read == 1)
					result = 1;
				break;
			case WasabiPermission.EXECUTE:
				int execute = wasabiACLEntry.getInsert();
				if (execute == -1)
					return -1;
				else if (execute == 1)
					result = 1;
				break;
			case WasabiPermission.COMMENT:
				int comment = wasabiACLEntry.getInsert();
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
				int write = wasabiACLEntry.getInsert();
				if (write == -1)
					return -1;
				else if (write == 1)
					result = 1;
				break;
			case WasabiPermission.GRANT:
				int grant = wasabiACLEntry.getInsert();
				if (grant == -1)
					return -1;
				else if (grant == 1)
					result = 1;
				break;
			}

		}
		return result;
	}

	private static boolean priorityCheck2(String objectUUID, String userUUID, int permission)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		long time = java.lang.System.currentTimeMillis();

		try {
			String getACLEntries = "SELECT * FROM wasabi_rights WHERE `object_id`=?";
			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getACLEntries, h, objectUUID);

			// Filter and check explicit user time rights
			List<WasabiACLEntry> explicitUserTimeRights = new Vector<WasabiACLEntry>();

			for (WasabiACLEntry wasabiACLEntry : result) {
				if (wasabiACLEntry.getUser_Id().equals(userUUID) && wasabiACLEntry.getInheritance_Id().equals("")
						&& wasabiACLEntry.getStart_Time() <= time && wasabiACLEntry.getEnd_Time() >= time) {
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
					if (wasabiACLEntry.getUser_Id().equals(userUUID) && !wasabiACLEntry.getInheritance_Id().equals("")
							&& wasabiACLEntry.getStart_Time() <= time && wasabiACLEntry.getEnd_Time() >= time) {
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
						if (wasabiACLEntry.getUser_Id().equals(userUUID)
								&& wasabiACLEntry.getInheritance_Id().equals("") && wasabiACLEntry.getStart_Time() <= 0
								&& wasabiACLEntry.getEnd_Time() >= 0) {
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
							if (wasabiACLEntry.getUser_Id().equals(userUUID)
									&& !wasabiACLEntry.getInheritance_Id().equals("")
									&& wasabiACLEntry.getStart_Time() <= 0 && wasabiACLEntry.getEnd_Time() >= 0) {
								explicitUserRights.add(wasabiACLEntry);
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

	public static boolean authorize(Node objectNode, String callerPrincipal, int[] permission, Session s)
			throws UnexpectedInternalProblemException {
		for (int i = 0; i < permission.length; i++) {
			if (authorize(objectNode, callerPrincipal, permission[i], s))
				return true;
		}
		return false;
	}

	public static boolean authorize(Node objectNode, String callerPrincipal, int permission, Session s)
			throws UnexpectedInternalProblemException {
		try {
			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			String userUUID = UserServiceImpl.getUserByName(callerPrincipal, s).getIdentifier();
			return priorityCheck1(objectUUID, userUUID, permission);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
