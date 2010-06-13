package jaas.ex1;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
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

public class UserFileLoginModule implements LoginModule {
	private String userFileName;
	private Hashtable userTable;

	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map sharedState;
	private Map options;
	private boolean debug = false;

	private String userID;

	private UserFilePrincipal userFilePrincipal;
	private boolean succeeded = false;
	private boolean commitSucceeded = false;

	protected String getDigestAsHexString(byte[] digestBytes) {
		StringBuffer digestString = new StringBuffer();
		for (int i = 0; i < digestBytes.length; i++) {
			String hexString = Integer.toHexString(digestBytes[i]);
			if (hexString.length() == 1)
				hexString = "0" + hexString;
			else if (hexString.length() == 8)
				hexString = hexString.substring(6);
			digestString.append(hexString);
		}
		return digestString.toString();
	}

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = options;
		debug = "true".equalsIgnoreCase((String) options.get("debug"));
		userFileName = (String) options.get("userfile");
		userTable = new Hashtable();
	}

	protected void readUserFile() throws LoginException {
		BufferedReader userFile;

		try {
			userFile = new BufferedReader(new FileReader(userFileName));
			String line;
			if (debug)
				System.out.println("\t\t[UserFileLoginModule] reading user file:");
			while ((line = userFile.readLine()) != null) {
				String[] splittedLine = line.split(":");
				userTable.put(splittedLine[0], splittedLine[1]);
				if (debug)
					System.out.println("\t\t\t" + splittedLine[0] + ":" + splittedLine[1]);
			}
			userFile.close();
		} catch (IOException e) {
			throw new LoginException("Error opening/reading user file.");
		}
	}

	public boolean login() throws LoginException {
		char[] password;

		if (callbackHandler == null)
			throw new LoginException("Error: no CallbackHandler available");

		if (userFileName == null)
			throw new LoginException("Error: no user file specified");
		readUserFile();
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("user name: ");
		callbacks[1] = new PasswordCallback("password: ", false);

		try {
			callbackHandler.handle(callbacks);
			userID = ((NameCallback) callbacks[0]).getName();
			char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
			if (tmpPassword == null) {
				tmpPassword = new char[0];
			}
			password = new char[tmpPassword.length];
			System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
			((PasswordCallback) callbacks[1]).clearPassword();
		} catch (java.io.IOException ioe) {
			throw new LoginException(ioe.toString());
		} catch (UnsupportedCallbackException uce) {
			throw new LoginException("Error: " + uce.getCallback().toString()
					+ " not available to garner authentication information " + "from the user");
		}

		MessageDigest md;
		String pwdHash, calculatedPwdHashStr;
		byte[] calculatedPwdHash;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new LoginException("no SHA-1 implementation found");
		}
		md.update(userID.getBytes());
		md.update(String.valueOf(password).getBytes());
		calculatedPwdHash = md.digest();
		for (int i = 0; i < password.length; i++)
			password[i] = ' ';
		password = null;

		pwdHash = (String) userTable.get(userID);
		if (pwdHash == null) {
			succeeded = false;
		} else {
			calculatedPwdHashStr = getDigestAsHexString(calculatedPwdHash);
			if (debug) {
				System.out.println("\t\t[UserFileLoginModule]");
				System.out.println("\t\t  hash from file : " + pwdHash);
				System.out.println("\t\t  calculated hash: " + calculatedPwdHashStr);
			}
			succeeded = pwdHash.equals(calculatedPwdHashStr);
		}
		if (debug) {
			if (succeeded)
				System.out.println("\t\t[UserFileLoginModule] authentication succeeded");
			else
				System.out.println("\t\t[UserFileLoginModule] authentication failed");
		}
		if (!succeeded)
			throw new FailedLoginException("login failed");

		return true;
	}

	public boolean commit() throws LoginException {
		if (debug)
			System.out.println("\t\t[UserFileLoginModule] commit: " + succeeded);
		if (succeeded == false) {
			return false;
		} else {
			userFilePrincipal = new UserFilePrincipal(userID);
			if (!subject.getPrincipals().contains(userFilePrincipal)) {
				subject.getPrincipals().add(userFilePrincipal);
				if (debug)
					System.out.println("\t\t[UserFileLoginModule] : added principal");
			}

			userID = null;
			commitSucceeded = true;
			return true;
		}
	}

	public boolean abort() throws LoginException {
		if (debug)
			System.out.println("\t\t[UserFileLoginModule] abort: " + succeeded);
		if (succeeded == false) {
			return false;
		} else if (succeeded == true && commitSucceeded == false) {
			succeeded = false;
			userID = null;
			userFilePrincipal = null;
		} else {
			logout();
		}
		return true;
	}

	public boolean logout() throws LoginException {
		subject.getPrincipals().remove(userFilePrincipal);
		if (debug)
			System.out.println("\t\t[UserFileLoginModule] : removed principal");
		succeeded = false;
		succeeded = commitSucceeded;
		userID = null;
		userFilePrincipal = null;
		return true;
	}

}
