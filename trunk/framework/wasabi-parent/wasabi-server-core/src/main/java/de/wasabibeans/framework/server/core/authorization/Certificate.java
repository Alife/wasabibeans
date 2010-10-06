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
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.local.CertificateServiceLocal;
import de.wasabibeans.framework.server.core.remote.CertificateServiceRemote;

@Stateless(name = "CertificateService")
public class Certificate implements CertificateServiceLocal, CertificateServiceRemote {

	public static long sum = 0;

	private static int certAccess = 0;
	private static int certResultAccess = 0;
	private static int dbAccess = 0;

	private static ConcurrentHashMap<String, Boolean> viewRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> readRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> commentRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> executeRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> insertRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> writeRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Boolean> grantRightMap = new ConcurrentHashMap<String, Boolean>();

	private static ConcurrentLinkedQueue<String> viewRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> readRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> commentRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> executeRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> insertRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> writeRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<String> grantRightQueue = new ConcurrentLinkedQueue<String>();

	private static String concatInputs(String user, String objectUUID, int permission) {
		return user + "::" + objectUUID + "::" + (new Integer(permission)).toString();
	}

	public static boolean getCertificate(String user, String objectUUID, int permission) {
		long start = java.lang.System.nanoTime();
		String key = concatInputs(user, objectUUID, permission);
		Boolean value;
		switch (permission) {
		case WasabiPermission.VIEW:
			value = viewRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.READ:
			value = readRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.COMMENT:
			value = commentRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.EXECUTE:
			value = executeRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.INSERT:
			value = insertRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.WRITE:
			value = writeRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		case WasabiPermission.GRANT:
			value = grantRightMap.get(key);
			if (value == null) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			} else if (value) {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return true;
			} else {
				long end = java.lang.System.nanoTime();
				System.out.println("cache pass: " + (end - start));
				return false;
			}
		}
		return false;
	}

	public static void setCertificate(String user, String objectUUID, int permission) {
		String key = concatInputs(user, objectUUID, permission);
		switch (permission) {
		case WasabiPermission.VIEW:
			if (viewRightMap.size() == WasabiConstants.VIEW_MAP_SIZE) {
				String topOfQueue = viewRightQueue.poll();
				viewRightMap.remove(topOfQueue);
			}
			viewRightMap.put(key, true);
			viewRightQueue.add(key);
		case WasabiPermission.READ:
			if (readRightMap.size() == WasabiConstants.READ_MAP_SIZE) {
				String topOfQueue = readRightQueue.poll();
				readRightMap.remove(topOfQueue);
			}
			readRightMap.put(key, true);
			readRightQueue.add(key);
		case WasabiPermission.COMMENT:
			if (commentRightMap.size() == WasabiConstants.COMMENT_MAP_SIZE) {
				String topOfQueue = commentRightQueue.poll();
				commentRightMap.remove(topOfQueue);
			}
			commentRightMap.put(key, true);
			commentRightQueue.add(key);
		case WasabiPermission.EXECUTE:
			if (executeRightMap.size() == WasabiConstants.EXECUTE_MAP_SIZE) {
				String topOfQueue = executeRightQueue.poll();
				executeRightMap.remove(topOfQueue);
			}
			executeRightMap.put(key, true);
			executeRightQueue.add(key);
		case WasabiPermission.INSERT:
			if (insertRightMap.size() == WasabiConstants.INSERT_MAP_SIZE) {
				String topOfQueue = insertRightQueue.poll();
				insertRightMap.remove(topOfQueue);
			}
			insertRightMap.put(key, true);
			insertRightQueue.add(key);
		case WasabiPermission.WRITE:
			if (writeRightMap.size() == WasabiConstants.WRITE_MAP_SIZE) {
				String topOfQueue = writeRightQueue.poll();
				writeRightMap.remove(topOfQueue);
			}
			writeRightMap.put(key, true);
			writeRightQueue.add(key);
		case WasabiPermission.GRANT:
			if (grantRightMap.size() == WasabiConstants.GRANT_MAP_SIZE) {
				String topOfQueue = grantRightQueue.poll();
				grantRightMap.remove(topOfQueue);
			}
			grantRightMap.put(key, true);
			grantRightQueue.add(key);
		}
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
