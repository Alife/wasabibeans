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
package de.wasabibeans.framework.server.core.internal;

import java.util.Date;
import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class ObjectServiceImpl {

	public static String getName(Node objectNode) throws UnexpectedInternalProblemException {
		if (objectNode == null) {
			return "";
		}
		try {
			return objectNode.getName();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getUUID(Node objectNode) throws UnexpectedInternalProblemException {
		if (objectNode == null) {
			return "";
		}
		try {
			return objectNode.getIdentifier();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getEnvironment(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getParent().getParent();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void rename(Node objectNode, String name) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		try {
			objectNode.getSession().move(objectNode.getPath(), objectNode.getParent().getPath() + "/" + name);
		} catch (ItemExistsException iee) {
			try {
				String what = objectNode.getPrimaryNodeType().getName();
				throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
						WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, what, name), name, iee);
			} catch (RepositoryException re) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			objectNode.remove();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean exists(Node objectNode) {
		// TODO Auto-generated method stub
		return false;
	}

	public static WasabiUserDTO getCreatedBy(Node objectNode) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Date getCreatedOn(Node objectNode) {
		// TODO Auto-generated method stub
		return null;
	}

	public static WasabiUserDTO getModifiedBy(Node objectNode) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Date getModifiedOn(Node objectNode) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Vector<WasabiObjectDTO> getObjectsByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean isRightsActive(Node objectNode) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void setCreatedBy(Node objectNode, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	public static void setCreatedOn(Node objectNode, Date creationTime) {
		// TODO Auto-generated method stub

	}

	public static void setModifiedBy(Node objectNode, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	public static void setModifiedOn(Node objectNode, Date modificationTime) {
		// TODO Auto-generated method stub

	}

	public static void setRightsActive(Node objectNode, boolean rightsActive) {
		// TODO Auto-generated method stub
	}
}
