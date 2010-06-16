package de.wasabibeans.framework.server.core.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import de.wasabibeans.framework.server.core.common.WasabiConstants;

public class SqlConnector {

	public static DataSource connect() {
		try {
			Context context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup(WasabiConstants.JDNI_SQL_DATASOURCE);
			return dataSource;
		} catch (NamingException e) {
			return null;
		}
	}
}
