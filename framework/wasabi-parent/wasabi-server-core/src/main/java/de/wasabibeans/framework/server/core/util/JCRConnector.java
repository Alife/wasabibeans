package de.wasabibeans.framework.server.core.util;

import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class JCRConnector {

	private Repository jcrRepository;
	private JNDIConnector jndi;

	public static JCRConnector getJCRConnector() {
		return new JCRConnector();
	}

	public JCRConnector() {
		this.jndi = JNDIConnector.getJNDIConnector();
	}

	public Repository getJCRRepository() throws UnexpectedInternalProblemException {
		if (this.jcrRepository == null) {
			this.jcrRepository = (Repository) jndi.localLookup(WasabiConstants.JNDI_JCR_DATASOURCE);
		}
		return this.jcrRepository;
	}

	@SuppressWarnings("unchecked")
	public void storeJCRSession(String username, Session session) throws UnexpectedInternalProblemException {
		ConcurrentHashMap<String, Session> user2session = (ConcurrentHashMap<String, Session>) jndi
				.lookup(WasabiConstants.JNDI_JCR_USER2SESSION);
		user2session.put(username, session);
	}

	@SuppressWarnings("unchecked")
	public Session getJCRSession(String username) throws UnexpectedInternalProblemException {
		ConcurrentHashMap<String, Session> user2session = (ConcurrentHashMap<String, Session>) jndi
				.lookup(WasabiConstants.JNDI_JCR_USER2SESSION);
		return user2session.get(username);
	}

	public Session getJCRSession() throws UnexpectedInternalProblemException {
		try {
			return getJCRRepository().login(new SimpleCredentials("user", "pwd".toCharArray()));
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
