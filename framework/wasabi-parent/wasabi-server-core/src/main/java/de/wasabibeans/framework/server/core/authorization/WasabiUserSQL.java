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

	public static void removeRights(Node userNode) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String wasabiUserUUID = ObjectServiceImpl.getUUID(userNode);
			String removeUserQuery = "DELETE FROM wasabi_rights WHERE `user`=?";

			run.update(removeUserQuery, wasabiUserUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static void setStatus(Node userNode, boolean status) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			int stat = 0;
			if (status)
				stat = 1;

			String user = ObjectServiceImpl.getName(userNode);
			String setStatusQuery = "UPDATE `wasabi_user` SET `status`=? WHERE `username`=?";

			run.update(setStatusQuery, stat, user);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static void SqlQueryForCreate(String name, String password) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
			String insertUserQuery = "INSERT INTO wasabi_user (username, password) VALUES (?,?)";

			run.update(insertUserQuery, name, passwordCrypt);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static String SqlQueryForGetPassword(Node userNode) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String wasabiUser = ObjectServiceImpl.getName(userNode);
			String getPasswordQuery = "SELECT password FROM wasabi_user WHERE username=?";

			ResultSetHandler<List<WasabiUserEntry>> h = new BeanListHandler<WasabiUserEntry>(WasabiUserEntry.class);

			List<WasabiUserEntry> result = run.query(getPasswordQuery, h, wasabiUser);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static void SqlQueryForRemove(Node userNode) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String wasabiUser = ObjectServiceImpl.getName(userNode);
			String removeUserQuery = "DELETE FROM wasabi_user WHERE username=?";

			run.update(removeUserQuery, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static void SqlQueryForRename(String wasabiUser, String name) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String renameUserQuery = "UPDATE wasabi_user SET username=? WHERE username=?";

			run.update(renameUserQuery, name, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		} finally {
			sqlConnector.close();
		}
	}

	public static void SqlQueryForSetPassword(Node userNode, String password) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			String wasabiUser = ObjectServiceImpl.getName(userNode);
			String passwordCrypt = HashGenerator.generateHash(password, hashAlgorithms.SHA);
			String setPasswordQuery = "UPDATE wasabi_user SET password=? WHERE username=?";

			run.update(setPasswordQuery, passwordCrypt, wasabiUser);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_NO_USER, e);
		} finally {
			sqlConnector.close();
		}
	}
}
