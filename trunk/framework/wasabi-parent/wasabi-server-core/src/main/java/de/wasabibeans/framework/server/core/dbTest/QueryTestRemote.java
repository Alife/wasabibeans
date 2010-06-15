package de.wasabibeans.framework.server.core.dbTest;

import java.sql.SQLException;

import javax.ejb.Remote;
import javax.naming.NamingException;

@Remote
public interface QueryTestRemote {
	public void doQuery() throws SQLException, NamingException;
}
