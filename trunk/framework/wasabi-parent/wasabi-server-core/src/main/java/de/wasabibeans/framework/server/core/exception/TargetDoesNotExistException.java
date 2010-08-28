package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TargetDoesNotExistException extends WasabiException {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 2624688659569807060L;

	public TargetDoesNotExistException(String msg, Throwable t) {
		super(msg, t);
	}

	public TargetDoesNotExistException(String msg) {
		super(msg);
	}
}
