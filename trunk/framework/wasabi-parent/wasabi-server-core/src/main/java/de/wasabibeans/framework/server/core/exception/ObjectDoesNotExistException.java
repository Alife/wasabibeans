/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 *  Further information are online available at: http://www.wasabibeans.de
 */

package de.wasabibeans.framework.server.core.exception;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;

public class ObjectDoesNotExistException extends WasabiException {

	private WasabiObjectDTO dto;

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -2901394653506435785L;

	public ObjectDoesNotExistException(String msg, WasabiObjectDTO dto) {
		super(msg);
		this.dto = dto;
	}
	
	public ObjectDoesNotExistException(String msg, WasabiObjectDTO dto, Throwable t) {
		super(msg, t);
		this.dto = dto;
	}

	public WasabiObjectDTO getDto() {
		return dto;
	}

	public void setDto(WasabiObjectDTO dto) {
		this.dto = dto;
	}
}