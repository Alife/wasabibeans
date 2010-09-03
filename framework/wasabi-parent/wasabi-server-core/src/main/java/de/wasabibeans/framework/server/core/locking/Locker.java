package de.wasabibeans.framework.server.core.locking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;

public class Locker {

	/**
	 * Attempts to acquire a lock on the given {@code node}. If the lock cannot be acquired due to an already existing
	 * lock or due to the fact that the given {@code optLockId} does not match the actual optLockId of the {@code node}
	 * (that is the 'wasabi:optLockId' property of the node), a {@code ConcurrentModificationException} is thrown.
	 * 
	 * @param node
	 * @param dto
	 *            the DTO that belongs to the given {@code node}. Used for more detailed exception messages.
	 * @param optLockId
	 *            must match the 'wasabi:optLockId' property of the given {@code node} for a successful execution of
	 *            this method
	 * @param s
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void acquireLock(Node node, WasabiObjectDTO dto, Long optLockId, Session s, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			String lockToken = locker.acquireLock(node.getIdentifier(), false);
			LockManager lockManager = s.getWorkspace().getLockManager();
			lockManager.addLockToken(lockToken);
			if (optLockId != null && !optLockId.equals(ObjectServiceImpl.getOptLockId(node))) {
				lockManager.removeLockToken(lockToken);
				locker.releaseLock(node.getIdentifier(), lockToken);
				throw new ConcurrentModificationException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_LOCKING_OPTLOCK, (dto != null) ? dto.toString() : ""));
			}
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_GENERAL, (dto != null) ? dto.toString() : ""), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Attempts to acquire a lock on the given {@code node}. If the lock cannot be acquired due to an already existing
	 * lock, a {@code ConcurrentModificationException} is thrown.
	 * 
	 * @param node
	 * @param dto
	 *            the DTO that belongs to the given {@code node}. Used for more detailed exception messages.
	 * @param isDeep
	 *            set to {@code true}, if the entire subtree of the given {@code node} should be locked
	 * @param s
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void acquireLock(Node node, WasabiObjectDTO dto, boolean isDeep, Session s, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			String lockToken = locker.acquireLock(node.getIdentifier(), isDeep);
			s.getWorkspace().getLockManager().addLockToken(lockToken);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_GENERAL, (dto != null) ? dto.toString() : ""), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Unlocks the given {@code node}. Does nothing if the given {@code node} is {@code null} or not locked at all.
	 * 
	 * @param node
	 * @param s
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @throws UnexpectedInternalProblemException
	 */
	public static void releaseLock(Node node, Session s, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException {
		if (node == null) {
			// do nothing
			return;
		}
		try {
			LockManager lockManager = s.getWorkspace().getLockManager();
			String lockToken = lockManager.getLock(node.getPath()).getLockToken();
			lockManager.removeLockToken(lockToken);
			locker.releaseLock(node.getIdentifier(), lockToken);
		} catch (LockException e) {
			// do nothing... there was no lock to unlock
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
