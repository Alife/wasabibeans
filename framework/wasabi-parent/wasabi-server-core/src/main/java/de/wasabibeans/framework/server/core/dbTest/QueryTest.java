package de.wasabibeans.framework.server.core.dbTest;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

@Stateful
public class QueryTest implements QueryTestRemote,
QueryTestLocal {

	public QueryTest() {
	}

	@SuppressWarnings("unchecked")
	public void doQuery() throws NamingException, SQLException {
		Context context = new InitialContext();

		DataSource dataSource = (DataSource) context.lookup("java:/wasabi");

		QueryRunner run = new QueryRunner(dataSource);
		
//		ResultSetHandler<Object[]> h = new ResultSetHandler<Object[]>() {
//		    public Object[] handle(ResultSet rs) throws SQLException {
//		        if (!rs.next()) {
//		            return null;
//		        }
//		    
//		        ResultSetMetaData meta = rs.getMetaData();
//		        int cols = meta.getColumnCount();
//		        Object[] result = new Object[cols];
//
//		        for (int i = 0; i < cols; i++) {
//		            result[i] = rs.getObject(i + 1);
//		        }
//
//		        return result;
//		    }
//		};
		
		ResultSetHandler<List<wasabiUsers>> h = new BeanListHandler(wasabiUsers.class);
		
		List<wasabiUsers> result = run.query(
			    "SELECT * FROM test WHERE f1=?", h, "out");
		
		result.get(0).getF1();
		result.get(0).getF2();
		result.get(0).getF3();
		result.get(1).getF1();
		result.get(1).getF2();
		result.get(1).getF3();
	}
}
