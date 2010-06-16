package de.wasabibeans.framework.server.core.util;

public class WasabiUser {
	private String password;
	private String username;

	public WasabiUser() {
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return this.username;
	}
}
