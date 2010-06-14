package de.wasabibeans.framework.server.core.common;

public interface WasabiConstants {
	public final static boolean JMSEnabled = false;

	public final static Long ROOT_ROOM_ID = 1L;

	public final static String HOME_ROOM_NAME = "home";

	public final static String ROOT_ROOM_NAME = "root";

	public final static String ROOT_USER_NAME = "root";

	public final static String ROOT_USER_PASSWORD = "meerrettich";

	public final static String ADMIN_USER_NAME = "admin";

	public final static String ADMIN_USER_PASSWORD = "meerrettich";

	public final static String ADMINS_GROUP_NAME = "admins";

	public final static String WASABI_GROUP_NAME = "wasabi";
	
	public final static String JCR_NS_PREFIX = "wasabi";
	
	public final static String JCR_NS_URI = "http://www.wasabibeans.de/jcr/1.0";

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
}
