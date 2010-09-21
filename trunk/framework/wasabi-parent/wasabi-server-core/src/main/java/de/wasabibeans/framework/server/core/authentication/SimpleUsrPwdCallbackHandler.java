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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class SimpleUsrPwdCallbackHandler implements CallbackHandler {

	private String pwd;
	private String usr;

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
