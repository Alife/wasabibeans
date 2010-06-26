package de.wasabibeans.framework.server.core.exception;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;

public class ObjectDoesNotExistException extends RuntimeException {
	
	private WasabiObjectDTO dto;

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -2901394653506435785L;
	
	public ObjectDoesNotExistException(WasabiObjectDTO dto) {
		super();
		this.dto = dto;
	}
	
	public WasabiObjectDTO getDto() {
		return dto;
	}

	public void setDto(WasabiObjectDTO dto) {
		this.dto = dto;
	}
}
