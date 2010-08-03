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

import java.util.Locale;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class TransferManager {

	private static WasabiLogger logger = WasabiLogger.getLogger(TransferManager.class);

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
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_NODE2DTO_REFLECTERROR, e);
		}
		return (T) dto;
	}

	public static Node convertDTO2Node(WasabiObjectDTO wasabiObjectDTO, Session s) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException {
		if (wasabiObjectDTO != null) {
			if (s != null) {
				try {
					Node node = s.getNodeByIdentifier(wasabiObjectDTO.getId());
					return node;
				} catch (ItemNotFoundException ie) {
					throw new ObjectDoesNotExistException(WasabiExceptionMessages.TRANSFER_DTO2NODE_FAILURE,
							wasabiObjectDTO, ie);
				} catch (RepositoryException re) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
				}
			} else {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLSESSION);
			}
		} else {
			throw new IllegalArgumentException(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLDTO);
		}
	}

	public static WasabiACLEntryTemplateDTO convertWasabiACLEntryTemplate2DTO(
			WasabiACLEntryTemplate wasabiDefaultACLEntry) {
		WasabiACLEntryTemplateDTO dto = new WasabiACLEntryTemplateDTO();
		dto.setId(wasabiDefaultACLEntry.getId());
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
		if(wasabiACLEntry.getInheritance_Id().isEmpty()) 
			dto.setInheritance(false);
		else 
			dto.setInheritance(true);
		return dto;
	}

	private static String generateWasabiObjectDTOName(String nodeTypeName) {
		String[] tmp = nodeTypeName.split(":");
		String firstLetter = tmp[1].substring(0, 1).toUpperCase(Locale.ENGLISH);
		return WasabiConstants.DTO_PREFIX + firstLetter + tmp[1].substring(1) + WasabiConstants.DTO_SUFFIX;
	}
}
