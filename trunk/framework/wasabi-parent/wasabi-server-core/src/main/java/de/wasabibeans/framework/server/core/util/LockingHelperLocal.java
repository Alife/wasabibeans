package de.wasabibeans.framework.server.core.util;

import javax.ejb.Local;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface LockingHelperLocal {

	public String acquireLock(Node node) throws AccessDeniedException, LockException, PathNotFoundException,
			InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException,
			UnexpectedInternalProblemException;

	public void releaseLock(Node node, String lockToken) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException;
}
