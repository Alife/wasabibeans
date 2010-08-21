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

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiGroupDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiGroupDTO objects.
 */
@Remote
public interface GroupServiceRemote extends ObjectServiceRemote {

	public void addMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public WasabiGroupDTO create(String name, WasabiGroupDTO parentGroup) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException;

	public WasabiValueDTO getDisplayName(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public WasabiGroupDTO getGroupByName(String groupName) throws UnexpectedInternalProblemException;

	public Vector<WasabiGroupDTO> getGroupsByDisplayName(String displayName) throws UnexpectedInternalProblemException;

	public WasabiUserDTO getMemberByName(WasabiGroupDTO group, String userName) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiUserDTO> getMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiUserDTO> getAllMembers(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public WasabiValueDTO getParentGroup(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public WasabiGroupDTO getSubGroupByName(WasabiGroupDTO group, String name) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public Vector<WasabiGroupDTO> getSubGroups(WasabiGroupDTO group) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public boolean isDirectMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public boolean isMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public void move(WasabiGroupDTO group, WasabiGroupDTO newParentGroup, Long version)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException;

	public void remove(WasabiGroupDTO group) throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public void removeMember(WasabiGroupDTO group, WasabiUserDTO user) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public void rename(WasabiGroupDTO group, String name, Long version) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException;

	public void setDisplayName(WasabiGroupDTO group, String displayName, Long version)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException;

	public Vector<WasabiGroupDTO> getAllGroups() throws UnexpectedInternalProblemException;

	public Vector<WasabiGroupDTO> getTopLevelGroups() throws UnexpectedInternalProblemException;
}
