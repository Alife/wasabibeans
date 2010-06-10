package de.wasabibeans.framework.server.core.dbTest;

import java.sql.SQLException;

import javax.ejb.Remote;
import javax.naming.NamingException;

@Remote
public interface ConnectionTestDBRemote {
	public void createDatabase() throws SQLException, NamingException;
}
