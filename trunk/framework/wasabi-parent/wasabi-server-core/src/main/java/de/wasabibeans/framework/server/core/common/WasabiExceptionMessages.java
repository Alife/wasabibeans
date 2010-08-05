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
	public final static String TRANSFER_DTO2NODE_NULLDTO = "The given DTO must not be null.";
	public final static String TRANSFER_DTO2NODE_NULLSESSION = "Internal Problem: Attempted to convert DTO to JCR node without a given JCR session.";
	public final static String TRANSFER_NODE2DTO_NULLNODE = "Internal Problem: Attempted to convert null to DTO.";
	public final static String TRANSFER_NODE2DTO_REFLECTERROR = "Internal Problem: Could not create corresponding DTO for given JCR node.";

	/* JNDI */
	public final static String JNDI_FAILED_BIND = "Internal Problem: Could not bind &0 to JNDI context.";
	public final static String JNDI_FAILED_LOOKUP = "Internal Problem: Could not retrieve &0 from JNDI context.";
	public final static String JNDI_FAILED_UNBIND = "Internal Problem: Could not unbind &0 from JNDI context.";
	public final static String JNDI_FAILED_UNBIND_NAME_NOT_BOUND = "Attempted to unbind &0 from JNDI context, although it is not bound";
	public final static String JNDI_NO_CONTEXT = "Internal Problem: Could not access the JNDI context.";
	
	/* JCR */
	public final static String JCR_REPOSITORY_FAILURE = "Internal Problem: Could not use JCR repository as expected.";
	
	/* Database, SQL */
	public final static String DB_FAILURE = "Internal Problem: Could not use database as expected.";

	/* Internal Services */
	public final static String INTERNAL_DOCUMENT_CONTENT_LOAD = "An exception occurred while trying to load the content of the given document.";
	public final static String INTERNAL_DOCUMENT_CONTENT_SAVE = "An exception occurred while trying to save the given content.";
	public final static String INTERNAL_NO_ROOT_ROOM = "Internal Problem: The root room does not exist.";
	public final static String INTERNAL_NO_HOME_ROOM = "Internal Problem: The home room does not exist.";
	public final static String INTERNAL_NO_USER = "User does not exist.";
	public final static String INTERNAL_OBJECT_ALREADY_EXISTS = "The &0 &1 already exists.";
	public final static String INTERNAL_PARAM_NULL = "The given &0 must not be null.";
	public final static String INTERNAL_UNEQUAL_LENGTH = "Count of &0 differs to count of &1.";
	public final static String INTERNAL_TYPE_CONFLICT = "Wrong WasabiType.";
	
	/* AuthorizationInterceptor */
}