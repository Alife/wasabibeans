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

import java.util.Collection;

import javax.ejb.Stateless;
import javax.jcr.Node;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.local.ACLServiceLocal;
import de.wasabibeans.framework.server.core.remote.ACLServiceRemote;

@SecurityDomain("wasabi")
@Stateless(name = "ACLService")
public class ACLService extends WasabiService implements ACLServiceLocal, ACLServiceRemote {

	@Override
	public void activateInheritance(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node wasabiObjectNode = tm.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = tm.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, perm, allow, 0, 0);
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node wasabiObjectNode = tm.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = tm.convertDTO2Node(wasabiIdentity, s);
		int[] perm = new int[1];
		boolean[] allow = new boolean[1];
		perm[0] = permission;
		allow[0] = allowance;
		ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, perm, allow, startTime, endTime);
	}

	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node wasabiObjectNode = tm.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = tm.convertDTO2Node(wasabiIdentity, s);
		ACLServiceImpl.create(wasabiObjectNode, wasabiIdentityNode, permission, allowance, 0, 0);
	}

	@Deprecated
	@Override
	public void create(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node wasabiObjectNode = tm.convertDTO2Node(wasabiObject, s);
		Node wasabiIdentityNode = tm.convertDTO2Node(wasabiIdentity, s);
		for (int i = 0; i < endTime.length; i++) {
			ACLServiceImpl
					.create(wasabiObjectNode, wasabiIdentityNode, permission, allowance, startTime[i], endTime[i]);
		}
	}

	@Override
	public void createDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivateInheritance(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<WasabiACLEntryDTO> getACLEntries(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<WasabiACLEntryDTO> getACLEntriesByIdentity(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPermission(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAllowance(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isExplicitRight(WasabiACLEntryDTO wasabiACLEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInheritanceAllowed(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime) throws UnexpectedInternalProblemException, ObjectDoesNotExistException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDefault(WasabiLocationDTO wasabiLocation, WasabiIdentityDTO wasabiIdentity, int[] permission) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset(WasabiObjectDTO wasabiObject) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException {
		Session s = getJCRSession();
		Node wasabiObjectNode = tm.convertDTO2Node(wasabiObject, s);
		ACLServiceImpl.reset(wasabiObjectNode);
	}

	@Override
	public void showAllACLEntries(WasabiObjectDTO wasabiObject) {
		// TODO Auto-generated method stub

	}

}
