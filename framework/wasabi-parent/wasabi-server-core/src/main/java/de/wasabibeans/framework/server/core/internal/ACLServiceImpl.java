package de.wasabibeans.framework.server.core.internal;

import java.sql.SQLException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

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
					endTime);
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
			updateRights(wasabiObjectNode, wasabiIdentityNode, permission, allowance, startTime, endTime);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static void updateRights(Node wasabiObjectNode, Node wasabiIdentityNode, int[] permission, int[] allowance,
			long startTime, long endTime) throws RepositoryException, UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
		String wasabiIdentityType = wasabiIdentityNode.getPrimaryNodeType().getName();
		String identityUUID = ObjectServiceImpl.getUUID(wasabiIdentityNode);
		String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(wasabiObjectNode));

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;

		if (wasabiIdentityType.equals(WasabiNodeType.WASABI_USER)) {
			try {
				String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
						+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=?";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
						identityUUID);

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
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID);
					} else {
						String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
								+ "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
								+ " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=?";
						run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment,
								grant, objectUUID, identityUUID, startTime, endTime);
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
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID);
					} else {
						String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
								+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`)"
								+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

						run.update(insertUserACLEntryQuery, objectUUID, identityUUID, parentUUID, "", view, read,
								insert, write, execute, comment, grant, startTime, endTime);
					}
				}
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		} else if (wasabiIdentityType.equals(WasabiNodeType.WASABI_GROUP)) {
			try {
				String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
						+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=?";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
						identityUUID);

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
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID);
					} else {
						String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
								+ "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
								+ " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=?";
						run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment,
								grant, objectUUID, identityUUID, startTime, endTime);
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
								+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=?";
						run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID);
					} else {
						String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
								+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`)"
								+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

						run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, identityUUID, view, read,
								insert, write, execute, comment, grant, startTime, endTime);
					}
				}
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		}
	}
}
