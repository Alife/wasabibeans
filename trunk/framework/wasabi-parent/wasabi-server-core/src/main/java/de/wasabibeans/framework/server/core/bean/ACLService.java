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

package de.wasabibeans.framework.server.core.bean;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.local.ACLServiceLocal;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;

@SecurityDomain("wasabi")
@Stateless(name = "ACLService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ACLService implements ACLServiceLocal, ACLServiceRemote {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;
	protected JndiConnector jndi;

	@Override
	public void activateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		try {
			Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.activateInheritance()",
							"GRANT", "wasabiObject"));
			/* Authorization - End */

			ACLServiceImpl.setInheritance(objectNode, true, s);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE + "::"
					+ re.toString(), re);
		}
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.create()", "GRANT",
						"wasabiObject"));
		/* Authorization - End */

		Node identityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.create(objectNode, identityNode, perm, allow, 0, 0, s);
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.create()", "GRANT",
						"wasabiObject"));
		/* Authorization - End */

		Node identityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.create(objectNode, identityNode, perm, allow, startTime, endTime, s);
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.create()", "GRANT",
						"wasabiObject"));
		/* Authorization - End */

		Node identityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		ACLServiceImpl.create(objectNode, identityNode, permission, allowance, 0, 0, s);
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		if (startTime.length != endTime.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "startTime", "endTime"));
		}

		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.create()", "GRANT",
						"wasabiObject"));
		/* Authorization - End */

		Node identityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

		for (int i = 0; i < endTime.length; i++)
			ACLServiceImpl.create(objectNode, identityNode, permission, allowance, startTime[i], endTime[i], s);
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.createDefault()", "GRANT",
						"wasabiLocation"));
		/* Authorization - End */

		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.createDefault(locationNode, wasabiType, perm, allow, 0, 0);
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.createDefault()", "GRANT",
						"wasabiLocation"));
		/* Authorization - End */

		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.createDefault(locationNode, wasabiType, perm, allow, startTime, endTime);
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.createDefault()", "GRANT",
						"wasabiLocation"));
		/* Authorization - End */

		ACLServiceImpl.createDefault(locationNode, wasabiType, permission, allowance, 0, 0);
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.GRANT, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.createDefault()", "GRANT",
						"wasabiObject"));
		/* Authorization - End */

		for (int i = 0; i < endTime.length; i++)
			ACLServiceImpl.createDefault(locationNode, wasabiType, permission, allowance, startTime[i], endTime[i]);
	}

	@Override
	public void deactivateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		try {
			Session s = jcr.getJCRSessionTx();
			Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.GRANT, s))
					throw new NoPermissionException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.activateInheritance()",
							"GRANT", "wasabiObject"));
			/* Authorization - End */

			ACLServiceImpl.setInheritance(objectNode, false, s);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public Vector<WasabiACLEntryDTO> getAclEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getAclEntries()", "READ",
						"wasabiObject"));
		/* Authorization - End */

		Vector<WasabiACLEntryDTO> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTO>();
		List<WasabiACLEntry> wasabiALCEntries = ACLServiceImpl.getAclEntries(objectNode);

		for (WasabiACLEntry wasabiACLEntry : wasabiALCEntries)
			wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDTO(wasabiACLEntry));

		return wasabiACLEntriesDTO;
	}

	@Deprecated
	@Override
	public Vector<WasabiACLEntryDTODeprecated> getACLEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getACLEntries()", "READ",
						"wasabiObject"));
		/* Authorization - End */

		Vector<WasabiACLEntryDTODeprecated> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTODeprecated>();
		Vector<WasabiACLEntryDeprecated> wasabiALCEntries = ACLServiceImpl.getACLEntriesDeprecated(objectNode, null, s);

		for (Iterator<WasabiACLEntryDeprecated> iterator = wasabiALCEntries.iterator(); iterator.hasNext();) {
			WasabiACLEntryDeprecated wasabiACLEntryDeprecated = (WasabiACLEntryDeprecated) iterator.next();
			wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDeprecated2DTO(wasabiACLEntryDeprecated));
		}
		return wasabiACLEntriesDTO;
	}

	@Override
	public Vector<WasabiACLEntryDTO> getAclEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node identityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getAclEntriesByIdentity()",
						"READ", "wasabiObject"));
		/* Authorization - End */

		Vector<WasabiACLEntryDTO> wasabiACLEntriesByIdentityDTO = new Vector<WasabiACLEntryDTO>();
		List<WasabiACLEntry> wasabiALCEntriesByIdentity = ACLServiceImpl.getAclEntriesByIdentity(objectNode,
				identityNode);

		for (Iterator<WasabiACLEntry> iterator = wasabiALCEntriesByIdentity.iterator(); iterator.hasNext();) {
			WasabiACLEntry wasabiACLEntryByIdentity = (WasabiACLEntry) iterator.next();
			wasabiACLEntriesByIdentityDTO.add(TransferManager.convertWasabiACLEntryDTO(wasabiACLEntryByIdentity));
		}

		return wasabiACLEntriesByIdentityDTO;
	}

	@Deprecated
	@Override
	public Vector<WasabiACLEntryDTODeprecated> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getACLEntriesByIdentity()",
						"READ", "wasabiObject"));
		/* Authorization - End */

		Vector<WasabiACLEntryDTODeprecated> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTODeprecated>();
		Vector<WasabiACLEntryDeprecated> wasabiALCEntries = ACLServiceImpl.getACLEntriesDeprecated(objectNode,
				wasabiIdentityNode, s);

		for (WasabiACLEntryDeprecated wasabiACLEntryDeprecated : wasabiALCEntries)
			wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDeprecated2DTO(wasabiACLEntryDeprecated));

		return wasabiACLEntriesDTO;
	}

	@Override
	public Vector<WasabiACLEntryTemplateDTO> getDefaultAclEntries(WasabiLocationDTO wasabiLocation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getDefaultAclEntries()",
						"READ", "wasabiLocation"));
		/* Authorization - End */

		Vector<WasabiACLEntryTemplateDTO> wasabiDefaultACLEntriesDTO = new Vector<WasabiACLEntryTemplateDTO>();
		List<WasabiACLEntryTemplate> wasabiDefaultALCEntries = ACLServiceImpl.getDefaultACLEntries(locationNode, s);

		for (Iterator<WasabiACLEntryTemplate> iterator = wasabiDefaultALCEntries.iterator(); iterator.hasNext();) {
			WasabiACLEntryTemplate wasabiDefaultACLEntry = (WasabiACLEntryTemplate) iterator.next();
			wasabiDefaultACLEntriesDTO.add(TransferManager.convertWasabiACLEntryTemplate2DTO(wasabiDefaultACLEntry));
		}
		return wasabiDefaultACLEntriesDTO;
	}

	@Override
	public Vector<WasabiACLEntryTemplateDTO> getDefaultAclEntriesByType(WasabiLocationDTO wasabiLocation,
			WasabiType wasabiType) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node locationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(locationNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.getDefaultAclEntries()",
						"READ", "wasabiLocation"));
		/* Authorization - End */

		Vector<WasabiACLEntryTemplateDTO> wasabiDefaultACLEntriesDTO = new Vector<WasabiACLEntryTemplateDTO>();
		List<WasabiACLEntryTemplate> wasabiDefaultALCEntries = ACLServiceImpl.getDefaultACLEntriesByType(locationNode,
				wasabiType, s);

		for (Iterator<WasabiACLEntryTemplate> iterator = wasabiDefaultALCEntries.iterator(); iterator.hasNext();) {
			WasabiACLEntryTemplate wasabiDefaultACLEntry = (WasabiACLEntryTemplate) iterator.next();
			wasabiDefaultACLEntriesDTO.add(TransferManager.convertWasabiACLEntryTemplate2DTO(wasabiDefaultACLEntry));
		}
		return wasabiDefaultACLEntriesDTO;
	}

	@Deprecated
	@Override
	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTODeprecated wasabiACLEntry) {
		return wasabiACLEntry.getIdentity();
	}

	@Deprecated
	@Override
	public int getPermission(WasabiACLEntryDTODeprecated wasabiACLEntry) {
		return wasabiACLEntry.getPermission();
	}

	@Deprecated
	@Override
	public boolean isAllowance(WasabiACLEntryDTODeprecated wasabiACLEntry) {
		return wasabiACLEntry.isAllowance();
	}

	@Deprecated
	@Override
	public boolean isExplicitRight(WasabiACLEntryDTODeprecated wasabiACLEntry) {
		if (wasabiACLEntry.getInheritance())
			return false;
		else
			return true;
	}

	@Override
	public boolean isInheritanceAllowed(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSessionTx();
		Node objectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		String callerPrincipal = ctx.getCallerPrincipal().getName();

		/* Authorization - Begin */
		if (WasabiConstants.ACL_CHECK_ENABLE)
			if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, WasabiPermission.READ, s))
				throw new NoPermissionException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION, "ACLService.isInheritanceAllowed()",
						"READ", "wasabiObject"));
		/* Authorization - End */

		return ACLServiceImpl.getInheritance(objectNode);
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

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		perm[0] = permission;
		ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, perm, 0, 0, s);
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		perm[0] = permission;
		ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, perm, startTime, endTime, s);
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
		ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, permission, 0, 0, s);
	}

	@Deprecated
	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (startTime.length != endTime.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "startTime", "endTime"));
		}

		Session s = jcr.getJCRSessionTx();
		Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

		for (int i = 0; i < endTime.length; i++) {
			ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, permission, startTime[i], endTime[i], s);
		}
	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		ACLServiceImpl.removeDefault(wasabiLocationNode, wasabiType, permission, 0, 0);
	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			long startTime, long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
		ACLServiceImpl.removeDefault(wasabiLocationNode, wasabiType, permission, startTime, endTime);
	}

	@Override
	public void reset(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSessionTx();
		Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
		ACLServiceImpl.reset(wasabiObjectNode, s);
	}

	// TODO: impl. getDefaultACLEntriesByType
}
