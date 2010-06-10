package de.wasabibeans.framework.server.core.dbTest;

import java.sql.SQLException;

import javax.ejb.Local;
import javax.naming.NamingException;

@Local
public interface ConnectionTestDBLocal {
	public void createDatabase() throws SQLException, NamingException;
}
