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

package de.wasabibeans.framework.server.core.remote;

import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiObjectDTO objects.
 */
@Remote
public interface ObjectServiceRemote {

	public boolean exists(WasabiObjectDTO object) throws UnexpectedInternalProblemException;

	public WasabiValueDTO getCreatedBy(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getCreatedOn(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getModifiedBy(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getModifiedOn(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public boolean isRightsActive(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiValueDTO getName(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public Vector<WasabiObjectDTO> getObjectsByAttributeName(String attributeName)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiObjectDTO> getObjectsByCreator(WasabiUserDTO creator)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiObjectDTO> getObjectsByModifier(WasabiUserDTO modifier)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiObjectDTO> getObjectsByName(String name) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public String getUUID(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void setCreatedBy(WasabiObjectDTO object, WasabiUserDTO user, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException;

	public void setCreatedOn(WasabiObjectDTO object, Date creationTime, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException;

	public void setModifiedBy(WasabiObjectDTO object, WasabiUserDTO user, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ConcurrentModificationException;

	public void setModifiedOn(WasabiObjectDTO object, Date modificationTime, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException;

	public void setRightsActive(WasabiObjectDTO object, boolean rightsActive)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;
}
