package de.wasabibeans.framework.server.core.authorization;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;

public class WasabiRoomACL {

	public static void ACLEntryForCreate(Node roomNode, Session s) throws UnexpectedInternalProblemException {
		try {
			if (roomNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
				ACLServiceImpl.setInheritance(roomNode, true, s);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void ACLEntryTemplateForCreate(Node roomNode, Node environmentNode, Node callerPrincipal, Session s)
			throws UnexpectedInternalProblemException {
		List<WasabiACLEntryTemplate> ACLEntryTemplate = ACLServiceImpl.getDefaultACLEntriesByType(environmentNode,
				WasabiType.ROOM, s);

		if (!ACLEntryTemplate.isEmpty()) {
			int[] allowance = new int[7];
			for (WasabiACLEntryTemplate wasabiACLEntryTemplate : ACLEntryTemplate) {
				allowance[WasabiPermission.VIEW] = wasabiACLEntryTemplate.getView();
				allowance[WasabiPermission.READ] = wasabiACLEntryTemplate.getRead();
				allowance[WasabiPermission.EXECUTE] = wasabiACLEntryTemplate.getExecute();
				allowance[WasabiPermission.COMMENT] = wasabiACLEntryTemplate.getComment();
				allowance[WasabiPermission.INSERT] = wasabiACLEntryTemplate.getInsert();
				allowance[WasabiPermission.WRITE] = wasabiACLEntryTemplate.getWrite();
				allowance[WasabiPermission.GRANT] = wasabiACLEntryTemplate.getGrant();

				long startTime = wasabiACLEntryTemplate.getStart_Time();
				long endTime = wasabiACLEntryTemplate.getEnd_Time();

				ACLServiceImpl.create(roomNode, callerPrincipal, new int[] { WasabiPermission.VIEW,
						WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
						WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT }, allowance,
						startTime, endTime, s);
			}
		}
	}
}
