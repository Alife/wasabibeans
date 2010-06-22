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

package de.wasabibeans.framework.server.core.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import de.wasabibeans.framework.server.core.common.WasabiConstants;

public class SqlConnector {

	public static DataSource connect() {
		try {
			Context context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup(WasabiConstants.JDNI_SQL_DATASOURCE);
			return dataSource;
		} catch (NamingException e) {
			return null;
		}
	}
}
