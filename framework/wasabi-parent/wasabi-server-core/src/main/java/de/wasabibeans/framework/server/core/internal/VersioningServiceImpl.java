package de.wasabibeans.framework.server.core.internal;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class VersioningServiceImpl {

	/**
	 * Recursively creates a new version for the given node as well as for each versionable node in the subtree of the
	 * given node. All versions created by this method will have the same comment and the same label.
	 * 
	 * @param node
	 * @param label
	 * @param comment
	 * @param versionManager
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException 
	 */
	public static void createVersionRecursively(Node node, String label, String comment, VersionManager versionManager)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			// check whether the node supports versioning
			VersionHistory versionHistory = null;
			try {
				versionHistory = versionManager.getVersionHistory(node.getPath());
			} catch (UnsupportedRepositoryOperationException uroe) {
				// does not support versioning
				return;
			}

			// deal with the subtree first
			for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
				createVersionRecursively(ni.nextNode(), label, comment, versionManager);
			}

			// create a version
			node.setProperty(WasabiNodeProperty.VERSION_COMMENT, comment);
			node.getSession().save();
			Version version = versionManager.checkin(node.getPath());
			versionHistory.addVersionLabel(version.getName(), label, false);
			versionManager.checkout(node.getPath());
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Recursively restores the version represented by the given {@code label} for the given node as well as for each
	 * versionable node in the subtree of the given node.
	 * 
	 * @param node
	 * @param label
	 * @param versionManager
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException 
	 */
	public static void restoreVersionRecursively(Node node, String label, VersionManager versionManager)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			// check whether the node supports versioning
			try {
				versionManager.checkout(node.getPath());
			} catch (UnsupportedRepositoryOperationException uroe) {
				// does not support versioning
				return;
			}

			// deal with the subtree first
			for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
				restoreVersionRecursively(ni.nextNode(), label, versionManager);
			}

			// restore the version
			versionManager.restoreByLabel(node.getPath(), label, true);
			versionManager.checkout(node.getPath());
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
