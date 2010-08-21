package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ConcurrentModificationException extends WasabiException {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 2528790534107587972L;

	public ConcurrentModificationException(String msg, Throwable t) {
		super(msg, t);
	}

	public ConcurrentModificationException(String msg) {
		super(msg);
	}
}
