package de.wasabibeans.framework.server.core.authorization;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ACLServiceImpl;

public class WasabiDocumentACL {

	public static void ACLEntryForCreate(Node documentNode, Session s) throws UnexpectedInternalProblemException {
		try {
			if (documentNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
				ACLServiceImpl.setInheritance(documentNode, true, s);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}
