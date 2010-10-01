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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WasabiObjectDTO", namespace = "de.wasabibeans.framework.server.core.dto")
public abstract class WasabiObjectDTO implements Serializable {

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	private static final long serialVersionUID = -3996987979960095763L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected String id;

	protected Long optLockId;
	protected String lockToken;
	protected boolean deepLock;

	protected WasabiObjectDTO() {
		this.deepLock = false;
	}

	public String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	protected void setOptLockId(Long optLockId) {
		this.optLockId = optLockId;
	}

	public Long getOptLockId() {
		return optLockId;
	}

	public String getLockToken() {
		return lockToken;
	}

	public boolean isDeepLock() {
		return deepLock;
	}

	protected void setLockToken(String lockToken, boolean isDeep) {
		this.lockToken = lockToken;
		this.deepLock = isDeep;
	}

	@Override
	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof WasabiObjectDTO))
			return false;
		WasabiObjectDTO other = (WasabiObjectDTO) obj;
		if (id == null) {
			if (other.getId() != null)
				return false;
		} else if (!id.equals(other.getId()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id + "]";
	}

}
