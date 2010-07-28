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

package de.wasabibeans.framework.server.core.internal;

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
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;

public class ACLServiceImpl {

	private static int[] allowanceConverter(boolean[] allowance) {
		int[] allow = new int[allowance.length];

		for (int i = 0; i < allow.length; i++) {
			if (allowance[i] = true)
				allow[i] = 1;
			else if (allowance[i] = false)
				allow[i] = -1;
			else
				allow[i] = 0;
		}

		return allow;
	}

	public static void create(Node wasabiObjectNode, Node wasabiIdentityNode, int[] permission, boolean[] allowance,
			long startTime, long endTime) throws UnexpectedInternalProblemException {
		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		try {
			updateRights(wasabiObjectNode, wasabiIdentityNode, permission, allowanceConverter(allowance), startTime,
					endTime, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Deprecated
	public static Vector<WasabiACLEntryDeprecated> getACLEntriesDeprecated(Node wasabiObjectNode,
			Node wasabiIdentityNode, Session s) throws UnexpectedInternalProblemException {

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;
		long id;
		String wasabiGroupUUID = "", wasabiUserUUID = "";
		Node wasabiIdentity;
		List<WasabiACLEntry> wasabiACLEntry;

		Vector<WasabiACLEntryDeprecated> aclEntries = new Vector<WasabiACLEntryDeprecated>();
		if (wasabiIdentityNode == null) {
			wasabiACLEntry = getAclEntries(wasabiObjectNode);
		} else {
			wasabiACLEntry = getAclEntriesByIdentity(wasabiObjectNode, wasabiIdentityNode);
		}

		for (int i = 0; i < wasabiACLEntry.size(); i++) {

			id = wasabiACLEntry.get(i).getId();
			view = wasabiACLEntry.get(i).getView();
			read = wasabiACLEntry.get(i).getRead();
			insert = wasabiACLEntry.get(i).getInsert();
			write = wasabiACLEntry.get(i).getWrite();
			execute = wasabiACLEntry.get(i).getExecute();
			comment = wasabiACLEntry.get(i).getComment();
			grant = wasabiACLEntry.get(i).getGrant();
			wasabiGroupUUID = wasabiACLEntry.get(i).getGroup_ID();
			wasabiUserUUID = wasabiACLEntry.get(i).getUser_ID();

			if (wasabiGroupUUID.length() > 0) {
				try {
					wasabiIdentity = s.getNodeByIdentifier(wasabiGroupUUID);
				} catch (RepositoryException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
				}
			} else {
				try {
					wasabiIdentity = s.getNodeByIdentifier(wasabiUserUUID);
				} catch (RepositoryException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
				}
			}

			WasabiIdentityDTO wasabiIdentityDTO = TransferManager.convertNode2DTO(wasabiIdentity);

			if (view == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.VIEW);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (view == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.VIEW);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (read == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.READ);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (read == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.READ);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}
			if (execute == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.EXECUTE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (execute == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.EXECUTE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (write == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.WRITE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (write == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.WRITE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (insert == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.INSERT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (insert == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.INSERT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (comment == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.COMMENT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (comment == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.COMMENT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (grant == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.GRANT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (grant == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.GRANT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			}
		}
		return aclEntries;

	}

	public static List<WasabiACLEntry> getAclEntries(Node wasabiObjectNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);

		String getACLEntriesQuery = "SELECT * FROM wasabi_rights WHERE `object_id`=?";

		ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
		try {
			List<WasabiACLEntry> result = run.query(getACLEntriesQuery, h, objectUUID);
			return result;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static List<WasabiACLEntry> getAclEntriesByIdentity(Node wasabiObjectNode, Node wasabiIdentityNode)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
		String identityUUID = ObjectServiceImpl.getUUID(wasabiIdentityNode);

		String getACLEntriesByIdentityQuery = "SELECT * FROM wasabi_rights "
				+ "WHERE object_id=? AND (group_id=? OR user_id=?)";

		ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
		try {
			List<WasabiACLEntry> result = run.query(getACLEntriesByIdentityQuery, h, objectUUID, identityUUID,
					identityUUID);
			return result;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static boolean getInheritance(Node wasabiObjectNode) throws UnexpectedInternalProblemException {
		if (wasabiObjectNode == null)
			return false;

		try {
			if (wasabiObjectNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
				return true;
			else
				return false;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void reset(Node wasabiObjectNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
		String deleteRight = "DELETE FROM wasabi_rights WHERE object_id=?";
		try {
			run.update(deleteRight, objectUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static void remove(Node wasabiObjectNode, Node wasabiIdentityNode, int[] permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException {
		int[] allowance = new int[permission.length];

		for (int i = 0; i < allowance.length; i++)
			allowance[i] = 0;

		try {
			updateRights(wasabiObjectNode, wasabiIdentityNode, permission, allowance, startTime, endTime, false);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setInheritance(Node wasabiObjectNode, boolean inheritance)
			throws UnexpectedInternalProblemException {
		try {
			wasabiObjectNode.setProperty(WasabiNodeProperty.INHERITANCE, inheritance);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static void updateRights(Node wasabiObjectNode, Node wasabiIdentityNode, int[] permission, int[] allowance,
			long startTime, long endTime, boolean inheritance) throws RepositoryException,
			UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
		String wasabiIdentityType = wasabiIdentityNode.getPrimaryNodeType().getName();
		String identityUUID = ObjectServiceImpl.getUUID(wasabiIdentityNode);
		String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(wasabiObjectNode));

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0, ihtc = 0;
		if (inheritance)
			ihtc = 1;

		if (wasabiIdentityType.equals(WasabiNodeType.WASABI_USER)) {
			try {
				String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
						+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance`=?";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
						identityUUID, ihtc);

				if (!result.isEmpty()) {
					view = result.get(0).getView();
					read = result.get(0).getRead();
					insert = result.get(0).getInsert();
					write = result.get(0).getWrite();
					execute = result.get(0).getExecute();
					comment = result.get(0).getComment();
					grant = result.get(0).getGrant();

					for (int i = 0; i < permission.length; i++) {
						switch (permission[i]) {
						case WasabiPermission.VIEW:
							view = allowance[i];
							break;
						case WasabiPermission.READ:
							read = allowance[i];
							break;
						case WasabiPermission.INSERT:
							insert = allowance[i];
							break;
						case WasabiPermission.WRITE:
							write = allowance[i];
							break;
						case WasabiPermission.EXECUTE:
							execute = allowance[i];
							break;
						case WasabiPermission.COMMENT:
							comment = allowance[i];
							break;
						case WasabiPermission.GRANT:
							grant = allowance[i];
							break;
						}
					}

					if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
							&& grant == 0) {
						String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID, ihtc);
					} else {
						String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
								+ "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
								+ " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance`=?";
						run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment,
								grant, objectUUID, identityUUID, startTime, endTime, ihtc);
					}
				} else {
					for (int i = 0; i < permission.length; i++) {
						switch (permission[i]) {
						case WasabiPermission.VIEW:
							view = allowance[i];
							break;
						case WasabiPermission.READ:
							read = allowance[i];
							break;
						case WasabiPermission.INSERT:
							insert = allowance[i];
							break;
						case WasabiPermission.WRITE:
							write = allowance[i];
							break;
						case WasabiPermission.EXECUTE:
							execute = allowance[i];
							break;
						case WasabiPermission.COMMENT:
							comment = allowance[i];
							break;
						case WasabiPermission.GRANT:
							grant = allowance[i];
							break;
						}
					}

					if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
							&& grant == 0) {
						String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID, ihtc);
					} else {
						String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
								+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance`)"
								+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

						run.update(insertUserACLEntryQuery, objectUUID, identityUUID, parentUUID, "", view, read,
								insert, write, execute, comment, grant, startTime, endTime, ihtc);
					}
				}
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		} else if (wasabiIdentityType.equals(WasabiNodeType.WASABI_GROUP)) {
			try {
				String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
						+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance`=?";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
						identityUUID, ihtc);

				if (!result.isEmpty()) {
					view = result.get(0).getView();
					read = result.get(0).getRead();
					insert = result.get(0).getInsert();
					write = result.get(0).getWrite();
					execute = result.get(0).getExecute();
					comment = result.get(0).getComment();
					grant = result.get(0).getGrant();

					for (int i = 0; i < permission.length; i++) {
						switch (permission[i]) {
						case WasabiPermission.VIEW:
							view = allowance[i];
							break;
						case WasabiPermission.READ:
							read = allowance[i];
							break;
						case WasabiPermission.INSERT:
							insert = allowance[i];
							break;
						case WasabiPermission.WRITE:
							write = allowance[i];
							break;
						case WasabiPermission.EXECUTE:
							execute = allowance[i];
							break;
						case WasabiPermission.COMMENT:
							comment = allowance[i];
							break;
						case WasabiPermission.GRANT:
							grant = allowance[i];
							break;
						}
					}

					if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
							&& grant == 0) {
						String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID, ihtc);
					} else {
						String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
								+ "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
								+ " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance`=?";
						run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment,
								grant, objectUUID, identityUUID, startTime, endTime, ihtc);
					}
				} else {
					for (int i = 0; i < permission.length; i++) {
						switch (permission[i]) {
						case WasabiPermission.VIEW:
							view = allowance[i];
							break;
						case WasabiPermission.READ:
							read = allowance[i];
							break;
						case WasabiPermission.INSERT:
							insert = allowance[i];
							break;
						case WasabiPermission.WRITE:
							write = allowance[i];
							break;
						case WasabiPermission.EXECUTE:
							execute = allowance[i];
							break;
						case WasabiPermission.COMMENT:
							comment = allowance[i];
							break;
						case WasabiPermission.GRANT:
							grant = allowance[i];
							break;
						}
					}

					if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
							&& grant == 0) {
						String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID, ihtc);
					} else {
						String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
								+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance`)"
								+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

						run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, identityUUID, view, read,
								insert, write, execute, comment, grant, startTime, endTime, ihtc);
					}
				}
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		}
	}
}
