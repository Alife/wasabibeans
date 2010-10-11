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
			msg = msg.replaceFirst("&" + i, params[i] != null ? params[i] : "");
		}
		return msg;
	}

	/* UsernamePasswordPrincipal */
	public final static String USERNAME_PASSWORD_PRINCIPAL_NULL_NAME = "Name cannot be null.";

	/* SqlLoginModule */
	public final static String SQL_LOGIN_MODULE_LOGIN_FAILURE = "Username or password incorrect.";

	/* TransferManager */
	public final static String TRANSFER_DTO2NODE_NULLSESSION = "Internal Problem: Attempted to convert DTO to JCR node without a given JCR session.";
	public final static String TRANSFER_NODE2DTO_NULLNODE = "Internal Problem: Attempted to convert null to DTO.";
	public final static String TRANSFER_NODE2DTO_REFLECTERROR = "Internal Problem: Could not create corresponding DTO for given JCR node.";
	public final static String TRANSFER_VALUE2DTO_NULLVERSION = "Internal Problem: Attempted to convert value to DTO without valid optLockId";

	/* JNDI */
	public final static String JNDI_FAILED_BIND = "Internal Problem: Could not bind &0 to JNDI context.";
	public final static String JNDI_FAILED_LOOKUP = "Internal Problem: Could not retrieve &0 from JNDI context.";
	public final static String JNDI_FAILED_UNBIND = "Internal Problem: Could not unbind &0 from JNDI context.";
	public final static String JNDI_FAILED_UNBIND_NAME_NOT_BOUND = "Attempted to unbind &0 from JNDI context, although it is not bound";
	public final static String JNDI_NO_CONTEXT = "Internal Problem: Could not access the JNDI context.";

	/* JCR */
	public final static String JCR_REPOSITORY_FAILURE = "Internal Problem: Could not use JCR repository as expected.";

	/* JMS */
	public final static String JMS_PROVIDER_FAILURE = "Internal Problem: Could not use JMS provider as expected.";
	public final static String JMS_DESTINATION_INVALID = "The given jmsDestinationName does not belong to an existing JMS &0.";

	/* Database, SQL */
	public final static String DB_FAILURE = "Internal Problem: Could not use database as expected.";

	/* Pipes */
	public final static String PIPES_NOT_FOUND = "The configured wasabi pipe could not be found.";

	/* Concurrent Modification */
	public final static String CONCURRENT_MOD_LOCKED = "The object &0 is locked.";
	public final static String CONCURRENT_MOD_INVALIDSTATE = "The operation could not be performed because of a concurrent modification. Please refresh your data and try again.";

	/* Object does not exist */
	public final static String OBJECT_DNE_ID = "The object with the id &0 does not exist.";
	public final static String OBJECT_DNE = "An object does not exist any more or has been moved.";

	/* Object already exists */
	public final static String OBJECT_ALREADY_EXISTS_NAME = "An object with the name '&0' already exists.";
	public final static String OBJECT_ALREADY_EXISTS_ENV = "An object with the same name already exists in the new environment";

	/* Invalid Argument */
	public final static String INVALID_ARG_NULL = "The given &0 must not be null.";

	/* Value */
	public final static String VALUE_WRONG_TYPE = "A value of the requested type is not stored. The currently stored value has the type &0.";
	public final static String VALUE_SAVE = "An exception occurred while trying to save the given &0.";
	public final static String VALUE_LOAD = "An exception occurred while trying to load the &0 of the given &1.";

	/* Target not found */
	public final static String TARGET_NOT_FOUND = "The referenced WasabiObject does not exist any more";

	/* Locking */
	public final static String LOCKING_OPTLOCK = "The given optLockId does not match the actual one. Please refresh your data and try again.";
	public final static String LOCKING_TOKEN_NULL = "The lock-token of the given DTO must not be null.";

	/* Versioning */
	public final static String VERSIONING_NOT_SUPPORTED = "Versioning is only supported for wasabi-rooms, wasabi-containers, and wasabi-documents.";

	/* Internal Services */
	public final static String INTERNAL_PROBLEM = "Unexpected internal problem.";
	public final static String INTERNAL_EJB_CONTAINER_PROBLEM = "The services of the EJB container could not be used as expected.";
	public final static String INTERNAL_NO_ROOT_ROOM = "Internal Problem: The root room does not exist.";
	public final static String INTERNAL_NO_HOME_ROOM = "Internal Problem: The home room does not exist.";
	public final static String INTERNAL_NO_USER = "User does not exist.";
	public final static String INTERNAL_NO_WASABI_GROUP = "Internal Problem: The wasabi group does not exist.";

	public final static String INTERNAL_TYPE_CONFLICT = "Wrong WasabiType.";
	public final static String INTERNAL_UNEQUAL_LENGTH = "Count of &0 differs to count of &1.";

	/* Authorization */
	public final static String AUTHORIZATION_NO_PERMISSION = "You don't have the permission to access: &0 requires &1 permission at parameter &2.";
	public final static String AUTHORIZATION_NO_PERMISSION_EXISTS = "You don't have the permission to access or object does not exist.";
	public final static String AUTHORIZATION_NO_PERMISSION_GROUP = "You don't have the permission to access: &0 requires &1 permission at group &2.";
	public final static String AUTHORIZATION_NO_PERMISSION_RETURN = "You don't have the permission to access: &0 requires &1 permission at requested data.";
	public final static String AUTHORIZATION_NO_PERMISSION_ADMIN = "You don't have the permission to access: Administrative user needed for this operation.";

}
