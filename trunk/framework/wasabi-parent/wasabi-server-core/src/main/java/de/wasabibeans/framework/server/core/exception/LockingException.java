package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class LockingException extends WasabiException {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 1748597032723928825L;

	public LockingException(String msg, Throwable t) {
		super(msg, t);
	}

	public LockingException(String msg) {
		super(msg);
	}
}
