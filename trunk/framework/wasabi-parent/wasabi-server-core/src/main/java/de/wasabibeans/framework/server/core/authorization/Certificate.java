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

package de.wasabibeans.framework.server.core.authorization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.local.CertificateServiceLocal;
import de.wasabibeans.framework.server.core.remote.CertificateServiceRemote;

@Stateless(name = "CertificateService")
public class Certificate implements CertificateServiceLocal, CertificateServiceRemote {

	public static long sum = 0;

	private static int certAccess = 0;
	private static int certResultAccess = 0;
	private static int dbAccess = 0;

	private static ConcurrentHashMap<String, Boolean> objectServiceMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> roomServiceMap = new ConcurrentHashMap<String, Boolean>();

	private static ConcurrentLinkedQueue<String> objectServiceQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> roomServiceQueue = new ConcurrentLinkedQueue<String>();

	private static String concatInputs(String user, String method, String objectUUID) {
		return user + "::" + method + "::" + objectUUID;
	}

	public static boolean getObjectServiceMap(String user, String method, String objectUUID) {
		long start = java.lang.System.nanoTime();
		String key = concatInputs(user, method, objectUUID);
		Boolean value = objectServiceMap.get(key);
		if (value == null) {
			long end = java.lang.System.nanoTime();
			System.out.println("objectCache pass: " + (end - start));
			return false;
		} else if (value) {
			long end = java.lang.System.nanoTime();
			System.out.println("objectCache pass: " + (end - start));
			return true;
		} else {
			long end = java.lang.System.nanoTime();
			System.out.println("objectCache pass: " + (end - start));
			return false;
		}
	}

	public static void setObjectServiceMap(String user, String method, String objectUUID, boolean value) {
		String key = concatInputs(user, method, objectUUID);
		if (objectServiceMap.size() == WasabiConstants.OBJECT_SERVICE_MAP_SIZE) {
			String topOfQueue = objectServiceQueue.poll();
			objectServiceMap.remove(topOfQueue);
		}
		objectServiceMap.put(key, value);
		objectServiceQueue.add(key);
	}

	public static boolean getRoomServiceMap(String user, String method, String objectUUID) {
		long start = java.lang.System.nanoTime();
		String key = concatInputs(user, method, objectUUID);
		Boolean value = roomServiceMap.get(key);
		if (value == null) {
			long end = java.lang.System.nanoTime();
			System.out.println("roomCache pass: " + (end - start));
			return false;
		} else if (value) {
			long end = java.lang.System.nanoTime();
			System.out.println("roomCache pass: " + (end - start));
			return true;
		} else {
			long end = java.lang.System.nanoTime();
			System.out.println("roomCache pass: " + (end - start));
			return false;
		}
	}

	public static void setRoomServiceMap(String user, String method, String objectUUID, boolean value) {
		String key = concatInputs(user, method, objectUUID);
		if (roomServiceMap.size() == WasabiConstants.ROOM_SERVICE_MAP_SIZE) {
			String topOfQueue = roomServiceQueue.poll();
			roomServiceMap.remove(topOfQueue);
		}
		roomServiceMap.put(key, value);
		roomServiceQueue.add(key);
	}

	@Override
	public int getCertAccess() {
		return certAccess;
	}

	@Override
	public int getDbAccess() {
		return dbAccess;
	}

	@Override
	public int getResultCertAccess() {
		return certResultAccess;
	}

	@Override
	public void setCertAccess() {
		certAccess = certAccess + 1;
	}

	@Override
	public void setDbAccess() {
		dbAccess = dbAccess + 1;
	}

	@Override
	public void setResultCertAccess() {
		certResultAccess = certResultAccess + 1;
	}
}
