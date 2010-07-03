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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public abstract class TransferManager {

	private static final String suffix = "DTO";
	private static final String prefix = "Wasabi";

	protected WasabiLogger logger;
	private Repository jcrRepository;

	public TransferManager() {
		this.logger = WasabiLogger.getLogger(this.getClass());
	}
	
	protected Repository getJCRRepository() throws UnexpectedInternalProblemException {
		try {
			if (this.jcrRepository == null) {
				InitialContext jndiContext = new InitialContext();
				this.jcrRepository = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
			}
			return this.jcrRepository;
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JNDI_NO_CONTEXT, ne);
		}
	}

	protected Session getJCRSession() throws UnexpectedInternalProblemException {
		try {
			return getJCRRepository().login(new SimpleCredentials("user", "pwd".toCharArray()));
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T extends WasabiObjectDTO> T convertNode2DTO(Node wasabiObject)
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

	protected Node convertDTO2Node(WasabiObjectDTO wasabiObjectDTO, Session s) throws ObjectDoesNotExistException,
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

	private String generateWasabiObjectDTOName(String nodeTypeName) {
		String[] tmp = nodeTypeName.split(":");
		String firstLetter = tmp[1].substring(0, 1).toUpperCase(Locale.ENGLISH);
		return prefix + firstLetter + tmp[1].substring(1) + suffix;
	}
}
