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

import de.wasabibeans.framework.server.core.common.WasabiType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WasabiACLEntryTemplateDTO", namespace = "de.wasabibeans.framework.server.core.dto")
public class WasabiACLEntryTemplateDTO implements Serializable {

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	private static final long serialVersionUID = -3013164768060621298L;

	protected int comment = 0;

	protected long endTime = 0;

	protected int execute = 0;
	protected int grant = 0;
	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected Long id = 0L;
	protected String identityID;
	protected int insert = 0;
	protected String locationID;
	protected int read = 0;
	protected long startTime = 0;

	protected int view = 0;
	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected WasabiIdentityDTO wasabiIdentity;

	protected String wasabiType;
	protected int write = 0;

	protected WasabiACLEntryTemplateDTO() {

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof WasabiACLEntryTemplateDTO))
			return false;
		WasabiACLEntryTemplateDTO other = (WasabiACLEntryTemplateDTO) obj;
		if (id == null) {
			if (other.getId() != null)
				return false;
		} else if (!id.equals(other.getId()))
			return false;
		return true;
	}

	public int getComment() {
		return this.comment;
	}

	public long getEndTime() {
		return this.endTime;
	}

	public int getExecute() {
		return this.execute;
	}

	public int getGrant() {
		return this.grant;
	}

	public Long getId() {
		return id;
	}

	public String getIdentityID() {
		return this.identityID;
	}

	public int getInsert() {
		return this.insert;
	}

	public String getLocationID() {
		return this.locationID;
	}

	public int getRead() {
		return this.read;
	}

	public long getStartTime() {
		return this.startTime;
	}

	public int getView() {
		return this.view;
	}

	public WasabiType getWasabiType() {
		return Enum.valueOf(WasabiType.class, this.wasabiType);
	}

	public int getWrite() {
		return this.write;
	}

	@Override
	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	protected void setComment(int comment) {
		this.comment = comment;
	}

	protected void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	protected void setExecute(int execute) {
		this.execute = execute;
	}

	protected void setGrant(int grant) {
		this.grant = grant;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public void setIdentityID(String identityID) {
		this.identityID = identityID;
	}

	protected void setInsert(int insert) {
		this.insert = insert;
	}

	public void setLocationID(String locationID) {
		this.locationID = locationID;
	}

	protected void setRead(int read) {
		this.read = read;
	}

	protected void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	protected void setView(int view) {
		this.view = view;
	}

	protected void setWasabiType(String wasabiType) {
		this.wasabiType = wasabiType;
	}

	protected void setWrite(int write) {
		this.write = write;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id + "]";
	}

}
