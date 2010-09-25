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

package de.wasabibeans.framework.server.core.common;

public class WasabiNodeProperty {

	// properties of wasabi:object
	public static final String CREATED_BY = WasabiConstants.JCR_NS_PREFIX + ":createdBy";
	public static final String CREATED_ON = WasabiConstants.JCR_NS_PREFIX + ":createdOn";
	public static final String MODIFIED_BY = WasabiConstants.JCR_NS_PREFIX + ":modifiedBy";
	public static final String MODIFIED_ON = WasabiConstants.JCR_NS_PREFIX + ":modifiedOn";
	public static final String ATTRIBUTES = WasabiConstants.JCR_NS_PREFIX + ":attributes";
	public static final String TAGS = WasabiConstants.JCR_NS_PREFIX + ":tags";
	public static final String INHERITANCE = WasabiConstants.JCR_NS_PREFIX + ":inheritance";
	public static final String OPT_LOCK_ID = WasabiConstants.JCR_NS_PREFIX + ":optLockId";

	// properties of wasabi:location
	public static final String DOCUMENTS = WasabiConstants.JCR_NS_PREFIX + ":documents";
	public static final String CONTAINERS = WasabiConstants.JCR_NS_PREFIX + ":containers";
	public static final String LINKS = WasabiConstants.JCR_NS_PREFIX + ":links";

	// properties of wasabi:room
	public static final String ROOMS = WasabiConstants.JCR_NS_PREFIX + ":rooms";
	public static final String PRESENT_USERS = WasabiConstants.JCR_NS_PREFIX + ":presentUsers";
	public static final String PIPELINE = WasabiConstants.JCR_NS_PREFIX + ":pipeline";

	// properties of wasabi:identity
	public static final String DISPLAY_NAME = WasabiConstants.JCR_NS_PREFIX + ":displayName";

	// properties of wasabi:group
	public static final String SUBGROUPS = WasabiConstants.JCR_NS_PREFIX + ":subgroups";
	public static final String MEMBERS = WasabiConstants.JCR_NS_PREFIX + ":members";

	// properties of wasabi:user
	public static final String HOME_ROOM = WasabiConstants.JCR_NS_PREFIX + ":homeRoom";
	public static final String START_ROOM = WasabiConstants.JCR_NS_PREFIX + ":startRoom";
	public static final String ACTIVE = WasabiConstants.JCR_NS_PREFIX + ":active";
	public static final String MEMBERSHIPS = WasabiConstants.JCR_NS_PREFIX + ":memberships";
	public static final String WHEREABOUTS = WasabiConstants.JCR_NS_PREFIX + ":whereabouts";

	// properties of wasabi:attribute
	public static final String VALUE = WasabiConstants.JCR_NS_PREFIX + ":value";
	public static final String TYPE = WasabiConstants.JCR_NS_PREFIX + ":type";

	// properties of wasabi:document
	public static final String CONTENT = WasabiConstants.JCR_NS_PREFIX + ":content";
	public static final String CONTENT_REFS = WasabiConstants.JCR_NS_PREFIX + ":contentrefs";

	// properties of wasabi:link
	public static final String DESTINATION = WasabiConstants.JCR_NS_PREFIX + ":destination";

	// properties of wasabi:pipeline
	public static final String FILTER = WasabiConstants.JCR_NS_PREFIX + ":filter";
	public static final String EMBEDDABLE = WasabiConstants.JCR_NS_PREFIX + ":embeddable";

	// properties of wasabiStructure:tag
	public static final String TEXT = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":text";

	// properties of wasabiStructure:objectref
	public static final String REFERENCED_OBJECT = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":referencedObject";

	// properties of wasabiStructure:contentref
	public static final String FILTER_CLASS = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":filterClass";
	public static final String REF = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":ref";
	public static final String DOCUMENT = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":document";
	public static final String MIME_TYPE = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":mimeType";
	public static final String JSONDATA = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":jsonData";
	public static final String SIZE = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":size";
	public static final String IS_CONTENT_AVAILABLE = WasabiConstants.JCR_NS_PREFIX_STRUCTURE + ":isContentAvailable";

	// properties of versionable wasabi node types
	public static final String VERSION_COMMENT = WasabiConstants.JCR_NS_PREFIX + ":versionComment";
}
