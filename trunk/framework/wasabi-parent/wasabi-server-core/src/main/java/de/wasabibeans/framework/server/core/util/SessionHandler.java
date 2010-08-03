package de.wasabibeans.framework.server.core.util;

import javax.ejb.SessionContext;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class SessionHandler {

	// logger
	private static WasabiLogger logger = WasabiLogger.getLogger(SessionHandler.class);

	// class variables
	private static Session baseSession;

	// instance variables
	private JndiConnector jndi;
	private JcrConnector jcr;
	private Session currentSession;

	public SessionHandler() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(this.jndi);
	}

	// --- instance methods -----------------------------------------------------------
	public Session getSession(SessionContext ctx) throws UnexpectedInternalProblemException {
		String username = ctx.getCallerPrincipal().getName();
		currentSession = jcr.getJCRSession(new SimpleCredentials(username, username.toCharArray()));
		return currentSession;
	}

	public void releaseSession(SessionContext ctx) {
		System.out.println("tuuuuuuuuuuut");
		// do nothing at the moment -> normally the jca adapter should clean up after a transaction
		// currentSession.logout();
		// currentSession = null;
	}

	// --- class methods -----------------------------------------------------------
	public static synchronized Session getBaseSession(JcrConnector jcr) throws UnexpectedInternalProblemException {
		if (baseSession == null) {
			baseSession = jcr.getJCRSession(new SimpleCredentials("base", "base".toCharArray()));
		} else {
			try {
				if (!baseSession.isLive()) {
					baseSession = jcr.getJCRSession(new SimpleCredentials("base", "base".toCharArray()));
				}
			} catch (IllegalStateException e) { // 'isLive()' threw error -> session is really not live anymore
				baseSession = jcr.getJCRSession(new SimpleCredentials("base", "base".toCharArray()));
			}
		}
		return baseSession;
	}
}
