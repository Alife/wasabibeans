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

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

public class WasabiRoomSQL {

	public static String[] SQLQueryForMove(String roomUUID) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String getInheritanceEntries = "SELECT `inheritance_id` FROM `wasabi_rights` "
				+ "WHERE `object_id`=? AND `inheritance_id`!=''";
		try {
			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> results = run.query(getInheritanceEntries, h, roomUUID);

			String[] result = new String[results.size()];
			int i = 0;

			for (WasabiACLEntry wasabiACLEntry : results) {
				result[i] = wasabiACLEntry.getInheritance_Id();
				i++;
			}

			return result;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static void SQLQueryForRemove(String roomUUID) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String deleteEntryQuery = "DELETE FROM `wasabi_template_rights` WHERE `location_id`=?";
		try {
			run.update(deleteEntryQuery, roomUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static void createRandomSQLinserts() throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
				+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`, `wasabi_type`)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try {
			for (int i = 0; i < randNr(8000); i++)
				run.update(insertUserACLEntryQuery, randNr(1000000), randNr(1000000), randNr(1000000), "", randNr(1),
						randNr(1), randNr(1), randNr(1), randNr(1), randNr(1), randNr(1), randNr(1), randNr(1),
						randNr(1000000), randNr(8), "ROOM");
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

	}

	private static int randNr(int n) {
		double decNr = Math.random();
		return (int) Math.round(decNr * n);
	}
}
