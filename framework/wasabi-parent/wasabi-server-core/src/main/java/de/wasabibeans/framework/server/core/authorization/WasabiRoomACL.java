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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.internal.RoomServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.JmsConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;

public class WasabiRoomACL {

	public static void ACLEntryForCreate(Node roomNode) throws UnexpectedInternalProblemException {
		if (!ObjectServiceImpl.getName(roomNode).equals(WasabiConstants.HOME_ROOM_NAME))
			if (ACLServiceImpl.getInheritance(roomNode))
				ACLServiceImpl.setInheritance(roomNode, true);
		//WasabiRoomSQL.createRandomSQLinserts();
	}

	public static void ACLEntryForMove(Node roomNode) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		try {
			String[] inheritance_ids = WasabiRoomSQL.SQLQueryForMove(roomNode.getIdentifier());
			ACLServiceImpl.resetInheritance(roomNode, inheritance_ids);
			if (ACLServiceImpl.getInheritance(roomNode))
				ACLServiceImpl.setInheritance(roomNode, true);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void ACLEntryTemplateForCreate(Node roomNode, Node environmentNode, String callerPrincipal, Session s)
			throws UnexpectedInternalProblemException {
		if (!callerPrincipal.equals("root")) {
			Node callerPrincipalNode = UserServiceImpl.getUserByName(callerPrincipal, s);

			List<WasabiACLEntryTemplate> ACLEntryTemplateALL = ACLServiceImpl.getDefaultACLEntriesByType(
					environmentNode, WasabiType.ALL, s);

			List<WasabiACLEntryTemplate> ACLEntryTemplate = ACLServiceImpl.getDefaultACLEntriesByType(environmentNode,
					WasabiType.ROOM, s);

			ACLEntryTemplateALL.addAll(ACLEntryTemplate);

			if (!ACLEntryTemplateALL.isEmpty()) {
				int[] allowance = new int[7];
				for (WasabiACLEntryTemplate wasabiACLEntryTemplate : ACLEntryTemplateALL) {
					allowance[WasabiPermission.VIEW] = wasabiACLEntryTemplate.getView();
					allowance[WasabiPermission.READ] = wasabiACLEntryTemplate.getRead();
					allowance[WasabiPermission.EXECUTE] = wasabiACLEntryTemplate.getExecute();
					allowance[WasabiPermission.COMMENT] = wasabiACLEntryTemplate.getComment();
					allowance[WasabiPermission.INSERT] = wasabiACLEntryTemplate.getInsert();
					allowance[WasabiPermission.WRITE] = wasabiACLEntryTemplate.getWrite();
					allowance[WasabiPermission.GRANT] = wasabiACLEntryTemplate.getGrant();

					long startTime = wasabiACLEntryTemplate.getStart_Time();
					long endTime = wasabiACLEntryTemplate.getEnd_Time();

					ACLServiceImpl.create(roomNode, callerPrincipalNode, new int[] { WasabiPermission.VIEW,
							WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
							WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT }, allowance,
							startTime, endTime);
				}
			}
		}
	}

	public static void remove(Node roomNode, String callerPrincipal, Session s, boolean doJcrSave, boolean throwEvents,
			JmsConnector jms) throws UnexpectedInternalProblemException, ConcurrentModificationException {
		if (RoomServiceImpl.isHomeRoom(roomNode, s)) {
			throw new IllegalArgumentException("A user's home-room cannot be removed.");
		}

		WasabiObjectACL.remove(roomNode, callerPrincipal, s, doJcrSave, throwEvents, jms);
	}
}
