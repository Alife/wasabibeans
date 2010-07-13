package de.wasabibeans.framework.server.core.internal;

import java.sql.SQLException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.dbutils.QueryRunner;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;

public class ACLServiceImpl {

	public static void create(Node wasabiObjectNode, Node wasabiIdentityNode, int[] permission, boolean[] allowance,
			long startTime, long endTime) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		try {
			int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;
			String wasabiIdentityType = wasabiIdentityNode.getPrimaryNodeType().getName();
			String identityUUID = ObjectServiceImpl.getUUID(wasabiIdentityNode);
			String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
			String parentUUID = ObjectServiceImpl.getUUID(wasabiObjectNode.getParent());

			for (int i = 0; i < permission.length; i++) {
				switch (permission[i]) {
				case WasabiPermission.VIEW:
					view = allowanceConverter(allowance[i]);
				case WasabiPermission.READ:
					read = allowanceConverter(allowance[i]);
				case WasabiPermission.INSERT:
					insert = allowanceConverter(allowance[i]);
				case WasabiPermission.WRITE:
					write = allowanceConverter(allowance[i]);
				case WasabiPermission.EXECUTE:
					execute = allowanceConverter(allowance[i]);
				case WasabiPermission.COMMENT:
					comment = allowanceConverter(allowance[i]);
				case WasabiPermission.GRANT:
					grant = allowanceConverter(allowance[i]);
				}
			}

			if (wasabiIdentityType.equals(WasabiNodeType.WASABI_USER)) {
				try {
					String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
							+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`)"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
					run.update(insertUserACLEntryQuery, objectUUID, identityUUID, parentUUID, "", view, read, insert,
							write, execute, comment, grant, startTime, endTime);
				} catch (SQLException e) {
					String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
						+ "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
						+ " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=?";
					try {
						run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment, grant,
								objectUUID, identityUUID, startTime, endTime);
					} catch (SQLException ex) {
						throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, ex);
					}
				}
			} else if (wasabiIdentityType.equals(WasabiNodeType.WASABI_GROUP)) {
				try {
					String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
							+ "view=?, read=?, insert=?, write=?, execute=?, comment=?, grant=?"
							+ " WHERE object_id=? AND _id=? AND start_time=? AND end_time=?";
					run.update(updateUserACLEntryQuery, view, read, insert, write, execute, comment, grant, objectUUID,
							identityUUID, startTime, endTime);
				} catch (SQLException e) {
					String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
							+ "(object_id, group_id, parent_id, view, read, insert, write, execute, comment, grant, start_time, end_time)"
							+ " VALUES (?,?)";
					try {
						run.update(insertUserACLEntryQuery, objectUUID, identityUUID, parentUUID, view, read, insert,
								write, execute, comment, grant, startTime, endTime);
					} catch (SQLException ex) {
						throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, ex);
					}
				}
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static int allowanceConverter(boolean allowance) {
		if (allowance = true)
			return 1;
		else if (allowance = false)
			return -1;
		else
			return 0;
	}
}
