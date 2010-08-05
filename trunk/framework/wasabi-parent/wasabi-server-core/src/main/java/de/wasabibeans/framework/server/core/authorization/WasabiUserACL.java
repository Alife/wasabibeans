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
