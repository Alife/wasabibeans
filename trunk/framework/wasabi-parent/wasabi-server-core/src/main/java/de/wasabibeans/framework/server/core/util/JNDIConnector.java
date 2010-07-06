package de.wasabibeans.framework.server.core.util;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class JNDIConnector {

	private InitialContext jndiContext;
	private WasabiLogger logger;

	public static JNDIConnector getJNDIConnector() {
		return new JNDIConnector();
	}

	public JNDIConnector() {
		this.logger = WasabiLogger.getLogger(this.getClass());
	}

	public InitialContext getJNDIContext() throws UnexpectedInternalProblemException {
		try {
			if (this.jndiContext == null) {
				this.jndiContext = new InitialContext();
			}
			return this.jndiContext;
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JNDI_NO_CONTEXT, ne);
		}
	}

	public Object localLookup(String name) throws UnexpectedInternalProblemException {
		try {
			return getJNDIContext().lookup(name + "/local");
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_LOOKUP, name), ne);
		}
	}

	public Object lookup(String name) throws UnexpectedInternalProblemException {
		try {
			return getJNDIContext().lookup(name);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_LOOKUP, name), ne);
		}
	}

	public void bind(String name, Object o) throws UnexpectedInternalProblemException {
		try {
			getJNDIContext().bind(name, o);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_BIND, name), ne);
		}
	}

	public void unbind(String name) throws UnexpectedInternalProblemException {
		try {
			getJNDIContext().unbind(name);
		} catch (NameNotFoundException nnfe) {
			logger.info(WasabiExceptionMessages.get(WasabiExceptionMessages.JNDI_FAILED_UNBIND_NAME_NOT_BOUND, name),
					nnfe);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_UNBIND, name), ne);
		}
	}
}
