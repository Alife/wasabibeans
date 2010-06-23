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

public class WasabiExceptionMessages {

	/* UsernamePasswordPrincipal */
	public final static String USERNAME_PASSWORD_PRINCIPAL_NULL_NAME = "Name cannot be null";

	/* SqlLoginModule */
	public final static String SQL_LOGIN_MODULE_LOGIN_FAILURE = "Username or Password incorrect";

	/* TransferManager */
	public final static String TRANSFER_NODE2DTO_FAILURE = "Could not convert DTO to internal representation. Internal representation may not exist anymore.";
	
	/* JNDI */
	public final static String JNDI_NO_CONTEXT = "Could not initialize JNDI context";
	
	/* JCR */
	public final static String JCR_REPOSITORY_FAILURE = "Could not access the JCR repository.";
	public final static String JCR_LOGIN_FAILURE = "Could not establish JCR session. Login failed.";
}
