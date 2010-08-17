package de.wasabibeans.framework.server.core.test.performance;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.WasabiException;
import de.wasabibeans.framework.server.core.test.remote.WasabiRemoteTest;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class PriorityCheckComparison extends WasabiRemoteTest {

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

	@Test
	public void createTest() throws WasabiException, SQLException, ClassNotFoundException {

//		final String dbDrv = "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
//		final String dbUrl = "jdbc:mysql://localhost:3306/wasabibeans";
//		final String dbUsr = "wasabi";
//		final String dbPwd = "wasabi";
//		final String dbTyp = "ENGINE=InnoDB";
//		final String dbTbl = "MeineTestTabelle1";
//
//		Connection cn = null;
//		Statement st = null;
//		try {
//			Class.forName(dbDrv);
//			cn = (Connection) DriverManager.getConnection(dbUrl, dbUsr, dbPwd);
//			cn.setAutoCommit(false);
//			System.out.println("AutoCommit=" + cn.getAutoCommit() + ", TransactionIsolation="
//					+ cn.getTransactionIsolation());
//
//		} finally {
//			try {
//				if (null != st)
//					st.close();
//			} catch (Exception ex) {
//			}
//			try {
//				if (null != cn)
//					cn.close();
//			} catch (Exception ex) {
//			}
//		}

		// Create user
		WasabiUserDTO user = userService().create("testUser", "password");
		WasabiUserDTO loginUser = userService().getUserByName("user");

		// Create document in users homeRoom and set rights to view, read document
		WasabiRoomDTO usersHome = userService().getHomeRoom(user);
		int[] rights = { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.WRITE,
				WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.GRANT };
		boolean[] allow = { true, true, true, true, true, true, true };
		aclService().create(usersHome, loginUser, rights, allow);

		long start = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++) {
			roomService().create("room" + i, usersHome);
			// System.out.println("Raum " + i + " erstellt.");
		}

		long end = System.currentTimeMillis();

		// list nodes
		long start1 = System.currentTimeMillis();

		Vector<WasabiRoomDTO> rooms = roomService().getRooms(usersHome);
//		for (WasabiRoomDTO wasabiRoomDTO : rooms) {
//			roomService().getName(wasabiRoomDTO);
//		}

		long end1 = System.currentTimeMillis();

		System.out.println("Runtime create: " + (end - start));
		System.out.println("Runtime getRooms: " + (end1 - start1));

	}
}
