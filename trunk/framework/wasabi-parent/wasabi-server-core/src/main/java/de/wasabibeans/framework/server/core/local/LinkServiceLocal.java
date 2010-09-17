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

package de.wasabibeans.framework.server.core.local;

import java.util.Date;
import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiLinkDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the local access on WasabiLinkDTO objects.
 */
@Local
public interface LinkServiceLocal extends ObjectServiceLocal {

	public WasabiLinkDTO create(String name, WasabiObjectDTO destination, WasabiLocationDTO environment)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException;

	public WasabiValueDTO getDestination(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, ObjectDoesNotExistException;

	public WasabiValueDTO getEnvironment(WasabiLinkDTO link) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiLinkDTO getLinkByName(WasabiLocationDTO location, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiLinkDTO> getLinks(WasabiLocationDTO location) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByCreationDate(WasabiLocationDTO environment, Date startDate, Date endDate,
			int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate, Date endDate)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public Vector<WasabiLinkDTO> getLinksByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiLinkDTO> getLinksOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void move(WasabiLinkDTO link, WasabiLocationDTO newEnvironment, Long optLockId)
			throws ObjectAlreadyExistsException, UnexpectedInternalProblemException, ConcurrentModificationException,
			ObjectDoesNotExistException;

	public void remove(WasabiLinkDTO link) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void rename(WasabiLinkDTO link, String name, Long optLockId) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, ObjectDoesNotExistException, ConcurrentModificationException;

	public void setDestination(WasabiLinkDTO link, WasabiObjectDTO object, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectDoesNotExistException;
}
