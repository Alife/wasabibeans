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

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.local.ACLServiceLocal;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;
import de.wasabibeans.framework.server.core.util.SessionHandler;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;

@SecurityDomain("wasabi")
@Stateless(name = "ACLService")
public class ACLService implements ACLServiceLocal, ACLServiceRemote {

	@Resource
	SessionContext ctx;

	private SessionHandler sesHa;

	public ACLService() {
		this.sesHa = new SessionHandler();
	}

	@Override
	public void activateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			ACLServiceImpl.setInheritance(wasabiObjectNode, true);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE + "::"
					+ re.toString(), re);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			int[] perm = new int[1];
			boolean[] allow = new boolean[1];
			perm[0] = permission;
			allow[0] = allowance;
			ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, perm, allow, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			int[] perm = new int[1];
			boolean[] allow = new boolean[1];
			perm[0] = permission;
			allow[0] = allowance;
			ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, perm, allow, startTime, endTime);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, permission, allowance, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Deprecated
	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		if (startTime.length != endTime.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "startTime", "endTime"));
		}

		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

			for (int i = 0; i < endTime.length; i++) {
				ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, permission, allowance, startTime[i],
						endTime[i]);
			}
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);

			if (permission.length != allowance.length) {
				throw new IllegalArgumentException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
			}

			ACLServiceImpl.createDefault(wasabiLocationNode, wasabiType, permission, allowance, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);

			if (!wasabiLocationNode.equals(WasabiNodeType.WASABI_ROOM)
					|| !wasabiLocationNode.equals(WasabiNodeType.WASABI_ROOM)) {
				throw new IllegalArgumentException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.INTERNAL_TYPE_CONFLICT));
			}

			if (permission.length != allowance.length) {
				throw new IllegalArgumentException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
			}

			ACLServiceImpl.createDefault(wasabiLocationNode, wasabiType, permission, allowance, startTime, endTime);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void deactivateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			ACLServiceImpl.setInheritance(wasabiObjectNode, false);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Deprecated
	@Override
	public Vector<WasabiACLEntryDTODeprecated> getACLEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);

			Vector<WasabiACLEntryDTODeprecated> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTODeprecated>();
			Vector<WasabiACLEntryDeprecated> wasabiALCEntries = ACLServiceImpl.getACLEntriesDeprecated(
					wasabiObjectNode, null, s);

			for (Iterator<WasabiACLEntryDeprecated> iterator = wasabiALCEntries.iterator(); iterator.hasNext();) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = (WasabiACLEntryDeprecated) iterator.next();
				wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDeprecated2DTO(wasabiACLEntryDeprecated));
			}
			return wasabiACLEntriesDTO;
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public Vector<WasabiACLEntryDTO> getAclEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);

			Vector<WasabiACLEntryDTO> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTO>();
			List<WasabiACLEntry> wasabiALCEntries = ACLServiceImpl.getAclEntries(wasabiObjectNode);

			for (Iterator<WasabiACLEntry> iterator = wasabiALCEntries.iterator(); iterator.hasNext();) {
				WasabiACLEntry wasabiACLEntry = (WasabiACLEntry) iterator.next();
				wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDTO(wasabiACLEntry));
			}

			return wasabiACLEntriesDTO;
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public Vector<WasabiACLEntryDTODeprecated> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

			Vector<WasabiACLEntryDTODeprecated> wasabiACLEntriesDTO = new Vector<WasabiACLEntryDTODeprecated>();
			Vector<WasabiACLEntryDeprecated> wasabiALCEntries = ACLServiceImpl.getACLEntriesDeprecated(
					wasabiObjectNode, wasabiIdentityNode, s);

			for (Iterator iterator = wasabiALCEntries.iterator(); iterator.hasNext();) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = (WasabiACLEntryDeprecated) iterator.next();
				wasabiACLEntriesDTO.add(TransferManager.convertWasabiACLEntryDeprecated2DTO(wasabiACLEntryDeprecated));
			}
			return wasabiACLEntriesDTO;
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public Vector<WasabiACLEntryDTO> getAclEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

			Vector<WasabiACLEntryDTO> wasabiACLEntriesByIdentityDTO = new Vector<WasabiACLEntryDTO>();
			List<WasabiACLEntry> wasabiALCEntriesByIdentity = ACLServiceImpl.getAclEntriesByIdentity(wasabiObjectNode,
					wasabiIdentityNode);

			for (Iterator<WasabiACLEntry> iterator = wasabiALCEntriesByIdentity.iterator(); iterator.hasNext();) {
				WasabiACLEntry wasabiACLEntryByIdentity = (WasabiACLEntry) iterator.next();
				wasabiACLEntriesByIdentityDTO.add(TransferManager.convertWasabiACLEntryDTO(wasabiACLEntryByIdentity));
			}

			return wasabiACLEntriesByIdentityDTO;
		} finally {
			sesHa.releaseSession(ctx);
		}
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
		return wasabiACLEntry.getInheritance();
	}

	@Override
	public boolean isInheritanceAllowed(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			return ACLServiceImpl.getInheritance(wasabiObjectNode);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			int[] perm = new int[1];
			perm[0] = permission;
			ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, perm, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			int[] perm = new int[1];
			perm[0] = permission;
			ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, perm, startTime, endTime);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);
			ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, permission, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Deprecated
	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		if (startTime.length != endTime.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "startTime", "endTime"));
		}

		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			Node wasabiIdentityNode = TransferManager.convertDTO2Node(wasabiIdentity, s);

			for (int i = 0; i < endTime.length; i++) {
				ACLServiceImpl.remove(wasabiObjectNode, wasabiIdentityNode, permission, startTime[i], endTime[i]);
			}
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
			ACLServiceImpl.removeDefault(wasabiLocationNode, wasabiType, permission, 0, 0);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			long startTime, long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);
			ACLServiceImpl.removeDefault(wasabiLocationNode, wasabiType, permission, startTime, endTime);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public void reset(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		// TODO: Reimpl
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiObjectNode = TransferManager.convertDTO2Node(wasabiObject, s);
			ACLServiceImpl.reset(wasabiObjectNode);
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

	@Override
	public Vector<WasabiACLEntryTemplateDTO> getDefaultAclEntries(WasabiLocationDTO wasabiLocation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = sesHa.getSession(ctx);
		try {
			Node wasabiLocationNode = TransferManager.convertDTO2Node(wasabiLocation, s);

			Vector<WasabiACLEntryTemplateDTO> wasabiDefaultACLEntriesDTO = new Vector<WasabiACLEntryTemplateDTO>();
			List<WasabiACLEntryTemplate> wasabiDefaultALCEntries = ACLServiceImpl.getDefaultACLEntries(
					wasabiLocationNode, s);

			for (Iterator<WasabiACLEntryTemplate> iterator = wasabiDefaultALCEntries.iterator(); iterator.hasNext();) {
				WasabiACLEntryTemplate wasabiDefaultACLEntry = (WasabiACLEntryTemplate) iterator.next();
				wasabiDefaultACLEntriesDTO
						.add(TransferManager.convertWasabiACLEntryTemplate2DTO(wasabiDefaultACLEntry));
			}
			return wasabiDefaultACLEntriesDTO;
		} finally {
			sesHa.releaseSession(ctx);
		}
	}

}
