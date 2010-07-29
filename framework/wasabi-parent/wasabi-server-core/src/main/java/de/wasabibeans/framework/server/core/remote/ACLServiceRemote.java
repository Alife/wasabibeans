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

import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Remote
public interface ACLServiceRemote {

	public void activateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	@Deprecated
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	@Deprecated
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void deactivateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public boolean isInheritanceAllowed(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void reset(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	@Deprecated
	public int getPermission(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public boolean isAllowance(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public boolean isExplicitRight(WasabiACLEntryDTODeprecated wasabiACLEntry);

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			long startTime, long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	@Deprecated
	public Vector<WasabiACLEntryDTODeprecated> getACLEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	@Deprecated
	public Vector<WasabiACLEntryDTODeprecated> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiACLEntryDTO> getAclEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiACLEntryDTO> getAclEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;
}