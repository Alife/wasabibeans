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

package de.wasabibeans.framework.server.core.dbTest;

import java.sql.SQLException;

import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;

@Stateful
public class ConnectionTestDB implements ConnectionTestDBRemote, ConnectionTestDBLocal {

	/**
	 * Default constructor.
	 */
	public ConnectionTestDB() {
		// TODO Auto-generated constructor stub
	}

	public void createDatabase() throws SQLException, NamingException {

		Context context = new InitialContext();

		DataSource dataSource = (DataSource) context.lookup("java:/wasabi");

		QueryRunner run = new QueryRunner(dataSource);

		String createAclTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_acl_entries (" + "`id` varchar(255) NOT NULL, "
				+ "`pid` varchar(255) NOT NULL, " + "`view` smallint(2) NOT NULL, " + "`read` smallint(2) NOT NULL, "
				+ "`execute` smallint(2) NOT NULL, " + "`comment` smallint(2) NOT NULL, "
				+ "`insert` smallint(2) NOT NULL, " + "`write` smallint(2) NOT NULL, "
				+ "`grant` smallint(2) NOT NULL, " + "`end_time` float NOT NULL, " + "`start_time` float NOT NULL, "
				+ "PRIMARY KEY (id));";

		run.update(createAclTableQuery);
	}
}
