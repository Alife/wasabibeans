package de.wasabibeans.framework.server.core.bean;

import java.util.Calendar;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
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

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.aop.JCRSessionInterceptor;
import de.wasabibeans.framework.server.core.aop.WasabiAOP;
import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.VersioningServiceImpl;
import de.wasabibeans.framework.server.core.local.VersioningServiceLocal;
import de.wasabibeans.framework.server.core.remote.VersioningServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;

@SecurityDomain("wasabi")
@Stateless(name = "VersioningService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors( { JCRSessionInterceptor.class })
public class VersioningService implements VersioningServiceLocal, VersioningServiceRemote, WasabiAOP {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;
	protected JndiConnector jndi;

	/**
	 * Creates a new version for the wasabi-object represented by the given {@code WasabiObjectDTO} {@code dto}. If the
	 * given wasabi-object has a subtree that contains further versionable wasabi-objects, then a new version for each
	 * of these versionable wasabi-objects will be created as well. The entire subtree of the given wasabi-object will
	 * be locked during this method. All versions created by this method will have the same comment and the same label.
	 * 
	 * @param object
	 * @param comment
	 * @throws UnexpectedInternalProblemException
	 * @throws ObjectDoesNotExistException
	 * @throws ConcurrentModificationException
	 * @throws NoPermissionException
	 */
	public void createVersion(WasabiObjectDTO object, String comment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		if (!(object instanceof WasabiLocationDTO || object instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node objectNode = null;
		Session s = jcr.getJCRSession();
		try {
			objectNode = TransferManager.convertDTO2Node(object, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "VersionService.createVersions()",
								"WRITE", "object"));
			/* Authorization - End */

			// get unique version label (that is, unique in the version histories of the affected nodes)
			// timestamp
			String label = "" + Calendar.getInstance().getTimeInMillis();
			// create versions for entire subtree
			VersioningServiceImpl.createVersionRecursively(objectNode, label, comment, s.getWorkspace()
					.getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public JcrConnector getJcrConnector() {
		return jcr;
	}

	public JndiConnector getJndiConnector() {
		return jndi;
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
	 * @throws NoPermissionException
	 */
	public Vector<WasabiVersionDTO> getVersions(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (!(object instanceof WasabiLocationDTO || object instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Session s = jcr.getJCRSession();
		try {
			Node objectNode = TransferManager.convertDTO2Node(object, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "VersionService.getVersions()",
								"READ", "object"));
			/* Authorization - End */

			VersionHistory versionHistory = s.getWorkspace().getVersionManager()
					.getVersionHistory(objectNode.getPath());
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
	 * Restores the version represented by the given {@code versionLabel} for the wasabi-object represented by the given
	 * {@code WasabiObjectDTO} {@code dto}. If the subtree of the given wasabi-object contains further versionable
	 * wasabi-objects, then a version represented by the given {@code vesionLabel} will be restored for each of these
	 * versionable wasabi-objects as well. The entire subtree of the given wasabi-object will be locked during this
	 * method.
	 * 
	 * @param object
	 * @param versionLabel
	 * @throws UnexpectedInternalProblemException
	 * @throws ObjectDoesNotExistException
	 * @throws ConcurrentModificationException
	 * @throws NoPermissionException
	 */
	public void restoreVersion(WasabiObjectDTO object, String versionLabel) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException {
		if (!(object instanceof WasabiLocationDTO || object instanceof WasabiDocumentDTO)) {
			throw new IllegalArgumentException(WasabiExceptionMessages.VERSIONING_NOT_SUPPORTED);
		}

		Node objectNode = null;
		Session s = jcr.getJCRSession();
		try {
			objectNode = TransferManager.convertDTO2Node(object, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.WRITE, s))
						throw new NoPermissionException(WasabiExceptionMessages.get(
								WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION,
								"VersionService.restoreVersions()", "WRITE", "object"));
			/* Authorization - End */

			// restore versions for entire subtree
			VersioningServiceImpl.restoreVersionRecursively(objectNode, versionLabel, s.getWorkspace()
					.getVersionManager());
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
