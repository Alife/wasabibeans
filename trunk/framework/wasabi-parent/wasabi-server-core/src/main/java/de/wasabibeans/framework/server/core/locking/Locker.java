package de.wasabibeans.framework.server.core.locking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class Locker {

	private static WasabiLogger logger = WasabiLogger.getLogger(Locker.class);

	/**
	 * If the given {@code dtosWithPotentialLockTokens} contain lock-tokens, these lock-tokens will be associated with
	 * the given {@code Session s}.
	 * 
	 * @param s
	 * @param dtosWithPotentialLockTokens
	 * @throws UnexpectedInternalProblemException
	 */
	public static void recognizeLockTokens(Session s, WasabiObjectDTO... dtosWithPotentialLockTokens)
			throws UnexpectedInternalProblemException {
		for (WasabiObjectDTO dto : dtosWithPotentialLockTokens) {
			String potentialLockToken = dto != null ? dto.getLockToken() : null;
			recognizeLockToken(s, potentialLockToken);
		}
	}

	/**
	 * Associates the given {@code lockToken} with the the given {@code Session s}.
	 * 
	 * @param s
	 * @param lockTokens
	 * @throws UnexpectedInternalProblemException
	 */
	public static void recognizeLockToken(Session s, String lockToken) throws UnexpectedInternalProblemException {
		if (lockToken != null) {
			try {
				s.getWorkspace().getLockManager().addLockToken(lockToken);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		}
	}

	/**
	 * Checks if the given {@code optLockId} matches the actual optLockId of the given {@code node} (that is the
	 * 'wasabi:optLockId' property of the node). If there is no match, a {@code ConcurrentModificationException} is
	 * thrown. Does nothing if the given {@code optLockId} is {@code null} or smaller than 0.
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
		if (optLockId != null && optLockId >= 0) {
			if (!optLockId.equals(ObjectServiceImpl.getOptLockId(node))) {
				throw new ConcurrentModificationException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_LOCKING_OPTLOCK, (dto != null) ? dto.toString() : ""));
			}
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

	// -------------------- Methods for explicit ('manual') locking --------------------------------------

	/**
	 * Attempts to acquire a lock on the given {@code node}. If the lock cannot be acquired due to an already existing
	 * lock, a {@code LockingException} is thrown. If the lock can be acquired, the corresponding lock-token will be
	 * returned.
	 * 
	 * @param node
	 * @param dto
	 *            the dto that belongs to the given node
	 * @param isDeep
	 *            set to {@code true}, if the entire subtree of the given {@code node} should be locked
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @return the lock-token
	 * @throws UnexpectedInternalProblemException
	 * @throws LockingException
	 */
	public static String acquireLock(Node node, WasabiObjectDTO dto, boolean isDeep, LockingHelperLocal locker)
			throws UnexpectedInternalProblemException, LockingException {
		try {
			// call the LockingHelperLocal instance to set the lock within a separate transaction
			return locker.acquireLock(dto.getId(), isDeep);
		} catch (LockException le) {
			throw new LockingException("The object represented by the dto " + dto.toString() + " is already locked.",
					le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Unlocks the given {@code node}. Does nothing if the given {@code node} is not locked at all.
	 * 
	 * @param node
	 * @param s
	 * @throws UnexpectedInternalProblemException
	 */
	public static void releaseLock(Node node, Session s) throws UnexpectedInternalProblemException {
		try {
			// unlock
			LockManager lockManager = s.getWorkspace().getLockManager();
			lockManager.unlock(node.getPath());
		} catch (LockException e) {
			// do nothing... there was no lock to unlock
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// -------------- Methods and classes for 'service-call'-locking ----------------------------------

	/**
	 * Attempts to acquire a lock on the nodes represented by the given {@code dto}. If the lock can be acquired, the
	 * corresponding lock-token will be returned. The established lock will be automatically unlocked once the
	 * transaction within which this method has been called completes. Returns {@code null} if the given {@code
	 * optLockId} is {@code null}.
	 * 
	 * @param dto
	 * @param optLockId
	 * @param locker
	 *            instance of {@code LockingHelperLocal}. This instance needs to have been retrieved by dependency
	 *            injection or by JNDI lookup, because it is mandatory that the EJB container uses separate transactions
	 *            for the execution of the methods of {@code LockingHelperLocal}.
	 * @param tm
	 *            the transaction manager of the EJB container
	 * @return the lock-token or {@code null} (if {@code optLockId} is {@code null})
	 * @throws ConcurrentModificationException
	 * @throws UnexpectedInternalProblemException
	 */
	public static String acquireServiceCallLock(WasabiObjectDTO dto, Long optLockId, LockingHelperLocal locker,
			TransactionManager tm) throws ConcurrentModificationException, UnexpectedInternalProblemException {
		if (optLockId == null) {
			return null;
		}
		try {
			String lockToken = locker.acquireLock(dto.getId(), false);
			tm.getTransaction().registerSynchronization(new ServiceCallLockUnlocker(dto.getId(), lockToken));
			return lockToken;
		} catch (LockException e) {
			throw new ConcurrentModificationException("The object represented by " + dto.toString()
					+ " is already locked.", e);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(
					"The services of the EJB container could not be used as expected.", e);
		}
	}

	/**
	 * If a 'service-call'-lock is acquired, an instance of this class is registered as a listener of the transaction
	 * that encapsulates the service call. Once the transaction has completed, the {@code afterCompletion()} method of
	 * this class unlocks the 'service-call'-lock.
	 * 
	 */
	static class ServiceCallLockUnlocker implements Synchronization {

		private String nodeId;
		private String lockToken;

		public ServiceCallLockUnlocker(String nodeId, String lockToken) {
			this.nodeId = nodeId;
			this.lockToken = lockToken;
		}

		@Override
		public void afterCompletion(int status) {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			try {
				LockingHelperLocal locker = (LockingHelperLocal) jndi.lookupLocal("LockingHelper");
				locker.releaseLock(nodeId, lockToken);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("A lock used for a single service call could not be unlocked.");
			} finally {
				jndi.close();
			}
		}

		@Override
		public void beforeCompletion() {
			// not interested in this
		}
	}
}
