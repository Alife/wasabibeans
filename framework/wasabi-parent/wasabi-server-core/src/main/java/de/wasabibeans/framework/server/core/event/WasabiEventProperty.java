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

package de.wasabibeans.framework.server.core.event;

public class WasabiEventProperty {

	// general event properties: the type of the event and the user who triggered the event
	public static final String EVENT_TYPE = "type";
	public static final String TRIGGERED_BY = "triggerdBy";

	// the object affected by the event
	public static final String OBJECT_ID = "objectId";
	public static final String OBJECT_NAME = "objectName";
	public static final String OBJECT_TYPE = "objectType";

	// the environment of the affected object
	public static final String ENV_ID = "envId";
	public static final String ENV_NAME = "envName";
	public static final String ENV_TYPE = "envType";

	// the new environment of the affected object (when an object is moved)
	public static final String NEW_ENV_ID = "newEnvId";
	public static final String NEW_ENV_NAME = "newEnvName";
	public static final String NEW_ENV_TYPE = "newEnvType";

	// information about the property of PROPERTY_CHANGED events
	public static final String PROPERTY_NAME = "propName";
	public static final String PROPERTY_TYPE = "propType";

	// properties for events concerning groups (adding and removing members)
	public static final String MEMBER_ID = "memberId";
	public static final String MEMBER_NAME = "memberName";
}
