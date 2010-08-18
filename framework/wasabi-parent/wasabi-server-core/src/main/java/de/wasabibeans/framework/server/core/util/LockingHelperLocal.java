package de.wasabibeans.framework.server.core.util;

import javax.ejb.Local;
import javax.jcr.Node;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface LockingHelperLocal {

	public void lock(Node node) throws UnexpectedInternalProblemException;

	public void unlock(Node node) throws UnexpectedInternalProblemException;
}
