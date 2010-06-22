/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

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

		String dropWasabiUserTableQuery = "DROP TABLE IF EXISTS wasabi_user";
		String createWasabiUserTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_user ("
				+ "`username` varchar(255) NOT NULL," + "`password` varchar(64) NOT NULL," + "PRIMARY KEY (username));";
		try {
			run.update(dropWasabiUserTableQuery);
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
