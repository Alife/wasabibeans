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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class WasabiGroupACL {

	public static void ACLEntryForCreate(Node groupNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		if (ACLServiceImpl.getInheritance(groupNode))
			ACLServiceImpl.setInheritance(groupNode, true);

		if (!ObjectServiceImpl.getName(groupNode).equals(WasabiConstants.ADMINS_GROUP_NAME)) {
			// Group gets READ and VIEW permission
			ACLServiceImpl.create(groupNode, groupNode, new int[] { WasabiPermission.VIEW, WasabiPermission.READ },
					new boolean[] { true, true }, 0, 0);

			// Creator of group gets GRANT permission
			if (!ObjectServiceImpl.getName(groupNode).equals(WasabiConstants.WASABI_GROUP_NAME)
					&& !ObjectServiceImpl.getName(groupNode).equals(WasabiConstants.PAF_GROUP_NAME))
				if (!WasabiAuthorizer.isAdminUser(callerPrincipal, s))
					ACLServiceImpl.create(groupNode, UserServiceImpl.getUserByName(callerPrincipal, s), new int[] {
							WasabiPermission.VIEW, WasabiPermission.READ, WasabiPermission.COMMENT,
							WasabiPermission.INSERT, WasabiPermission.EXECUTE, WasabiPermission.WRITE,
							WasabiPermission.GRANT }, new boolean[] { true, true, true, true, true, true, true }, 0, 0);
		}
	}

	public static void ACLEntryForMove(Node groupNode, Node newParentGroupNode)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			String parentUUID = newParentGroupNode.getSession().getRootNode().getNode(
					WasabiConstants.JCR_ROOT_FOR_GROUPS_NAME).getIdentifier();

			if (newParentGroupNode != null)
				parentUUID = newParentGroupNode.getIdentifier();

			String[] inheritance_ids = WasabiGroupSQL.SQLQueryForMove(groupNode.getIdentifier(), parentUUID);
			ACLServiceImpl.resetInheritance(groupNode, inheritance_ids);
			if (ACLServiceImpl.getInheritance(groupNode))
				ACLServiceImpl.setInheritance(groupNode, true);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void remove(Node roomNode, String callerPrincipal, Session s, boolean doJcrSave, boolean throwEvents,
			JmsConnector jms) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		WasabiObjectACL.remove(roomNode, callerPrincipal, s, doJcrSave, throwEvents, jms);
	}
}
