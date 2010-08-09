package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class UnexpectedInternalProblemException extends WasabiException {
	
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 177793274853041534L;

	public UnexpectedInternalProblemException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public UnexpectedInternalProblemException(String msg) {
		super(msg);
	}
}
