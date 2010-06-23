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

package de.wasabibeans.framework.server.core.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="WasabiLocationDTO", namespace="de.wasabibeans.framework.server.core.dto")
public abstract class WasabiLocationDTO extends WasabiObjectDTO {

	@XmlElement(namespace="de.wasabibeans.framework.server.core.dto",required=true)
	private static final long serialVersionUID = -3061976862076696813L;

}
