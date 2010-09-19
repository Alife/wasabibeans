package de.wasabibeans.framework.server.core.bean;

import java.util.Calendar;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import de.wasabibeans.framework.server.core.aop.TransactionInterceptor;
import de.wasabibeans.framework.server.core.aop.WasabiService;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.LockingException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.VersioningServiceImpl;
import de.wasabibeans.framework.server.core.local.VersioningServiceLocal;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.remote.VersioningServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@Stateless(name = "VersioningService")
@Interceptors( { TransactionInterceptor.class })
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class VersioningService extends WasabiService implements VersioningServiceLocal, VersioningServiceRemote {

	@EJB
	private LockingHelperLocal locker;

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
	 * @throws LockingException
	 */
	public void createVersion(WasabiObjectDTO dto, String comment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, LockingException {
		if (!(dto instanceof WasabiLocationDTO || dto instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node node = null;
		Session s = jcr.getJCRSession();
		try {
			node = TransferManager.convertDTO2Node(dto, s);
			// get unique version label (that is, unique in the version histories of the affected nodes)
			// timestamp
			String label = "" + Calendar.getInstance().getTimeInMillis();
			// acquire deep lock
			Locker.recognizeLockTokens(s, dto);
			Locker.acquireLock(node, dto, true, s, locker);
			// create versions for entire subtree
			VersioningServiceImpl.createVersionRecursively(node, label, comment, s.getWorkspace().getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(node, dto, s, locker);
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
	 * @throws LockingException
	 */
	public void restoreVersion(WasabiObjectDTO dto, String versionLabel) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, LockingException {
		if (!(dto instanceof WasabiLocationDTO || dto instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node node = null;
		Session s = jcr.getJCRSession();
		try {
			node = TransferManager.convertDTO2Node(dto, s);
			// acquire deep lock
			Locker.recognizeLockTokens(s, dto);
			Locker.acquireLock(node, dto, true, s, locker);
			// restore versions for entire subtree
			VersioningServiceImpl.restoreVersionRecursively(node, versionLabel, s.getWorkspace().getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			Locker.releaseLock(node, dto, s, locker);
		}
	}
}
