package de.wasabibeans.framework.server.core.locking;

import javax.ejb.Local;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface LockingHelperLocal {

	public String acquireLock(String nodePath, boolean isDeep) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException;

	public void releaseLock(String nodePath, String lockToken) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException;
}
