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

	public static String get(String msg, String... params) {
		for (int i = 0; i < params.length; i++) {
			msg = msg.replaceFirst("&" + i, params[i]);
		}
		return msg;
	}

	/* UsernamePasswordPrincipal */
	public final static String USERNAME_PASSWORD_PRINCIPAL_NULL_NAME = "Name cannot be null.";

	/* SqlLoginModule */
	public final static String SQL_LOGIN_MODULE_LOGIN_FAILURE = "Username or password incorrect.";

	/* TransferManager */
	public final static String TRANSFER_DTO2NODE_FAILURE = "Could not convert DTO to internal representation. Internal representation may not exist anymore.";
	public final static String TRANSFER_DTO2NODE_NULLDTO = "Attempted to convert null to internal representation.";
	public final static String TRANSFER_DTO2NODE_NULLSESSION = "Attempted to convert DTO to internal respresentation without a given JCR session.";
	public final static String TRANSFER_NODE2DTO_NULLNODE = "Attempted to convert null to DTO.";
	public final static String TRANSFER_NODE2DTO_REFLECTERROR = "Could not create corresponding DTO for given internal representation.";

	/* JNDI */
	public final static String JNDI_NO_CONTEXT = "Could not initialize JNDI context";

	/* JCR */
	public final static String JCR_REPOSITORY_FAILURE = "Could not access the JCR repository.";
	public final static String JCR_LOGIN_FAILURE = "Could not establish JCR session. Login failed.";

	/* Internal Services */
	public final static String INTERNAL_NAME_NULL = "The given name must not be null.";
	public final static String INTERNAL_ENVIRONMENT_NULL = "The given environment must not be null.";
	public final static String INTERNAL_NO_ROOT_ROOM = "The root room does not exist.";
	public final static String INTERNAL_NO_HOME_ROOM = "The home room does not exist.";
	public final static String INTERNAL_NO_USER = "User does not exist.";
}
