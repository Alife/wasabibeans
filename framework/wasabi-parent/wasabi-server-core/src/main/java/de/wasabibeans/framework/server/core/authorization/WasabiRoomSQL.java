package de.wasabibeans.framework.server.core.authorization;

import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;

public class WasabiRoomSQL {

	public static void SQLQueryForRemove(String roomUUID) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String deleteEntryQuery = "DELETE FROM wasabi_template_rights WHERE `location_id`=?";
		try {
			run.update(deleteEntryQuery, roomUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}
}
