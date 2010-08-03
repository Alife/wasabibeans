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
@XmlType(name = "WasabiACLEntryDTO", namespace = "de.wasabibeans.framework.server.core.dto")
public class WasabiACLEntryDTO implements Serializable {

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	private static final long serialVersionUID = -3013164768060621298L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected Long id = 0L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected WasabiIdentityDTO wasabiIdentity;

	protected int view = 0;
	protected int read = 0;
	protected int write = 0;
	protected int insert = 0;
	protected int execute = 0;
	protected int comment = 0;
	protected int grant = 0;

	protected String user = "";
	protected String group = "";
	protected String parent = "";
	protected String inheritance_id;

	protected long startTime = 0;
	protected long endTime = 0;

	protected boolean inheritance;
	

	protected WasabiACLEntryDTO() {

	}

	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}
	
	protected void setInheritanceId(String inheritance_id) {
		this.inheritance_id = inheritance_id;
	}

	protected void setView(int view) {
		this.view = view;
	}

	protected void setRead(int read) {
		this.read = read;
	}

	protected void setWrite(int write) {
		this.write = write;
	}

	protected void setInsert(int insert) {
		this.insert = insert;
	}

	protected void setExecute(int execute) {
		this.execute = execute;
	}

	protected void setComment(int comment) {
		this.comment = comment;
	}

	protected void setGrant(int grant) {
		this.grant = grant;
	}

	public int getView() {
		return this.view;
	}

	public int getRead() {
		return this.read;
	}

	public int getInsert() {
		return this.insert;
	}

	public int getExecute() {
		return this.execute;
	}

	public int getWrite() {
		return this.write;
	}

	public int getComment() {
		return this.comment;
	}

	public int getGrant() {
		return this.grant;
	}

	public String getUserId() {
		return this.user;
	}

	protected void setUserId(String user) {
		this.user = user;
	}

	protected void setGroupId(String group) {
		this.group = group;
	}

	public String getGroupId() {
		return this.group;
	}

	protected void setParentId(String parent) {
		this.parent = parent;
	}

	public String getParentId() {
		return this.parent;
	}

	protected void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getStartTime() {
		return this.startTime;
	}

	protected void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getEndTime() {
		return this.endTime;
	}

	protected void setInheritance(boolean inheritance) {
		this.inheritance = inheritance;
	}

	public boolean getInheritance() {
		return this.inheritance;
	}
	
	public String getInheritanceId() {
		return this.inheritance_id;
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
