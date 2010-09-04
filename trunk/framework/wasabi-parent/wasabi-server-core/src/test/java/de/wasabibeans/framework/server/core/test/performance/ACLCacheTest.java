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

package de.wasabibeans.framework.server.core.test.performance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;

@Run(RunModeType.AS_CLIENT)
public class ACLCacheTest extends WasabiRemoteTest {

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	private static DataSource getDS() {
        MysqlDataSource mySqlDs = new MysqlDataSource();
        mySqlDs.setServerName("localhost");
        mySqlDs.setDatabaseName("wasabibeans");
        mySqlDs.setUser("wasabi");
        mySqlDs.setPassword("wasabi");
        return mySqlDs;
    } 
	
	@Test
	public void compareHashMapAndQuerys() throws Exception {
		QueryRunner run = new QueryRunner(getDS());
		
		HashMap<String, String> hs = new HashMap<String, String>();

		for (int i = 0; i < 1000; i++)
			hs.put(new Integer(i).toString(), new Integer(Numbers(1000)).toString());

		String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
				+ "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		for (int i = 0; i < 1000; i++)
			run.update(insertUserACLEntryQuery, i, Numbers(1000), Numbers(1000), "", 1, 1, 1, 1, 1, 1, 1,
					1, 1, Numbers(1000), 0);

		long startHM1 = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++)
			System.out.println(hs.get(new Integer(Numbers(1000)).toString()));

		long endHM1 = System.currentTimeMillis();

		long startSQL1 = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++) {
			String select = "SELECT * FROM `wasabi_rights` WHERE `object_id`=?";
			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(select, h, new Integer(Numbers(1000)).toString());
		}

		long endSQL1 = System.currentTimeMillis();

		long startHM2 = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++)
			hs.get(new Integer(Numbers(1000)).toString());

		long endHM2 = System.currentTimeMillis();

		long startSQL2 = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++) {
			String select = "SELECT * FROM `wasabi_rights` WHERE `object_id`=?";
			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(select, h, new Integer(Numbers(1000)).toString());
		}

		long endSQL2 = System.currentTimeMillis();

		System.out.println("HashMap1: " + (endHM1 - startHM1));
		System.out.println("HashMap2: " + (endHM2 - startHM2));
		System.out.println("SQL1: " + (endSQL1 - startSQL1));
		System.out.println("SQL1: " + (endSQL2 - startSQL2));
	}

	private static int Numbers(int n) {
		double decnum = Math.random();
		return (int) Math.round(decnum * n);
	}
}
