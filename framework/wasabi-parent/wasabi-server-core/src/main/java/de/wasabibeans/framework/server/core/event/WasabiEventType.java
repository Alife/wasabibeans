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

public class WasabiEventType {

	public static final byte REMOVED = 0;
	public static final byte CREATED = 1;
	public static final byte MOVED = 2;
	public static final byte PROPERTY_CHANGED = 3;
	public static final byte ROOM_ENTERED = 4;
	public static final byte ROOM_LEFT = 5;
	public static final byte MEMBER_ADDED = 6;
	public static final byte MEMBER_REMOVED = 7;
	public static final byte NO_PERMISSION = 8;
	
}
