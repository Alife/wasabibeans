package de.wasabibeans.framework.server.core.dbTest;

import java.sql.SQLException;
import java.util.Properties;

import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;

@Stateful
public class ConnectionTestDB implements ConnectionTestDBRemote,
		ConnectionTestDBLocal {

	/**
	 * Default constructor.
	 */
	public ConnectionTestDB() {
		// TODO Auto-generated constructor stub
	}

	public void createDatabase() throws SQLException, NamingException {

		Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.jnp.interfaces.NamingContextFactory");
		properties.put(Context.URL_PKG_PREFIXES,
				"org.jboss.naming:org.jnp.interfaces");
		properties.put(Context.PROVIDER_URL, "localhost:1099");
		Context context = new InitialContext(properties);

		DataSource dataSource = (DataSource) context.lookup("java:/wasabi");

		QueryRunner run = new QueryRunner(dataSource);

		String createAclTableQuery = "CREATE TABLE IF NOT EXISTS wasabi_acl_entries ("
				+ "`id` varchar(255) NOT NULL, "
				+ "`pid` varchar(255) NOT NULL, "
				+ "`view` smallint(2) NOT NULL, "
				+ "`read` smallint(2) NOT NULL, "
				+ "`execute` smallint(2) NOT NULL, "
				+ "`comment` smallint(2) NOT NULL, "
				+ "`insert` smallint(2) NOT NULL, "
				+ "`write` smallint(2) NOT NULL, "
				+ "`grant` smallint(2) NOT NULL, "
				+ "`end_time` float NOT NULL, "
				+ "`start_time` float NOT NULL, " + "PRIMARY KEY (id));";

		run.update(createAclTableQuery);
	}
}
