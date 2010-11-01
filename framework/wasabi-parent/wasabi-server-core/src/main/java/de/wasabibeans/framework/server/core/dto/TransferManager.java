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

package de.wasabibeans.framework.server.core.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;
import de.wasabibeans.framework.server.core.util.WasabiCertificateHandle;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class TransferManager {

	private static WasabiLogger logger = WasabiLogger.getLogger(TransferManager.class);

	public static Node convertDTO2Node(WasabiObjectDTO wasabiObjectDTO, Session s) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		if (wasabiObjectDTO != null) {
			if (s != null) {
				try {
					Node node = s.getNodeByIdentifier(wasabiObjectDTO.getId());
					return node;
				} catch (ItemNotFoundException ie) {
					throw new ObjectDoesNotExistException(WasabiExceptionMessages.get(
							WasabiExceptionMessages.OBJECT_DNE_ID, wasabiObjectDTO.getId()), ie);
				} catch (RepositoryException re) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
				}
			} else {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLSESSION);
			}
		} else {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INVALID_ARG_NULL,
					"DTO"));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends WasabiObjectDTO> T convertNode2DTO(Node wasabiObject)
			throws UnexpectedInternalProblemException {
		if (wasabiObject == null) {
			logger.warn(WasabiExceptionMessages.TRANSFER_NODE2DTO_NULLNODE);
			return null;
		}
		WasabiObjectDTO dto = null;
		try {
			String wasabiObjectName = generateWasabiObjectDTOName(wasabiObject.getPrimaryNodeType().getName());
			Class<T> clazz = (Class<T>) Class.forName(WasabiObjectDTO.class.getPackage().getName() + "."
					+ wasabiObjectName);
			dto = clazz.newInstance();
			dto.setId(wasabiObject.getIdentifier());
			dto.setOptLockId(ObjectServiceImpl.getOptLockId(wasabiObject));
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_NODE2DTO_REFLECTERROR, e);
		}
		return (T) dto;
	}

	@SuppressWarnings("unchecked")
	public static <T extends WasabiObjectDTO> T convertNode2DTO(Node wasabiObject, WasabiObjectDTO dtoOfParent)
			throws UnexpectedInternalProblemException {
		T dto = (T) convertNode2DTO(wasabiObject);
		String lockToken = dtoOfParent != null ? dtoOfParent.getLockToken() : null;
		if (lockToken != null && dtoOfParent.isDeepLock()) {
			return enrichWithLockToken(dto, lockToken, true);
		}
		return dto;
	}

	public static WasabiValueDTO convertValue2DTO(Node wasabiObject, Long optLockId)
			throws UnexpectedInternalProblemException {
		if (optLockId != null) {
			WasabiObjectDTO objectDTO = convertNode2DTO(wasabiObject);
			return new WasabiValueDTO(objectDTO, optLockId);
		} else {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_VALUE2DTO_NULLVERSION);
		}
	}

	public static WasabiValueDTO convertValue2DTO(Serializable value, Long optLockId)
			throws UnexpectedInternalProblemException {
		if (optLockId != null) {
			return new WasabiValueDTO(value, optLockId);
		} else {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_VALUE2DTO_NULLVERSION);
		}
	}

	public static WasabiVersionDTO convertVersion2DTO(Version version, VersionHistory versionHistory)
			throws UnexpectedInternalProblemException {
		try {
			String label = versionHistory.getVersionLabels(version)[0];
			Date creationDate = new Date(Long.parseLong(label));
			String comment = null;
			try {
				comment = version.getFrozenNode().getProperty(WasabiNodeProperty.VERSION_COMMENT).getString();
			} catch (PathNotFoundException pnfe) {
				// no comment set
			}
			return new WasabiVersionDTO(label, comment, creationDate);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static WasabiACLEntryDTODeprecated convertWasabiACLEntryDeprecated2DTO(
			WasabiACLEntryDeprecated wasabiACLEntryDeprecated) {
		WasabiACLEntryDTODeprecated dto = new WasabiACLEntryDTODeprecated();
		dto.setId(wasabiACLEntryDeprecated.getId());
		dto.setIdentity(wasabiACLEntryDeprecated.getWasabiIdentity());
		dto.setIsAllowance(wasabiACLEntryDeprecated.getAllowance());
		dto.setPermission(wasabiACLEntryDeprecated.getPermission());
		dto.setInheritance(wasabiACLEntryDeprecated.getInheritance());
		return dto;
	}

	public static WasabiACLEntryDTO convertWasabiACLEntryDTO(WasabiACLEntry wasabiACLEntry) {
		WasabiACLEntryDTO dto = new WasabiACLEntryDTO();
		dto.setId(wasabiACLEntry.getId());
		dto.setUserId(wasabiACLEntry.getUser_Id());
		dto.setGroupId(wasabiACLEntry.getGroup_Id());
		dto.setParentId(wasabiACLEntry.getParent_Id());
		dto.setView(wasabiACLEntry.getView());
		dto.setRead(wasabiACLEntry.getRead());
		dto.setInsert(wasabiACLEntry.getInsert());
		dto.setWrite(wasabiACLEntry.getWrite());
		dto.setComment(wasabiACLEntry.getComment());
		dto.setExecute(wasabiACLEntry.getExecute());
		dto.setGrant(wasabiACLEntry.getGrant());
		dto.setStartTime(wasabiACLEntry.getStart_Time());
		dto.setEndTime(wasabiACLEntry.getEnd_Time());
		dto.setInheritanceId(wasabiACLEntry.getInheritance_Id());
		if (wasabiACLEntry.getInheritance_Id().isEmpty())
			dto.setInheritance(false);
		else
			dto.setInheritance(true);
		return dto;
	}

	public static WasabiACLEntryTemplateDTO convertWasabiACLEntryTemplate2DTO(
			WasabiACLEntryTemplate wasabiDefaultACLEntry) {
		WasabiACLEntryTemplateDTO dto = new WasabiACLEntryTemplateDTO();
		dto.setId(wasabiDefaultACLEntry.getId());
		dto.setLocationID(wasabiDefaultACLEntry.getLocation_Id());
		dto.setIdentityID(wasabiDefaultACLEntry.getIdentity_Id());
		dto.setWasabiType(wasabiDefaultACLEntry.getWasabi_Type());
		dto.setView(wasabiDefaultACLEntry.getView());
		dto.setRead(wasabiDefaultACLEntry.getRead());
		dto.setInsert(wasabiDefaultACLEntry.getInsert());
		dto.setWrite(wasabiDefaultACLEntry.getWrite());
		dto.setComment(wasabiDefaultACLEntry.getComment());
		dto.setExecute(wasabiDefaultACLEntry.getExecute());
		dto.setGrant(wasabiDefaultACLEntry.getGrant());
		dto.setStartTime(wasabiDefaultACLEntry.getStart_Time());
		dto.setEndTime(wasabiDefaultACLEntry.getEnd_Time());
		return dto;
	}

	public static WasabiCertificateDTO convertWasabiCertificate2DTO(WasabiCertificateHandle certificate,
			WasabiUserDTO user, WasabiObjectDTO object) {
		WasabiCertificateDTO dto = new WasabiCertificateDTO();
		dto.setId(certificate.getID());
		dto.setObject(object);
		dto.setUser(user);
		dto.setPermission(certificate.getPermission());
		return dto;
	}

	public static <T extends WasabiObjectDTO> T enrichWithLockToken(T wasabiObjectDTO, String lockToken, boolean isDeep) {
		wasabiObjectDTO.setLockToken(lockToken, isDeep);
		return wasabiObjectDTO;
	}

	private static String generateWasabiObjectDTOName(String nodeTypeName) {
		String[] tmp = nodeTypeName.split(":");
		String firstLetter = tmp[1].substring(0, 1).toUpperCase(Locale.ENGLISH);
		return WasabiConstants.DTO_PREFIX + firstLetter + tmp[1].substring(1) + WasabiConstants.DTO_SUFFIX;
	}

	public static <T extends WasabiObjectDTO> T removeLockToken(T wasabiObjectDTO) {
		wasabiObjectDTO.setLockToken(null, false);
		return wasabiObjectDTO;
	}
}
