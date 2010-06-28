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

package de.wasabibeans.framework.server.core.common;

public class WasabiNodeProperty {
	
	// properties of wasabi:object
	public static final String CREATED_BY = "wasabi:createdBy";
	public static final String CREATED_ON = "wasabi:createdOn";
	public static final String MODIFIED_BY = "wasabi:modifiedBy";
	public static final String MODIFIED_ON = "wasabi:modifiedOn";
	public static final String ATTRIBUTES = "wasabi:attributes";
	public static final String TAGS = "wasabi:tags";
	
	// properties of wasabi:location
	public static final String DOCUMENTS = "wasabi:documents";
	public static final String CONTAINERS = "wasabi:containers";
	public static final String LINKS = "wasabi:links";
	
	// properties of wasabi:room
	public static final String ROOMS = "wasabi:rooms";
	
	// properties of wasabi:identity
	public static final String DISPLAY_NAME = "wasabi:displayName";
	public static final String HOME_ROOM = "wasabi:homeRoom";
	
	// properties of wasabi:group
	public static final String SUBGROUPS = "wasabi:subgroups";
	public static final String MEMBERS = "wasabi:members";
	
	// properties of wasabi:user
	public static final String START_ROOM = "wasabi:startRoom";
	public static final String ACTIVE = "wasabi:active";
	public static final String MEMBERSHIPS = "wasabi:memberships";
	
	// properties of wasabi:document
	public static final String CONTENT = "wasabi:content";
	
	// properties of wasabi:link
	public static final String DESTINATION = "wasabi:destination";
	
	// properties of wasabi:tag
	public static final String TEXT = "wasabi:text";
	
}
