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

public class WasabiACLPriority {

	/* time rights */
	public static final int EXPLICIT_USER_TIME_RIGHT = 0;
	public static final int INHERITED_USER_TIME_RIGHT = 1;
	public static final int EXPLICIT_GROUP_TIME_RIGHT = 2;
	public static final int INHERITED_GROUP_TIME_RIGHT = 3;

	/* unlimited rights */
	public static final int EXPLICIT_USER_RIGHT = 4;
	public static final int INHERITED_USER_RIGHT = 5;
	public static final int EXPLICIT_GROUP_RIGHT = 6;
	public static final int INHERITED_GROUP_RIGHT = 7;
}
