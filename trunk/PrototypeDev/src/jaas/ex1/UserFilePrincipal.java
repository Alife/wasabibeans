package jaas.ex1;


import java.security.Principal;

public class UserFilePrincipal implements Principal {

	private String name;

	public UserFilePrincipal(String name) {
		if (name == null)
			throw new NullPointerException("prinicpal name must not be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return ("UserFilePrincipal: " + name);
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof UserFilePrincipal))
			return false;
		UserFilePrincipal fp = (UserFilePrincipal) o;
		return this.name.equals(fp.name);
	}

	public int hashCode() {
		return name.hashCode();
	}

}
