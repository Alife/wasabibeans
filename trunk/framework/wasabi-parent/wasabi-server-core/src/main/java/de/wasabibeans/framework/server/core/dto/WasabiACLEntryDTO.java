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
@XmlType(name = "WasabiACLEntryDTO", namespace = "org.wasabibeans.server.core.transfer.model.dto")
public class WasabiACLEntryDTO implements Serializable {

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	private static final long serialVersionUID = -3013164768060621298L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected Long id;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected int permission;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected WasabiIdentityDTO wasabiIdentity;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected boolean allowance;

	protected WasabiACLEntryDTO() {

	}

	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public int getPermission() {
		return permission;
	}

	public boolean isAllowance() {
		return allowance;
	}

	public WasabiIdentityDTO getIdentity() {
		return wasabiIdentity;
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
		if (!(obj instanceof WasabiACLEntryDTO))
			return false;
		WasabiACLEntryDTO other = (WasabiACLEntryDTO) obj;
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