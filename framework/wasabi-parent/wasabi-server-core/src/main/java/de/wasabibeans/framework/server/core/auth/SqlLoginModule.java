/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.auth;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiUser;

public class SqlLoginModule implements LoginModule {

	private CallbackHandler callbackHandler;
	private Subject subject;
	private String username;
	private char[] password;
	private UsernamePasswordPrincipal userPrincipal;

	private boolean succeeded = false;
	private boolean commitSucceeded = false;

	@Override
	public boolean abort() throws LoginException {
		if (succeeded == false) {
			return false;
		} else if (succeeded == true && commitSucceeded == false) {
			succeeded = false;
			username = null;
			password = null;
			userPrincipal = null;
		} else {
			logout();
		}
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		if (succeeded == false) {
			return false;
		} else {
			userPrincipal = new UsernamePasswordPrincipal(username);
			if (!subject.getPrincipals().contains(userPrincipal))
				subject.getPrincipals().add(userPrincipal);
			username = null;
			password = null;
			commitSucceeded = true;
			return true;
		}
	}

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.callbackHandler = callbackHandler;
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("username: ");
		callbacks[1] = new PasswordCallback("password: ", false);
		try {
			callbackHandler.handle(callbacks);
			username = ((NameCallback) callbacks[0]).getName();
			password = ((PasswordCallback) callbacks[1]).getPassword();
			if (password == null) {
				password = new char[0];
			}
			((PasswordCallback) callbacks[1]).clearPassword();
		} catch (java.io.IOException ioe) {
			throw new LoginException(ioe.getMessage());
		} catch (UnsupportedCallbackException uce) {
			throw new LoginException(uce.getMessage());
		}

		boolean passwordCorrect = Arrays.equals(getUsersPassword(username).toCharArray(), convertRawPassword(
				String.copyValueOf(password)).toCharArray());

		if (passwordCorrect) {
			succeeded = true;
			return true;
		} else {
			throw new FailedLoginException(WasabiExceptionMessages.SQL_LOGIN_MODULE_LOGIN_FAILURE);
		}

	}

	private String convertRawPassword(String rawPassword) {
		return HashGenerator.generateHash(rawPassword, hashAlgorithms.SHA);
	}

	@SuppressWarnings("unchecked")
	protected String getUsersPassword(String username) throws LoginException {

		try {
			QueryRunner run = new QueryRunner(SqlConnector.connect());

			ResultSetHandler<List<WasabiUser>> h = new BeanListHandler(WasabiUser.class);

			List<WasabiUser> result = run.query(WasabiConstants.SQL_LOGIN_MODULE_QUERY, h, username);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (SQLException ex) {
			throw new LoginException(ex.toString());
		}
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().remove(userPrincipal);
		succeeded = false;
		succeeded = commitSucceeded;
		username = null;
		password = null;
		userPrincipal = null;
		return true;
	}
}