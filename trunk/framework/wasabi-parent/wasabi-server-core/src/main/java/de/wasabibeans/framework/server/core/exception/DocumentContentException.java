package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class DocumentContentException extends WasabiException {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -3249739105140574871L;

	public DocumentContentException(String msg, Throwable t) {
		super(msg, t);
	}

}
