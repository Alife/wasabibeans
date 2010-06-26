package de.wasabibeans.framework.server.core.exception;

public class ObjectAlreadyExistsException extends RuntimeException {
	
	private String nameOfObject;

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -7655185892186652200L;
	
	public ObjectAlreadyExistsException(String msg, String nameOfObject) {
		super(msg);
		this.nameOfObject = nameOfObject;
	}

	public String getNameOfObject() {
		return nameOfObject;
	}

	public void setNameOfObject(String nameOfObject) {
		this.nameOfObject = nameOfObject;
	}
}
