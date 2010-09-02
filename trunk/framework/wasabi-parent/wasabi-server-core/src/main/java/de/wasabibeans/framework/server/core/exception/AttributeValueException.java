package de.wasabibeans.framework.server.core.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class AttributeValueException extends WasabiException {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -24545788177153433L;


	public AttributeValueException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public AttributeValueException(String msg) {
		super(msg);
	}
}
