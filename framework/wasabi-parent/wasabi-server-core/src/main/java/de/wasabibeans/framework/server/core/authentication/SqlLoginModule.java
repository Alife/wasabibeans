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

package de.wasabibeans.framework.server.core.authentication;

import java.security.acl.Group;
import java.sql.SQLException;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiUserEntry;

public class SqlLoginModule extends UsernamePasswordLoginModule {

	@Override
	protected boolean validatePassword(String inputPassword, String expectedPassword) {
		if (inputPassword == null || expectedPassword == null) {
			return false;
		}
		return expectedPassword.equals(convertRawPassword(inputPassword));
	}

	private String convertRawPassword(String rawPassword) {
		return HashGenerator.generateHash(rawPassword, hashAlgorithms.SHA);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getUsersPassword() throws LoginException {
		try {
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
			ResultSetHandler<List<WasabiUserEntry>> h = new BeanListHandler(WasabiUserEntry.class);
			List<WasabiUserEntry> result = run.query(WasabiConstants.SQL_LOGIN_MODULE_QUERY, h, getUsername());

			if (result.size() > 1) {
				return null;
			} else {
				return result.get(0).getPassword();
			}
		} catch (SQLException ex) {
			throw new LoginException(ex.toString());
		}
	}

	@Override
	protected Group[] getRoleSets() throws LoginException {
		return new Group[0];
	}
}
