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

import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.local.CertificateServiceLocal;
import de.wasabibeans.framework.server.core.remote.CertificateServiceRemote;

@Stateless(name = "CertificateService")
public class Certificate implements CertificateServiceLocal, CertificateServiceRemote {

	private static int certAccess = 0;

	private static int certResultAccess = 0;
	private static ConcurrentHashMap<String, Boolean> commentRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> commentRightQueue = new ConcurrentLinkedQueue<String>();

	private static int dbAccess = 0;
	private static ConcurrentHashMap<String, Boolean> executeRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> executeRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Boolean> grantRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> grantRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Boolean> insertRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> insertRightQueue = new ConcurrentLinkedQueue<String>();

	private static ConcurrentHashMap<String, Boolean> readRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> readRightQueue = new ConcurrentLinkedQueue<String>();
	public static long sum = 0;
	private static ConcurrentHashMap<String, Boolean> viewRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> viewRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Boolean> writeRightMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentLinkedQueue<String> writeRightQueue = new ConcurrentLinkedQueue<String>();

	private static String concatInputs(String userUUID, String objectUUID) {
		return userUUID + "::" + objectUUID;
	}

	private static Vector<Node> getAllUserByGroup(Node groupNode) throws UnexpectedInternalProblemException {
		try {
			Vector<Node> allMembers = new Vector<Node>();
			for (NodeIterator ni : GroupServiceImpl.getAllMembers(groupNode)) {
				while (ni.hasNext()) {
					Node userRef = ni.nextNode();
					Node user = null;
					try {
						user = userRef.getProperty(WasabiNodeProperty.REFERENCED_OBJECT).getNode();
					} catch (ItemNotFoundException infe) {
						userRef.remove();
					}
					if (user != null)
						if (!allMembers.contains(user))
							allMembers.add(user);
				}
			}
			return allMembers;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean getCertificate(String userUUID, String objectUUID, int permission) {
		long start = java.lang.System.nanoTime();
		String key = concatInputs(userUUID, objectUUID);
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

	private static void invalidate(String objectUUID, int permission) {
		Enumeration<String> keys;
		switch (permission) {
		case WasabiPermission.VIEW:
			keys = viewRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					viewRightMap.remove(key);
					viewRightQueue.remove(key);
				}
			}
		case WasabiPermission.READ:
			keys = readRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					readRightMap.remove(key);
					readRightQueue.remove(key);
				}
			}
		case WasabiPermission.COMMENT:
			keys = commentRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					commentRightMap.remove(key);
					commentRightQueue.remove(key);
				}
			}
		case WasabiPermission.EXECUTE:
			keys = executeRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					executeRightMap.remove(key);
					executeRightQueue.remove(key);
				}
			}
		case WasabiPermission.INSERT:
			keys = insertRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					insertRightMap.remove(key);
					insertRightQueue.remove(key);
				}
			}
		case WasabiPermission.WRITE:
			keys = writeRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					writeRightMap.remove(key);
					writeRightQueue.remove(key);
				}
			}
		case WasabiPermission.GRANT:
			keys = grantRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					grantRightMap.remove(key);
					grantRightQueue.remove(key);
				}
			}
		}
	}

	private static void invalidate(String userUUID, String objectUUID, int permission) {
		String key = concatInputs(userUUID, objectUUID);
		switch (permission) {
		case WasabiPermission.VIEW:
			if (viewRightMap.containsKey(key)) {
				viewRightMap.remove(key);
				viewRightQueue.remove(key);
			}
		case WasabiPermission.READ:
			if (readRightMap.containsKey(key)) {
				readRightMap.remove(key);
				readRightQueue.remove(key);
			}
		case WasabiPermission.COMMENT:
			if (commentRightMap.containsKey(key)) {
				commentRightMap.remove(key);
				commentRightQueue.remove(key);
			}
		case WasabiPermission.EXECUTE:
			if (executeRightMap.containsKey(key)) {
				executeRightMap.remove(key);
				executeRightQueue.remove(key);
			}
		case WasabiPermission.INSERT:
			if (insertRightMap.containsKey(key)) {
				insertRightMap.remove(key);
				insertRightQueue.remove(key);
			}
		case WasabiPermission.WRITE:
			if (writeRightMap.containsKey(key)) {
				writeRightMap.remove(key);
				writeRightQueue.remove(key);
			}
		case WasabiPermission.GRANT:
			if (grantRightMap.containsKey(key)) {
				grantRightMap.remove(key);
				grantRightQueue.remove(key);
			}
		}
	}

	public static void invalidateCertificate(Node objectNode, Node identityNode, int[] permission, int[] allowance)
			throws RepositoryException, UnexpectedInternalProblemException {
		for (int i = 0; i < allowance.length; i++) {
			if (allowance[i] == -1 || allowance[i] == 0) {
				String identityType = identityNode.getPrimaryNodeType().getName();
				String objectUUID = ObjectServiceImpl.getUUID(objectNode);

				if (identityType.equals(WasabiNodeType.USER)) {
					String userUUID = ObjectServiceImpl.getUUID(identityNode);
					invalidate(userUUID, objectUUID, permission[i]);
				} else if (identityType.equals(WasabiNodeType.GROUP)) {
					Vector<Node> users = getAllUserByGroup(identityNode);
					for (Node userNode : users) {
						String userUUID = ObjectServiceImpl.getUUID(userNode);
						invalidate(userUUID, objectUUID, permission[i]);
					}
				}
			}
		}
	}

	public static void invalidateCertificateByObject(Node objectNode, int[] permission, int[] allowance)
			throws RepositoryException, UnexpectedInternalProblemException {
		for (int i = 0; i < allowance.length; i++) {
			if (allowance[i] == -1 || allowance[i] == 0) {
				String objectUUID = ObjectServiceImpl.getUUID(objectNode);
				invalidate(objectUUID, permission[i]);
			}
		}
	}

	public static void setCertificate(String userUUID, String objectUUID, int permission) {
		String key = concatInputs(userUUID, objectUUID);
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
