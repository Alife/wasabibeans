package org.wasabibeans.server.core.common;

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

	public enum SortType {
		ASCENDING, DESCENDING
	}
}
