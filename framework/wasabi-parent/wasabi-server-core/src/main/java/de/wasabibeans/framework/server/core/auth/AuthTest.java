package de.wasabibeans.framework.server.core.auth;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@SecurityDomain("wasabi")
@Stateless(name="AuthTest")
public class AuthTest implements AuthTestLocal, AuthTestRemote {

	@Override
	public String HelloWorld() {
		return "Hello World";
	}

}
