package de.wasabibeans.framework.server.core.bean;

import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.local.VersioningServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.remote.VersioningServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless(name = "VersioningService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class VersioningService implements VersioningServiceLocal, VersioningServiceRemote {

	@EJB
	private LockingHelperLocal locker;

	private JndiConnector jndi;
	private JcrConnector jcr;

	@PostConstruct
	public void postConstruct() {
		this.jndi = JndiConnector.getJNDIConnector();
		this.jcr = JcrConnector.getJCRConnector(jndi);
	}

	@PreDestroy
	public void preDestroy() {
		jndi.close();
	}

	/**
	 * Returns a {@code Vector} of {@code WasabiVersionDTO}s. Each {@code WasabiVersionDTO} encapsulates the label, the
	 * comment, and the creation-date of a version of the wasabi-object represented by the given {@code WasabiObjectDTO}
	 * {@code object}. Only applicable for wasabi-rooms, wasabi-containers and wasabi-documents.
	 * 
	 * @param object
	 * @return {@code Vector} of {@code WasabiVersionDTO}s
	 * @throws UnexpectedInternalProblemException
	 * @throws ObjectDoesNotExistException
	 */
	public Vector<WasabiVersionDTO> getVersions(WasabiObjectDTO dto) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		if (!(dto instanceof WasabiLocationDTO || dto instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Session s = jcr.getJCRSession();
		try {
			Node node = TransferManager.convertDTO2Node(dto, s);
			VersionHistory versionHistory = s.getWorkspace().getVersionManager().getVersionHistory(node.getPath());
			String rootVersionName = versionHistory.getRootVersion().getName();
			Vector<WasabiVersionDTO> versions = new Vector<WasabiVersionDTO>();
			for (VersionIterator vi = versionHistory.getAllVersions(); vi.hasNext();) {
				Version aVersion = vi.nextVersion();
				// do not expose the JCR root version, it cannot be restored anyway
				if (!aVersion.getName().equals(rootVersionName)) {
					versions.add(TransferManager.convertVersion2DTO(aVersion, versionHistory));
				}
			}
			return versions;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jcr.logout();
		}
	}

	/**
	 * Creates a new version for the wasabi-object represented by the given {@code WasabiObjectDTO} {@code dto}. If the
	 * given wasabi-object has a subtree that contains further versionable wasabi-objects, then a new version for each
	 * of these versionable wasabi-objects will be created as well. The entire subtree of the given wasabi-object will
	 * be locked during this method. All versions created by this method will have the same comment and the same label.
	 * 
	 * @param dto
	 * @param comment
	 * @throws UnexpectedInternalProblemException
	 * @throws ObjectDoesNotExistException
	 * @throws ConcurrentModificationException
	 */
	public void createVersion(WasabiObjectDTO dto, String comment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		if (!(dto instanceof WasabiLocationDTO || dto instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node node = null;
		Session s = jcr.getJCRSession();
		try {
			node = TransferManager.convertDTO2Node(dto, s);
			// get unique version label
			String label = getUniqueVersionLabel(s.getRootNode().getNode(WasabiConstants.JCR_HIGHEST_VERSION_LABEL),
					dto, s);
			// acquire deep lock
			Locker.acquireLock(node, dto, true, s, locker);
			// create versions for entire subtree
			createVersionRecursively(node, label, comment, s.getWorkspace().getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(node, s, locker);
			jcr.logout();
		}
	}

	/**
	 * Returns a version-label that must be unique in the version-history of the node to be versioned and in the
	 * version-histories of the versionable nodes in the subtree of the node to be versioned.
	 * 
	 * @param workspaceRoot
	 * @param dto
	 * @param s
	 * @return
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	private String getUniqueVersionLabel(Node highestVersionLabelStore, WasabiObjectDTO dto, Session s)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			Locker.acquireLock(highestVersionLabelStore, dto, false, s, locker);
			Long uniqueVersionNumber = highestVersionLabelStore.getProperty(WasabiConstants.JCR_HIGHEST_VERSION_LABEL)
					.getLong();
			highestVersionLabelStore.setProperty(WasabiConstants.JCR_HIGHEST_VERSION_LABEL, ++uniqueVersionNumber);
			s.save();
			return "" + uniqueVersionNumber;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(highestVersionLabelStore, s, locker);
		}
	}

	/**
	 * Recursively creates a new version for the given node as well as for each versionable node in the subtree of the
	 * given node. All versions created by this method will have the same comment and the same label.
	 * 
	 * @param node
	 * @param label
	 * @param comment
	 * @param versionManager
	 * @throws UnexpectedInternalProblemException
	 */
	private void createVersionRecursively(Node node, String label, String comment, VersionManager versionManager)
			throws UnexpectedInternalProblemException {
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
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Restores the version represented by the given {@code versionLabel} for the wasabi-object represented by the given
	 * {@code WasabiObjectDTO} {@code dto}. If the subtree of the given wasabi-object contains further versionable
	 * wasabi-objects, then a version represented by the given {@code vesionLabel} will be restored for each of these
	 * versionable wasabi-objects as well. The entire subtree of the given wasabi-object will be locked during this
	 * method.
	 * 
	 * @param dto
	 * @param versionLabel
	 * @throws UnexpectedInternalProblemException
	 * @throws ObjectDoesNotExistException
	 * @throws ConcurrentModificationException
	 */
	public void restoreVersion(WasabiObjectDTO dto, String versionLabel) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		if (!(dto instanceof WasabiLocationDTO || dto instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node node = null;
		Session s = jcr.getJCRSession();
		try {
			node = TransferManager.convertDTO2Node(dto, s);
			// acquire deep lock
			Locker.acquireLock(node, dto, true, s, locker);
			// restore versions for entire subtree
			restoreVersionRecursively(node, versionLabel, s.getWorkspace().getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(node, s, locker);
			jcr.logout();
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
	 */
	private void restoreVersionRecursively(Node node, String label, VersionManager versionManager)
			throws UnexpectedInternalProblemException {
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
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
