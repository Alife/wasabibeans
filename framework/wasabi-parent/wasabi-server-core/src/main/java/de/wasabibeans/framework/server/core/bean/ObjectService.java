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

import java.util.Date;
import java.util.Vector;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.ObjectServiceLocal;
import de.wasabibeans.framework.server.core.remote.ObjectServiceRemote;
import de.wasabibeans.framework.server.core.util.JcrConnector;

/**
 * Class, that implements the internal access on WasabiObject objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "ObjectService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ObjectService implements ObjectServiceLocal, ObjectServiceRemote {

	@Resource
	protected SessionContext ctx;

	protected JcrConnector jcr;

	public ObjectService() {
		this.jcr = JcrConnector.getJCRConnector();
	}

	public String getName(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException {
		Session s = jcr.getJCRSession();
		try {
			Node objectNode = TransferManager.convertDTO2Node(object, s);
			String callerPrincipal = ctx.getCallerPrincipal().getName();

			/* Authorization - Begin */
			if (WasabiConstants.ACL_CHECK_ENABLE)
				if (!WasabiAuthorizer.authorize(objectNode, callerPrincipal, new int[] { WasabiPermission.VIEW,
						WasabiPermission.READ }, s))
					throw new NoPermissionException(WasabiExceptionMessages.AUTHORIZATION_NO_PERMISSION);
			/* Authorization - End */

			return ObjectServiceImpl.getName(objectNode);
		} finally {
			s.logout();
		}
	}

	public String getUUID(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = jcr.getJCRSession();
		try {
			Node objectNode = TransferManager.convertDTO2Node(object, s);
			return ObjectServiceImpl.getUUID(objectNode);
		} finally {
			s.logout();
		}
	}

	@Override
	public boolean exists(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WasabiUserDTO getCreatedBy(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreatedOn(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiUserDTO getModifiedBy(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getModifiedOn(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRightsActive(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCreatedBy(WasabiObjectDTO object, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCreatedOn(WasabiObjectDTO object, Date creationTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setModifiedBy(WasabiObjectDTO object, WasabiUserDTO user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setModifiedOn(WasabiObjectDTO object, Date modificationTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRightsActive(WasabiObjectDTO object, boolean rightsActive) {
		// TODO Auto-generated method stub

	}
}
