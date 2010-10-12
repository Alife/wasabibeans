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
@XmlType(name = "WasabiCertificateDTO", namespace = "de.wasabibeans.framework.server.core.dto")
public class WasabiCertificateDTO implements Serializable {

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	private static final long serialVersionUID = -3013164768060621298L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected Long id = 0L;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected int permission;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected WasabiObjectDTO wasabiObject;

	@XmlElement(namespace = "de.wasabibeans.framework.server.core.dto", required = true)
	protected WasabiUserDTO wasabiUser;

	protected WasabiCertificateDTO() {

	}

	public Long getId() {
		return id;
	}

	public WasabiObjectDTO getObject() {
		return this.wasabiObject;
	}

	public int getPermission() {
		return this.permission;
	}

	public WasabiUserDTO getUser() {
		return this.wasabiUser;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	protected void setObject(WasabiObjectDTO wasabiObject) {
		this.wasabiObject = wasabiObject;
	}

	protected void setPermission(int permission) {
		this.permission = permission;
	}

	protected void setUser(WasabiUserDTO wasabiUser) {
		this.wasabiUser = wasabiUser;
	}
}
