/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 *  Further information are online available at: http://www.wasabibeans.de
 */

package de.wasabibeans.framework.server.core.locking;

import java.lang.reflect.Method;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.apache.jackrabbit.core.lock.SessionLockManager;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class Locker {

	private static WasabiLogger logger = WasabiLogger.getLogger(Locker.class);

	/**
	 * If the given {@code dtoWithPotentialLockToken} contains a lock-token, this lock-token will be associated with the
	 * given {@code Session s}.
	 * 
	 * @param s
	 * @param dtoWithPotentialLockToken
	 * @throws UnexpectedInternalProblemException
	 */
	public static void recognizeLockToken(Session s, WasabiObjectDTO dtoWithPotentialLockToken)
			throws UnexpectedInternalProblemException {
		String potentialLockToken = dtoWithPotentialLockToken != null ? dtoWithPotentialLockToken.getLockToken() : null;
		recognizeLockToken(s, potentialLockToken);
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
				throw new ConcurrentModificationException(WasabiExceptionMessages.LOCKING_OPTLOCK);
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
				try {
					lockManager.removeLockToken(lockToken);
				} catch (ItemNotFoundException infe) {
					/*
					 * This happens if the session still holds a lock-token for a node that does not exist any more. Use
					 * a workaround to forcibly remove such a 'corrupt' lock-token from the session.
					 */
					try {
						SessionLockManager slm = (SessionLockManager) lockManager;
						Method lockTokenRemoved = slm.getClass().getDeclaredMethod("lockTokenRemoved", String.class);
						lockTokenRemoved.setAccessible(true);
						lockTokenRemoved.invoke(slm, lockToken);
					} catch (Exception e) {
						throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
					}
				}
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// -------------------- Methods for explicit ('manual') locking --------------------------------------

	/**
	 * Attempts to acquire a lock on the given {@code node}. If the lock cannot be acquired due to an already existing
	 * lock, a {@code ConcurrentModificationException} is thrown. If the lock can be acquired, the corresponding
	 * lock-token will be returned.
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
	 * @throws ConcurrentModificationException
	 * @throws ObjectDoesNotExistException
	 */
	public static String acquireLock(Node node, WasabiObjectDTO dto, boolean isDeep, Session s,
			LockingHelperLocal locker) throws UnexpectedInternalProblemException, ConcurrentModificationException,
			ObjectDoesNotExistException {
		try {
			// call the LockingHelperLocal instance to set the lock within a separate transaction
			return locker.acquireLock(dto.getId(), isDeep);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
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
	 * @throws ObjectDoesNotExistException
	 * @throws ConcurrentModificationException
	 */
	public static void releaseLock(Node node, Session s) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		try {
			// unlock
			LockManager lockManager = s.getWorkspace().getLockManager();
			lockManager.unlock(node.getPath());
		} catch (LockException e) {
			// do nothing... there was no lock to unlock
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// -------------- Methods and classes for 'service-call'-locking ----------------------------------

	/**
	 * Attempts to acquire a lock on the nodes represented by the given {@code dto}. If the lock can be acquired, the
	 * corresponding lock-token will be returned. The established lock will be automatically unlocked once the
	 * transaction within which this method has been called completes. Does nothing and returns {@code null} if the
	 * given {@code dto} or the given {@code optLockId} is {@code null}.
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
	 * @throws ObjectDoesNotExistException
	 */
	public static String acquireServiceCallLock(WasabiObjectDTO dto, Long optLockId, LockingHelperLocal locker,
			TransactionManager tm) throws ConcurrentModificationException, UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		if (dto == null || optLockId == null) {
			return null;
		}
		try {
			String lockToken = locker.acquireLock(dto.getId(), false);
			tm.getTransaction().registerSynchronization(new ServiceCallLockUnlocker(dto.getId(), lockToken, locker));
			return lockToken;
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectDoesNotExistException(WasabiExceptionMessages.OBJECT_DNE);
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.INTERNAL_EJB_CONTAINER_PROBLEM, e);
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
		private LockingHelperLocal locker;

		public ServiceCallLockUnlocker(String nodeId, String lockToken, LockingHelperLocal locker) {
			this.nodeId = nodeId;
			this.lockToken = lockToken;
			this.locker = locker;
		}

		@Override
		public void afterCompletion(int status) {
			JndiConnector jndi = JndiConnector.getJNDIConnector();
			try {
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
