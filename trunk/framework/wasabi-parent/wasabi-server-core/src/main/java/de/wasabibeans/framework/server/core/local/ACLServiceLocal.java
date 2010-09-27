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

import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTODeprecated;
import de.wasabibeans.framework.server.core.dto.WasabiACLEntryTemplateDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface ACLServiceLocal {

	public void activateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;
	
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void deactivateInheritance(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public Vector<WasabiACLEntryDTO> getAclEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	@Deprecated
	public Vector<WasabiACLEntryDTODeprecated> getACLEntries(WasabiObjectDTO wasabiObject)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public Vector<WasabiACLEntryDTO> getAclEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	@Deprecated
	public Vector<WasabiACLEntryDTODeprecated> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	public Vector<WasabiACLEntryTemplateDTO> getDefaultAclEntries(WasabiLocationDTO wasabiLocation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;
	
	public Vector<WasabiACLEntryTemplateDTO> getDefaultAclEntriesByType(WasabiLocationDTO wasabiLocation,
			WasabiType wasabiType) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	@Deprecated
	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public int getPermission(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public boolean isAllowance(WasabiACLEntryDTODeprecated wasabiACLEntry);

	@Deprecated
	public boolean isExplicitRight(WasabiACLEntryDTODeprecated wasabiACLEntry);

	public boolean isInheritanceAllowed(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	@Deprecated
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, NoPermissionException;

	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiType wasabiType, int[] permission,
			long startTime, long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			NoPermissionException;

	public void reset(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;
}