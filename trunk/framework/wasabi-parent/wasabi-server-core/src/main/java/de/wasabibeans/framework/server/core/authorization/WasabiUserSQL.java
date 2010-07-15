package de.wasabibeans.framework.server.core.authorization;

import java.sql.SQLException;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiUserEntry;

public class WasabiUserSQL {
	
	public static void SqlQueryForCreate(String name, String password) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String insertUserQuery = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

		try {
			run.update(insertUserQuery, name, passwordCrypt);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}
	
	public static void SqlQueryForRemove(Node userNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String removeUserQuery = "DELETE FROM wasabi_user WHERE username=?";

		try {
			run.update(removeUserQuery, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}
	
	public static void SqlQueryForRename(Node userNode, String name) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String renameUserQuery = "UPDATE wasabi_user SET username=? WHERE username=?";

		try {
			run.update(renameUserQuery, name, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}
	
	public static String SqlQueryForGetPassword(Node userNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String getPasswordQuery = "SELECT password FROM wasabi_user WHERE username=?";
		try {
			ResultSetHandler<List<WasabiUserEntry>> h = new BeanListHandler<WasabiUserEntry>(WasabiUserEntry.class);

			List<WasabiUserEntry> result = run.query(getPasswordQuery, h, wasabiUser);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}
	
	public static void SqlQueryForSetPassword(Node userNode, String password) throws UnexpectedInternalProblemException{
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String wasabiUser = ObjectServiceImpl.getName(userNode);
		String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
		String setPasswordQuery = "UPDATE wasabi_user SET password=? WHERE username=?";

		try {
			run.update(setPasswordQuery, passwordCrypt, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_USER, e);
		}
	}
}
