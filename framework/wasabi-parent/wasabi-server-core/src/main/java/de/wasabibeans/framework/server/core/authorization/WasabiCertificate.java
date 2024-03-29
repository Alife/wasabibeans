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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiCertificateHandle;

public class WasabiCertificate {

	private static ConcurrentHashMap<String, Long> commentRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> commentRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> executeRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> executeRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> grantRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> grantRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> insertRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> insertRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> readRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> readRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> viewRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> viewRightQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Long> writeRightMap = new ConcurrentHashMap<String, Long>();
	private static ConcurrentLinkedQueue<String> writeRightQueue = new ConcurrentLinkedQueue<String>();

	private static String concatInputs(String userUUID, String objectUUID) {
		return userUUID + "::" + objectUUID;
	}

	public static Vector<WasabiCertificateHandle> filterCertificateByObject(String objectUUID, int[] permission) {
		Vector<WasabiCertificateHandle> certificates = new Vector<WasabiCertificateHandle>();

		for (int i = 0; i < permission.length; i++) {
			switch (permission[i]) {
			case WasabiPermission.VIEW:
				for (String entry : viewRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.READ:
				for (String entry : readRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.COMMENT:
				for (String entry : commentRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.EXECUTE:
				for (String entry : executeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.INSERT:
				for (String entry : insertRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.WRITE:
				for (String entry : writeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.GRANT:
				for (String entry : grantRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String object = getObjectUUID(entry);
					if (object.equals(objectUUID)) {
						cert.setObjectUUID(object);
						cert.setUserUUID(getUserUUID(entry));
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			}
		}
		return certificates;
	}

	public static Vector<WasabiCertificateHandle> filterCertificateByPermission(int[] permission) {
		Vector<WasabiCertificateHandle> certificates = new Vector<WasabiCertificateHandle>();

		for (int i = 0; i < permission.length; i++) {
			switch (permission[i]) {
			case WasabiPermission.VIEW:
				for (String entry : viewRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.READ:
				for (String entry : readRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.COMMENT:
				for (String entry : commentRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.EXECUTE:
				for (String entry : executeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.INSERT:
				for (String entry : insertRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.WRITE:
				for (String entry : writeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			case WasabiPermission.GRANT:
				for (String entry : grantRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					cert.setObjectUUID(getObjectUUID(entry));
					cert.setUserUUID(getUserUUID(entry));
					cert.setPermission(permission[i]);
					certificates.add(cert);
				}
				break;
			}
		}
		return certificates;
	}

	public static Vector<WasabiCertificateHandle> filterCertificateByUser(String userUUID, int[] permission) {
		Vector<WasabiCertificateHandle> certificates = new Vector<WasabiCertificateHandle>();

		for (int i = 0; i < permission.length; i++) {
			switch (permission[i]) {
			case WasabiPermission.VIEW:
				for (String entry : viewRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.READ:
				for (String entry : readRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.COMMENT:
				for (String entry : commentRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.EXECUTE:
				for (String entry : executeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.INSERT:
				for (String entry : insertRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.WRITE:
				for (String entry : writeRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			case WasabiPermission.GRANT:
				for (String entry : grantRightMap.keySet()) {
					WasabiCertificateHandle cert = new WasabiCertificateHandle();
					String user = getUserUUID(entry);
					if (user.equals(userUUID)) {
						cert.setObjectUUID(getObjectUUID(entry));
						cert.setUserUUID(user);
						cert.setPermission(permission[i]);
						certificates.add(cert);
					}
				}
				break;
			}
		}
		return certificates;
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
		String key = concatInputs(userUUID, objectUUID);
		Long value;
		long time = java.lang.System.currentTimeMillis();

		switch (permission) {
		case WasabiPermission.VIEW:
			value = viewRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.READ:
			value = readRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.COMMENT:
			value = commentRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.EXECUTE:
			value = executeRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.INSERT:
			value = insertRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.WRITE:
			value = writeRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		case WasabiPermission.GRANT:
			value = grantRightMap.get(key);
			if (value == null)
				return false;
			else if (time < value || value == 0)
				return true;
			else
				return false;
		}
		return false;
	}

	public static boolean getCertificate(String userUUID, String objectUUID, int[] permission) {
		for (int i = 0; i < permission.length; i++) {
			boolean ret = getCertificate(userUUID, objectUUID, permission[i]);
			if (ret)
				return true;
		}
		return false;
	}

	private static String getObjectUUID(String key) {
		String[] split = key.split("::");
		return split[1];
	}

	private static String getUserUUID(String key) {
		String[] split = key.split("::");
		return split[0];
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
			break;
		case WasabiPermission.READ:
			keys = readRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					readRightMap.remove(key);
					readRightQueue.remove(key);
				}
			}
			break;
		case WasabiPermission.COMMENT:
			keys = commentRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					commentRightMap.remove(key);
					commentRightQueue.remove(key);
				}
			}
			break;
		case WasabiPermission.EXECUTE:
			keys = executeRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					executeRightMap.remove(key);
					executeRightQueue.remove(key);
				}
			}
			break;
		case WasabiPermission.INSERT:
			keys = insertRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					insertRightMap.remove(key);
					insertRightQueue.remove(key);
				}
			}
			break;
		case WasabiPermission.WRITE:
			keys = writeRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					writeRightMap.remove(key);
					writeRightQueue.remove(key);
				}
			}
			break;
		case WasabiPermission.GRANT:
			keys = grantRightMap.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains(objectUUID)) {
					grantRightMap.remove(key);
					grantRightQueue.remove(key);
				}
			}
			break;
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
			break;
		case WasabiPermission.READ:
			if (readRightMap.containsKey(key)) {
				readRightMap.remove(key);
				readRightQueue.remove(key);
			}
			break;
		case WasabiPermission.COMMENT:
			if (commentRightMap.containsKey(key)) {
				commentRightMap.remove(key);
				commentRightQueue.remove(key);
			}
			break;
		case WasabiPermission.EXECUTE:
			if (executeRightMap.containsKey(key)) {
				executeRightMap.remove(key);
				executeRightQueue.remove(key);
			}
			break;
		case WasabiPermission.INSERT:
			if (insertRightMap.containsKey(key)) {
				insertRightMap.remove(key);
				insertRightQueue.remove(key);
			}
			break;
		case WasabiPermission.WRITE:
			if (writeRightMap.containsKey(key)) {
				writeRightMap.remove(key);
				writeRightQueue.remove(key);
			}
			break;
		case WasabiPermission.GRANT:
			if (grantRightMap.containsKey(key)) {
				grantRightMap.remove(key);
				grantRightQueue.remove(key);
			}
			break;
		}
	}

	public static void invalidateCertificate(Node objectNode, Node identityNode, int[] permission, int[] allowance)
			throws RepositoryException, UnexpectedInternalProblemException {
		String identityType = identityNode.getPrimaryNodeType().getName();
		String objectUUID = ObjectServiceImpl.getUUID(objectNode);

		for (int i = 0; i < allowance.length; i++) {
			if (allowance[i] == -1 || allowance[i] == 0) {
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

	public static void invalidateCertificateByIdentity(Node identityNode, int[] permission, int[] allowance)
			throws RepositoryException, UnexpectedInternalProblemException {
		String identityType = identityNode.getPrimaryNodeType().getName();
		String identityUUID = ObjectServiceImpl.getUUID(identityNode);

		if (identityType.equals(WasabiNodeType.USER)) {
			for (int i = 0; i < allowance.length; i++) {
				if (allowance[i] == -1 || allowance[i] == 0) {
					invalidate(identityUUID, permission[i]);
				} else if (identityType.equals(WasabiNodeType.GROUP)) {
					Vector<Node> users = getAllUserByGroup(identityNode);
					for (Node userNode : users) {
						invalidate(ObjectServiceImpl.getUUID(userNode), permission[i]);
					}
				}
			}
		}
	}

	public static void invalidateCertificateByObject(Node objectNode, int[] permission, int[] allowance)
			throws UnexpectedInternalProblemException {
		String objectUUID = ObjectServiceImpl.getUUID(objectNode);

		for (int i = 0; i < allowance.length; i++) {
			if (allowance[i] == -1 || allowance[i] == 0) {
				invalidate(objectUUID, permission[i]);
			}
		}
	}

	public static void invalidateCertificateForGroup(String objectUUID, String groupUUID, int permission)
			throws UnexpectedInternalProblemException {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		Session s = jcr.getJCRSession();

		try {
			Node groupNode = s.getNodeByIdentifier(groupUUID);
			Vector<Node> users = getAllUserByGroup(groupNode);
			for (Node userNode : users) {
				String userUUID = ObjectServiceImpl.getUUID(userNode);
				invalidate(userUUID, objectUUID, permission);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} finally {
			jndi.close();
		}
	}

	public static void invalidateCertificateForUser(String objectUUID, String userUUID, int permission) {
		invalidate(userUUID, objectUUID, permission);
	}

	public static void setCertificate(String userUUID, String objectUUID, int permission, long maxLifeTime) {
		String key = concatInputs(userUUID, objectUUID);
		switch (permission) {
		case WasabiPermission.VIEW:
			if (viewRightMap.size() == WasabiConstants.VIEW_MAP_SIZE) {
				String topOfQueue = viewRightQueue.poll();
				viewRightMap.remove(topOfQueue);
			}
			viewRightMap.put(key, maxLifeTime);
			viewRightQueue.add(key);
			break;
		case WasabiPermission.READ:
			if (readRightMap.size() == WasabiConstants.READ_MAP_SIZE) {
				String topOfQueue = readRightQueue.poll();
				readRightMap.remove(topOfQueue);
			}
			readRightMap.put(key, maxLifeTime);
			readRightQueue.add(key);
			break;
		case WasabiPermission.COMMENT:
			if (commentRightMap.size() == WasabiConstants.COMMENT_MAP_SIZE) {
				String topOfQueue = commentRightQueue.poll();
				commentRightMap.remove(topOfQueue);
			}
			commentRightMap.put(key, maxLifeTime);
			commentRightQueue.add(key);
			break;
		case WasabiPermission.EXECUTE:
			if (executeRightMap.size() == WasabiConstants.EXECUTE_MAP_SIZE) {
				String topOfQueue = executeRightQueue.poll();
				executeRightMap.remove(topOfQueue);
			}
			executeRightMap.put(key, maxLifeTime);
			executeRightQueue.add(key);
			break;
		case WasabiPermission.INSERT:
			if (insertRightMap.size() == WasabiConstants.INSERT_MAP_SIZE) {
				String topOfQueue = insertRightQueue.poll();
				insertRightMap.remove(topOfQueue);
			}
			insertRightMap.put(key, maxLifeTime);
			insertRightQueue.add(key);
			break;
		case WasabiPermission.WRITE:
			if (writeRightMap.size() == WasabiConstants.WRITE_MAP_SIZE) {
				String topOfQueue = writeRightQueue.poll();
				writeRightMap.remove(topOfQueue);
			}
			writeRightMap.put(key, maxLifeTime);
			writeRightQueue.add(key);
			break;
		case WasabiPermission.GRANT:
			if (grantRightMap.size() == WasabiConstants.GRANT_MAP_SIZE) {
				String topOfQueue = grantRightQueue.poll();
				grantRightMap.remove(topOfQueue);
			}
			grantRightMap.put(key, maxLifeTime);
			grantRightQueue.add(key);
			break;
		}
	}
}
