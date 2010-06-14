package de.wasabibeans.framework.server.core.auth;

import java.util.Arrays;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class UsernamePasswordLoginModule implements LoginModule {

	private Subject subject;

	private CallbackHandler callbackHandler;

	private boolean succeeded = false;

	private boolean commitSucceeded = false;

	private String username;

	private char[] password;

	private UsernamePasswordPrincipal userPrincipal;

	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<java.lang.String, ?> sharedState,
			Map<java.lang.String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

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
		// TODO: Do the authentication against our internal database.
		boolean usernameCorrect = username.equals("root");
		boolean passwordCorrect = Arrays.equals("meerrettich".toCharArray(),
				password);
		//usernameCorrect = true;
		//passwordCorrect = true;
		if (usernameCorrect && passwordCorrect) {
			succeeded = true;
			return true;
		} else {
			succeeded = false;
			username = null;
			password = null;
			if (!usernameCorrect && !passwordCorrect) {
				throw new FailedLoginException(
						"Username and password incorrect");
			} else if (!usernameCorrect) {
				throw new FailedLoginException("Username incorrect");
			} else {
				throw new FailedLoginException("Password incorrect");
			}
		}
	}

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
