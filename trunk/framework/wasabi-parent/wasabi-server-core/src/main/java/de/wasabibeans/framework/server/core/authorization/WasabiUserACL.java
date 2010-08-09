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

package de.wasabibeans.framework.server.core.authorization;

import javax.jcr.Node;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;

public class WasabiUserACL {

	public static void ACLEntryForCreate(Node userNode, Node homeRoomNode, Node callerPrincipalNode,
			String callerPrincipal, Session s) throws UnexpectedInternalProblemException {
		int[] rights = { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.WRITE,
				WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.GRANT };
		boolean[] allow = { true, true, true, true, true, true, true };

		ACLServiceImpl.create(userNode, userNode, rights, allow, 0, 0, s);
		ACLServiceImpl.create(homeRoomNode, userNode, rights, allow, 0, 0, s);

		// TODO: deactivate inheritance; check if callerPrincipal is in admin group
		if (callerPrincipalNode != userNode && !callerPrincipal.equals("root"))
			ACLServiceImpl.create(userNode, callerPrincipalNode, rights, allow, 0, 0, s);
	}
}
