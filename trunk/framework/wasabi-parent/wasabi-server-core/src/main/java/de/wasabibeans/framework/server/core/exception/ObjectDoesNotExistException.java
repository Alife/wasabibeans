/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

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
