package de.wasabibeans.framework.server.core.manager;

import java.sql.SQLException;

import javax.ejb.Stateless;

import org.apache.commons.dbutils.QueryRunner;

import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;

@Stateless(name = "WasabiManager")
public class WasabiManager implements WasabiManagerLocal {

	public final static String rootUserName = "root";
	public final static String rootUserPassword = "meerrettich";

	@Override
	public void init() {

		/**
		 * Create user database and entries
		 */
		QueryRunner run = new QueryRunner(SqlConnector.connect());

		String createWasabiUserTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_user ("
				+ "`username` varchar(255) NOT NULL," + "`password` varchar(64) NOT NULL" + "PRIMARY KEY (username));";
		try {
			run.update(createWasabiUserTableQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String insertWasabiRootUser = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

		try {
			run.update(insertWasabiRootUser, rootUserName, HashGenerator.generateHash(rootUserPassword,
					hashAlgorithms.SHA));
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}
