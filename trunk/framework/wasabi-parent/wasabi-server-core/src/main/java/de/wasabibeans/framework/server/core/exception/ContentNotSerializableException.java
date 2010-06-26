package de.wasabibeans.framework.server.core.exception;


public class ContentNotSerializableException extends RuntimeException {
	
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 7856320536596769325L;
	
	private String nameOfDocument;

	public ContentNotSerializableException(String msg, String nameOfDocument) {
		super(msg);
		this.nameOfDocument = nameOfDocument;
	}

	public String getNameOfDocument() {
		return nameOfDocument;
	}

	public void setNameOfDocument(String nameOfDocument) {
		this.nameOfDocument = nameOfDocument;
	}
}
