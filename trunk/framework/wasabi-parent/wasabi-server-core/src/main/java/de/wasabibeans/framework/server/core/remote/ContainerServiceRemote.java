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

import de.wasabibeans.framework.server.core.dto.WasabiContainerDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiContainerDTO objects.
 */
@Remote
public interface ContainerServiceRemote extends ObjectServiceRemote {

	public WasabiContainerDTO create(String name, WasabiLocationDTO environment) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException;

	public WasabiContainerDTO getContainerByName(WasabiLocationDTO location, String name)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainers(WasabiLocationDTO location) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiContainerDTO> getContainersByNamePattern(WasabiLocationDTO environment, String pattern)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public WasabiValueDTO getEnvironment(WasabiContainerDTO container) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public void move(WasabiContainerDTO container, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException;

	public void remove(WasabiContainerDTO container) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void rename(WasabiContainerDTO container, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException;
}
