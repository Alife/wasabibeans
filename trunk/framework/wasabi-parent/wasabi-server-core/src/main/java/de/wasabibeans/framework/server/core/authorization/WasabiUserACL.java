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

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;

public class WasabiUserACL {

	public static void ACLEntryForCreate(Node userNode, Node homeRoomNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException {
		int[] rights = { WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.INSERT, WasabiPermission.WRITE,
				WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.GRANT };
		boolean[] allow = { true, true, true, true, true, true, true };

		if (!ObjectServiceImpl.getName(userNode).equals(WasabiConstants.ROOT_USER_NAME)
				&& !ObjectServiceImpl.getName(userNode).equals(WasabiConstants.ADMIN_USER_NAME)) {
			// user gets all rights at his homeRoom and himself
			ACLServiceImpl.create(userNode, userNode, rights, allow, 0, 0);
			ACLServiceImpl.create(homeRoomNode, userNode, rights, allow, 0, 0);

			if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s)) {
				Node callerPrincipalNode = UserServiceImpl.getUserByName(callerPrincipal, s);
				if (callerPrincipalNode != userNode)
					ACLServiceImpl.create(userNode, callerPrincipalNode, rights, allow, 0, 0);
			}
		}
	}
}
