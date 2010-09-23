package de.wasabibeans.framework.server.core.authorization;

import java.util.concurrent.ConcurrentHashMap;

public class Certificate {

	private static ConcurrentHashMap<String, Boolean> certificate = new ConcurrentHashMap<String, Boolean>();

	public static void set(String key, boolean value) {
		certificate.put(key, value);
	}

	public static void set(String user, String service, String method, String objectUUID, boolean value) {
		String key = concatInputs(user, service, method, objectUUID);
		certificate.put(key, value);
	}

	public static boolean get(String key) {
		if (certificate.get(key)!=null && certificate.get(key))
			return true;
		else
			return false;
	}

	public static boolean get(String user, String service, String method, String objectUUID) {
		String key = concatInputs(user, service, method, objectUUID);
		if (certificate.get(key)!=null && certificate.get(key))
			return true;
		else
			return false;
	}

	private static String concatInputs(String user, String service, String method, String objectUUID) {
		return user + "::" + service + "::" + method + "::" + objectUUID;
	}
}
