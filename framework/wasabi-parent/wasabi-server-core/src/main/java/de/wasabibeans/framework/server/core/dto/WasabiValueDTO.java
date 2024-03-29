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

package de.wasabibeans.framework.server.core.dto;

import java.io.Serializable;

public class WasabiValueDTO implements Serializable {

	private static final long serialVersionUID = -2463969904673513235L;
	
	private Serializable value;
	private Long optLockId;

	protected WasabiValueDTO(Serializable value, Long optLockId) {
		this.value = value;
		this.optLockId = optLockId;
	}

	public Long getOptLockId() {
		return optLockId;
	}

	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getValue() {
		return (T) value;
	}
}
