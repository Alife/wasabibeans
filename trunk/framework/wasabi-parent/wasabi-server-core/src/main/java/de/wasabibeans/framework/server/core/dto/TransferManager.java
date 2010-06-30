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
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public abstract class TransferManager {

	private final String suffix = "DTO";
	private final String prefix = "Wasabi";

	protected WasabiLogger logger;
	protected Repository jcrRepository;

	protected TransferManager() {
		this.logger = WasabiLogger.getLogger(this.getClass());
		try {
			InitialContext jndiContext = new InitialContext();
			this.jcrRepository = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
		} catch (NamingException ne) {
			logger.error(WasabiExceptionMessages.JNDI_NO_CONTEXT, ne);
			throw new RuntimeException(ne);
		}
	}

	protected Session getJCRSession() {
		try {
			return jcrRepository.login(new SimpleCredentials("user", "pwd".toCharArray()));
		} catch (LoginException le) {
			logger.error(WasabiExceptionMessages.JCR_LOGIN_FAILURE, le);
			throw new RuntimeException(le);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T extends WasabiObjectDTO> T convertNode2DTO(Node wasabiObject) {
		if (wasabiObject == null) {
			logger.warn(WasabiExceptionMessages.TRANSFER_NODE2DTO_NULLNODE);
			return null;
		}
		Class<T> clazz = null;
		try {
			String wasabiObjectName = generateWasabiObjectDTOName(wasabiObject.getPrimaryNodeType().getName());
			clazz = (Class<T>) Class.forName(WasabiObjectDTO.class.getPackage().getName() + "." + wasabiObjectName);
		} catch (ClassNotFoundException cnfe) {
			logger.error(WasabiExceptionMessages.TRANSFER_NODE2DTO_REFLECTERROR, cnfe);
			throw new RuntimeException(cnfe);
		} catch (RepositoryException re) {
			logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			throw new RuntimeException(re);
		}
		WasabiObjectDTO dto = null;
		try {
			dto = clazz.newInstance();
			dto.setId(wasabiObject.getIdentifier());
		} catch (Exception e) {
			logger.error(WasabiExceptionMessages.TRANSFER_NODE2DTO_REFLECTERROR, e);
			throw new RuntimeException(e);
		}
		return (T) dto;
	}

	protected Node convertDTO2Node(WasabiObjectDTO wasabiObjectDTO, Session s) {
		if (wasabiObjectDTO != null) {
			if (s != null) {
				try {
					Node node = s.getNodeByIdentifier(wasabiObjectDTO.getId());
					return node;
				} catch (ItemNotFoundException ie) {
					logger.warn(WasabiExceptionMessages.TRANSFER_DTO2NODE_FAILURE, ie);
					throw new ObjectDoesNotExistException(wasabiObjectDTO);
				} catch (RepositoryException re) {
					logger.error(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
					throw new RuntimeException(re);
				}
			} else {
				logger.error(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLSESSION);
				throw new IllegalArgumentException(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLSESSION);
			}
		} else {
			logger.error(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLDTO);
			throw new IllegalArgumentException(WasabiExceptionMessages.TRANSFER_DTO2NODE_NULLDTO);
		}
	}

	private String generateWasabiObjectDTOName(String nodeTypeName) {
		String[] tmp = nodeTypeName.split(":");
		String firstLetter = tmp[1].substring(0, 1).toUpperCase(Locale.ENGLISH);
		return prefix + firstLetter + tmp[1].substring(1) + suffix;
	}
}
