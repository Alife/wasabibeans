package de.wasabibeans.framework.server.core.locking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;

public class Locker {

	/**
	 * Checks if the given {@code optLockId} matches the actual optLockId of the given {@code node} (that is the
	 * 'wasabi:optLockId' property of the node). If there is no match, a {@code ConcurrentModificationException} is
	 * thrown.
	 * 
	 * @param node
	 * @param dto
	 *            DTO that belongs to the given {@code node}
	 * @param optLockId
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void checkOptLockId(Node node, WasabiObjectDTO dto, Long optLockId)
			throws ConcurrentModificationException, UnexpectedInternalProblemException {
		if (optLockId != null && !optLockId.equals(ObjectServiceImpl.getOptLockId(node))) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_OPTLOCK, (dto != null) ? dto.toString() : ""));
		}
	}

	/**
	 * If the given {@code dtosWithPotentialLockTokens} contain lock-tokens, these lock-tokens will be associated with
	 * the given {@code Session s}.
	 * 
	 * @param parentDTO
	 * @param s
	 * @throws UnexpectedInternalProblemException
	 */
	public static void recognizeLockTokens(Session s, WasabiObjectDTO... dtosWithPotentialLockTokens)
			throws UnexpectedInternalProblemException {
		try {
			for (WasabiObjectDTO dto : dtosWithPotentialLockTokens) {
				String potentialLockToken = dto != null ? dto.getLockToken() : null;
				if (potentialLockToken != null) {
					s.getWorkspace().getLockManager().addLockToken(potentialLockToken);
				}
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Clears the given {@code Session s} of all lock-tokens it may hold.
	 * 
	 * @param s
	 * @throws UnexpectedInternalProblemException
	 */
	public static void cleanUpLockTokens(Session s) throws UnexpectedInternalProblemException {
		try {
			LockManager lockManager = s.getWorkspace().getLockManager();
			for (String lockToken : lockManager.getLockTokens()) {
				lockManager.removeLockToken(lockToken);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Attempts to acquire a lock on the given {@code node}. If the lock cannot be acquired due to an already existing
	 * lock, a {@code ConcurrentModificationException} is thrown. If the lock can be acquired, it will automatically be
	 * associated with the given {@code session} and the corresponding lock-token will be returned. If the given {@code
	 * dto} contains a lock-token that is still valid, then that lock-token will be returned (and the corresponding lock
	 * should already be associated with the given {@code session} due to a previous call of {@code
	 * recognizeLockTokens()}). However, if an existing lock-token is still valid but the associated lock is not deep
	 * although {@code isDeep} is {@code true}, then a {@code LockingException} is thrown.
	 * 
	 * @param node
	 * @param dto
	 *            DTO that belongs to the given {@code node}
	 * @param isDeep
	 *            set to {@code true}, if the entire subtree of the given {@code node} should be locked
	 * @param s
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @return the lock-token
	 * @throws ConcurrentModificationException
	 * @throws UnexpectedInternalProblemException
	 * @throws LockingException
	 */
	public static String acquireLock(Node node, WasabiObjectDTO dto, boolean isDeep, Session s,
			LockingHelperLocal locker) throws ConcurrentModificationException, UnexpectedInternalProblemException,
			LockingException {
		try {
			LockManager lockManager = s.getWorkspace().getLockManager();
			String existingLockToken = dto.getLockToken();
			// if the dto contains a lock-token, check its validity
			if (existingLockToken != null) {
				try {
					Lock lock = lockManager.getLock(node.getPath());
					if (existingLockToken.equals(lock.getLockToken())) {
						// if a deep lock is required, check whether existing lock is deep
						if (isDeep && !lock.isDeep()) {
							throw new LockingException(WasabiExceptionMessages.INTERNAL_LOCKING_NOT_DEEP);
						}
						// existing lock-token still valid
						return existingLockToken;
					}
				} catch (LockException le) {
					// no lock found for node, remove invalid existing lock token, then proceed and try to set a new
					// lock
					lockManager.removeLockToken(existingLockToken);
				}
			}
			// try to set a new lock
			String newToken = locker.acquireLock(dto.getId(), isDeep);
			lockManager.addLockToken(newToken);
			return newToken;
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_LOCKING_GENERAL, (dto != null) ? dto.toString() : ""), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Unlocks the given {@code node}. Does nothing if the given {@code node} is {@code null} or if the given {@code
	 * node} is not locked at all or if the given {@code dto} contains a lock-token that is still valid.
	 * 
	 * @param node
	 * @param dto
	 *            DTO that belongs to the given {@code node}
	 * @param s
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @throws UnexpectedInternalProblemException
	 */
	public static void releaseLock(Node node, WasabiObjectDTO dto, Session s, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException {
		if (node == null) {
			// do nothing
			return;
		}
		try {
			// get active lock-token (if exists)
			LockManager lockManager = s.getWorkspace().getLockManager();
			String lockToken = lockManager.getLock(node.getPath()).getLockToken();
			
			// remove lock-token from the given session in any case
			lockManager.removeLockToken(lockToken);

			// if the dto contains a lock-token, check its validity
			String dtoLockToken = dto.getLockToken();
			if (dtoLockToken != null) {
				if (dtoLockToken.equals(lockToken)) {
					// dto-lock-token still valid, do not unlock
					return;
				}
			}

			// release lock
			releaseLock(dto.getId(), lockToken, locker);
		} catch (LockException e) {
			// do nothing... there was no lock to unlock
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Unlocks the node represented by the given {@code id}. Does nothing if the node is not locked at all.
	 * 
	 * @param id
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @throws UnexpectedInternalProblemException
	 */
	public static void releaseLock(String id, String lockToken, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException {
		try {
			locker.releaseLock(id, lockToken);
		} catch (LockException e) {
			// do nothing... there was no lock to unlock
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
