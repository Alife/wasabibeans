package jaas.ex1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class ConsoleCallbackHandler implements CallbackHandler {

	private String readString() throws IOException {
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof NameCallback) {
				NameCallback nameCallback = (NameCallback) callbacks[i];
				System.out.print(nameCallback.getPrompt());
				System.out.flush();
				nameCallback.setName(readString());
			} else if (callbacks[i] instanceof PasswordCallback) {
				PasswordCallback pwCallback = (PasswordCallback) callbacks[i];
				System.out.print(pwCallback.getPrompt());
				System.out.flush();
				pwCallback.setPassword(readString().toCharArray());
			} else
				throw new UnsupportedCallbackException(callbacks[i]);
		}
	}

}
