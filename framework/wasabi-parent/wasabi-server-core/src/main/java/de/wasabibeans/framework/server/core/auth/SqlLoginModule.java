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

import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;
import de.wasabibeans.framework.server.core.util.HashGenerator;
import de.wasabibeans.framework.server.core.util.WasabiUser;

public class SqlLoginModule implements LoginModule {

	private CallbackHandler callbackHandler;
	private Subject subject;
	private String dsJndiName;
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
		dsJndiName = (String) options.get("dsJndiName");
		if (dsJndiName == null)
			dsJndiName = "java:/wasabi";
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
			throw new FailedLoginException("Username or Password incorrect");
		}

	}

	private String convertRawPassword(String rawPassword) {
		return HashGenerator.generateHash(rawPassword, hashAlgorithms.SHA);
	}

	@SuppressWarnings("unchecked")
	protected String getUsersPassword(String username) throws LoginException {

		try {
			//TODO: dsJndiName in eine globale Konfigdatei auslagern, Query auch, übergabe per put in der WasabiConnection. Initialisierung nicht vergessen
			Context context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup("java:/wasabi");
			QueryRunner run = new QueryRunner(dataSource);

			ResultSetHandler<List<WasabiUser>> h = new BeanListHandler(WasabiUser.class);

			List<WasabiUser> result = run.query("SELECT password FROM wasabi_user WHERE username=?", h, username);

			if (result.size() > 1)
				return null;
			else
				return result.get(0).getPassword();
		} catch (NamingException ex) {
			throw new LoginException(ex.toString(true));
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
