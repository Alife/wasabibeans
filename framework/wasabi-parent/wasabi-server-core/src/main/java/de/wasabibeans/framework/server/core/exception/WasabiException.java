package de.wasabibeans.framework.server.core.exception;

public class WasabiException extends Exception {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 4516238521163806159L;

	public WasabiException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public WasabiException(String msg) {
		super(msg);
	}
}
