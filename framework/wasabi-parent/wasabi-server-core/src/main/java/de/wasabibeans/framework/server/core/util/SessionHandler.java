package de.wasabibeans.framework.server.core.util;

import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.SessionContext;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class SessionHandler {

	// logger
	private static WasabiLogger logger = WasabiLogger.getLogger(SessionHandler.class);
	
	// class variables
	private static Session baseSession;
	private static ConcurrentHashMap<String, Session> sessionStore;
	
	// instance variables
	private JndiConnector jndi;
	private JcrConnector jcr;
	private Session currentSession;

	public SessionHandler() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(this.jndi);
	}

	//--- instance methods -----------------------------------------------------------
	public Session getSession(SessionContext ctx) throws UnexpectedInternalProblemException {
		if (currentSession == null) {
			String username = ctx.getCallerPrincipal().getName();
			logger.debug("A new JCR session will be created for user " + username + ".");
			currentSession = jcr.getJCRSession(new SimpleCredentials(username, username.toCharArray()));
		}
		return currentSession;
	}

	public void releaseSession(SessionContext ctx) {
		currentSession.logout();
		currentSession = null;
	}

	public void storeJCRSession(String key, Session session) {
		sessionStore.put(key, session);
	}

	public void removeJCRSession(String key) {
		sessionStore.remove(key);
	}

	public Session getJCRSession(String key) {
		return sessionStore.get(key);
	}

	//--- class methods -----------------------------------------------------------
	public static synchronized Session getBaseSession(JcrConnector jcr) throws UnexpectedInternalProblemException {
		if (baseSession == null) {
			logger.debug("A new base session will be created.");
			baseSession = jcr.getJCRSession(new SimpleCredentials("base", "base".toCharArray()));
		}
		return baseSession;
	}
	
	public static synchronized void releaseBaseSession() {
		if (baseSession != null) {
			logger.debug("The base session will be logged out.");
			baseSession.logout();
			baseSession = null;
		}
	}
	
	
}
