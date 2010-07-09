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

public class WasabiNodeType {

	// node types representing wasabi objects
	public static final String WASABI_ROOM = WasabiConstants.JCR_NS_PREFIX + ":room";
	public static final String WASABI_CONTAINER = WasabiConstants.JCR_NS_PREFIX + ":container";
	public static final String WASABI_GROUP = WasabiConstants.JCR_NS_PREFIX + ":group";
	public static final String WASABI_USER = WasabiConstants.JCR_NS_PREFIX + ":user";
	public static final String WASABI_DOCUMENT = WasabiConstants.JCR_NS_PREFIX + ":document";
	public static final String WASABI_LINK = WasabiConstants.JCR_NS_PREFIX + ":link";
	public static final String WASABI_ATTRIBUTE = WasabiConstants.JCR_NS_PREFIX + ":attribute";
	public static final String WASABI_TAG = WasabiConstants.JCR_NS_PREFIX + ":tag";
	
	// node types for structuring
	public static final String WASABI_USERS = WasabiConstants.JCR_NS_PREFIX + ":users";
	public static final String WASABI_GROUPS = WasabiConstants.JCR_NS_PREFIX + ":groups";

}
