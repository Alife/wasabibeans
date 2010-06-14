package de.wasabibeans.framework.server.core.auth;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class SimpleUsrPwdCallbackHandler implements CallbackHandler{

	private String usr;
	private String pwd;

	public SimpleUsrPwdCallbackHandler(String usr, String pwd) {
		this.usr = usr;
		this.pwd = pwd;
	}

	public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback) {
				NameCallback usrCallback = (NameCallback) callback;
				usrCallback.setName(usr);
			} else if (callback instanceof PasswordCallback) {
				PasswordCallback pwdCallback = (PasswordCallback) callback;
				pwdCallback.setPassword(pwd.toCharArray());
			} else {
				throw new UnsupportedCallbackException(callback, "Unsupported Callback");
			}
		}
	}
}
