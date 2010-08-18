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
import java.util.Date;

public class WasabiValue {

	private Serializable value;
	private Integer version;

	public WasabiValue(Serializable value) {
		this.value = value;
		this.version = null;
	}

	protected WasabiValue(Serializable value, Integer version) {
		this.value = value;
		this.version = version;
	}

	protected Integer getVersion() {
		return version;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

	public Boolean getBoolean() {
		return (Boolean) value;
	}

	public Character getCharacter() {
		return (Character) value;
	}

	public String getString() {
		return (String) value;
	}

	public Byte getByte() {
		return (Byte) value;
	}

	public Short getShort() {
		return (Short) value;
	}

	public Integer getInteger() {
		return (Integer) value;
	}

	public Long getLong() {
		return (Long) value;
	}

	public Float getFloat() {
		return (Float) value;
	}

	public Double getDouble() {
		return (Double) value;
	}

	public Date getDate() {
		return (Date) value;
	}

	public byte[] getByteArray() {
		return (byte[]) value;
	}

	public Serializable getValue() {
		return value;
	}
}
