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

public interface WasabiConstants {
	public final static boolean JMSEnabled = false;
	public final static String HOME_ROOM_NAME = "home";
	public final static String ROOT_ROOM_NAME = "root";
	public final static String ROOT_USER_NAME = "root";
	public final static String ROOT_USER_PASSWORD = "meerrettich";
	public final static String ADMIN_USER_NAME = "admin";
	public final static String ADMIN_USER_PASSWORD = "meerrettich";
	public final static String ADMINS_GROUP_NAME = "admins";
	public final static String WASABI_GROUP_NAME = "wasabi";
	
	/* ACL checks and entries */
	public final static boolean ACL_CHECK_ENABLE = true;
	public final static boolean ACL_ENTRY_ENABLE = true;

	/* Enumerations */
	public enum SortType {
		ASCENDING, DESCENDING
	}

	public enum hashAlgorithms {
		SHA, MD5
	}

	/* Login Module */
	public final static int USE_JCR_LOGIN_MODULE = 1;
	public final static int USE_USERNAME_PASSWORD_LOGIN_MODULE = 2;
	public final static int USE_DATABASE_SERVER_LOGIN_MODULE = 3;
	public final static int USE_NO_PASSWORD_LOGIN_MODULE = 4;
	public final static int USE_LDAP_LOGIN_MODULE = 5;

	public final static int preferredLoginModule = USE_JCR_LOGIN_MODULE;

	public final static String SQL_LOGIN_MODULE_QUERY = "SELECT password FROM wasabi_user WHERE username=?";

	/* JNDI datasources */
	public final static String JNDI_SQL_DATASOURCE = "java:/wasabi";
	public final static String JNDI_JCR_DATASOURCE = "java:/jcr";

	/* JCR */
	public final static String JCR_NS_PREFIX = "wasabi";
	public final static String JCR_NS_PREFIX_STRUCTURE = "wasabiStructure";
	public final static String JCR_NS_URI = "http://www.wasabibeans.de/jcr/wasabi/1.0";
	public final static String JCR_NODETYPES_RESOURCE_PATH = "wasabi_nodetypes.cnd";
	public final static String JCR_ROOT_FOR_USERS_NAME = JCR_NS_PREFIX + ":users";
	public final static String JCR_ROOT_FOR_GROUPS_NAME = JCR_NS_PREFIX + ":groups";
	public final static String JCR_LOGIN = "wasabi";
	public final static String JCR_HIGHEST_VERSION_LABEL = "versionLabel";
	
	/* DTO */
	public final static String DTO_SUFFIX = "DTO";
	public final static String DTO_PREFIX = "Wasabi";

}
