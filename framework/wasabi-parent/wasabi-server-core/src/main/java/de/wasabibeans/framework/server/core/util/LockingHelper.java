package de.wasabibeans.framework.server.core.util;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Stateless
public class LockingHelper implements LockingHelperLocal {

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void lock(Node node) throws UnexpectedInternalProblemException {
		try {
			node.getSession().getWorkspace().getLockManager().lock(node.getPath(), false, true, 120, null);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_LOCK_AQUISITION, re);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void unlock(Node node) throws UnexpectedInternalProblemException {
		try {
			node.getSession().getWorkspace().getLockManager().unlock(node.getPath());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_LOCK_RELEASE, re);
		}
	}
}
