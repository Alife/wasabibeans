package de.wasabibeans.framework.server.core.auth;

import java.security.Principal;

public class UsernamePasswordPrincipal implements Principal, java.io.Serializable {

	private static final long serialVersionUID = -6659927928185347112L;

	private String name;

	public UsernamePasswordPrincipal(String name) {
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null.");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return ("NamePasswordPrincipal:  " + name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UsernamePasswordPrincipal))
			return false;
		UsernamePasswordPrincipal other = (UsernamePasswordPrincipal) obj;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
