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

import java.util.HashMap;

import javax.ejb.Stateless;

import de.wasabibeans.framework.server.core.local.CertificateServiceLocal;
import de.wasabibeans.framework.server.core.remote.CertificateServiceRemote;

@Stateless(name = "CertificateService")
public class Certificate implements CertificateServiceLocal, CertificateServiceRemote {

	private static HashMap<String, Boolean> certificate = new HashMap<String, Boolean>();
	private static int dbAccess = 0;
	private static int certAccess = 0;

	public static void set(String key, boolean value) {
		certificate.put(key, value);
	}

	public static void set(String user, String service, String method, String objectUUID, boolean value) {
		String key = concatInputs(user, service, method, objectUUID);
		certificate.put(key, value);
	}

	public static boolean get(String key) {
		if (certificate.get(key) != null && certificate.get(key))
			return true;
		else
			return false;
	}

	public static boolean get(String user, String service, String method, String objectUUID) {
		String key = concatInputs(user, service, method, objectUUID);
		if (certificate.get(key) != null && certificate.get(key)) {
			return true;
		} else
			return false;
	}

	private static String concatInputs(String user, String service, String method, String objectUUID) {
		return user + "::" + service + "::" + method + "::" + objectUUID;
	}

	@Override
	public int getDbAccess() {
		return dbAccess;
	}

	@Override
	public void setDbAccess() {
		dbAccess = dbAccess + 1;
	}

	@Override
	public int getCertAccess() {
		return certAccess;
	}

	@Override
	public void setCertAccess() {
		certAccess = certAccess + 1;
	}
}
