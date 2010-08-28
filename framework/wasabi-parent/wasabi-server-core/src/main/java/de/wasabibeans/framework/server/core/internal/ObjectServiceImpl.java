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

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class ObjectServiceImpl {

	public static String getName(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getName();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static String getUUID(Node objectNode) throws UnexpectedInternalProblemException {
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

	public static void rename(Node objectNode, String name, String callerPrincipal)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException {
		try {
			objectNode.getSession().move(objectNode.getPath(), objectNode.getParent().getPath() + "/" + name);
			ObjectServiceImpl.modified(objectNode, objectNode.getSession(), callerPrincipal, false);
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

	public static Node getCreatedBy(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.CREATED_BY).getNode();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Date getCreatedOn(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.CREATED_ON).getDate().getTime();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Node getModifiedBy(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.MODIFIED_BY).getNode();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Date getModifiedOn(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.MODIFIED_ON).getDate().getTime();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
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

	public static void setCreatedBy(Node objectNode, Node userNode) throws UnexpectedInternalProblemException {
		try {
			objectNode.setProperty(WasabiNodeProperty.CREATED_BY, userNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setCreatedOn(Node objectNode, Date creationTime) throws UnexpectedInternalProblemException {
		if (creationTime == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"creationTime"));
		}
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(creationTime);
			objectNode.setProperty(WasabiNodeProperty.CREATED_ON, cal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setModifiedBy(Node objectNode, Node userNode) throws UnexpectedInternalProblemException {
		try {
			objectNode.setProperty(WasabiNodeProperty.MODIFIED_BY, userNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setModifiedOn(Node objectNode, Date modificationTime) throws UnexpectedInternalProblemException {
		if (modificationTime == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"modificationTime"));
		}
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(modificationTime);
			objectNode.setProperty(WasabiNodeProperty.MODIFIED_ON, cal);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static long getOptLockId(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			return objectNode.getProperty(WasabiNodeProperty.OPT_LOCK_ID).getLong();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setOptLockId(Node objectNode, long optLockId) throws UnexpectedInternalProblemException {
		try {
			objectNode.setProperty(WasabiNodeProperty.OPT_LOCK_ID, optLockId);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	/**
	 * Sets the wasabi:createdOn, wasabi:createdBy, wasabi:modifiedOn, wasabi:modifiedBy, and wasabi:version properties
	 * for the given {@code objectNode}. If the the given {@code callerPrincipal} is {@code null} and the given {@code
	 * nullEntryEnabled} is {@code true}, the properties wasabi:createdBy and wasabi:modifiedBy will be set to {@code
	 * null}. If the the given {@code callerPrincipal} is {@code null} and the given {@code nullEntryEnabled} is {@code
	 * false}, none of the properties will be set.
	 * 
	 * @param objectNode
	 * @param s
	 * @param callerPrincipal
	 * @param nullEntryEnabled
	 * @throws UnexpectedInternalProblemException
	 */
	public static void created(Node objectNode, Session s, String callerPrincipal, boolean nullEntryEnabled)
			throws UnexpectedInternalProblemException {
		Node currentUser = null;
		if (callerPrincipal != null) {
			currentUser = UserServiceImpl.getUserByName(callerPrincipal, s);
		} else {
			if (!nullEntryEnabled) {
				return;
			}
		}
		Date timestamp = Calendar.getInstance().getTime();
		ObjectServiceImpl.setCreatedOn(objectNode, timestamp);
		ObjectServiceImpl.setCreatedBy(objectNode, currentUser);
		ObjectServiceImpl.setModifiedOn(objectNode, timestamp);
		ObjectServiceImpl.setModifiedBy(objectNode, currentUser);
		ObjectServiceImpl.setOptLockId(objectNode, 0);
	}

	/**
	 * Sets the wasabi:modifiedOn, wasabi:modifiedBy, and wasabi:version properties for the given {@code objectNode}. If
	 * the the given {@code callerPrincipal} is {@code null} and the given {@code nullEntryEnabled} is {@code true}, the
	 * property wasabi:modifiedBy will be set to {@code null}. If the the given {@code callerPrincipal} is {@code null}
	 * and the given {@code nullEntryEnabled} is {@code false}, none of the properties will be set.
	 * 
	 * @param objectNode
	 * @param s
	 * @param callerPrincipal
	 * @param nullEntryEnabled
	 * @throws UnexpectedInternalProblemException
	 */
	public static void modified(Node objectNode, Session s, String callerPrincipal, boolean nullEntryEnabled)
			throws UnexpectedInternalProblemException {
		Node currentUser = null;
		if (callerPrincipal != null) {
			currentUser = UserServiceImpl.getUserByName(callerPrincipal, s);
		} else {
			if (!nullEntryEnabled) {
				return;
			}
		}
		ObjectServiceImpl.setModifiedOn(objectNode, Calendar.getInstance().getTime());
		ObjectServiceImpl.setModifiedBy(objectNode, currentUser);
		long currentVersion = ObjectServiceImpl.getOptLockId(objectNode);
		ObjectServiceImpl.setOptLockId(objectNode, ++currentVersion);
	}

	public static void setRightsActive(Node objectNode, boolean rightsActive) {
		// TODO Auto-generated method stub
	}
}
