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

package de.wasabibeans.framework.server.core.internal;

import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.authorization.WasabiCertificate;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.WasabiCertificateHandle;

public class AuthorizationServiceImpl {

	public static boolean existsCertificate(String objectUUID, String userUUID, int permission) {
		if (WasabiCertificate.getCertificate(userUUID, objectUUID, permission))
			return true;
		else
			return false;
	}

	public static boolean hasPermission(String objectUUID, String userUUID, int permission,

	Node objectNode, Node userNode, Session s) throws UnexpectedInternalProblemException {
		if (WasabiCertificate.getCertificate(userUUID, objectUUID, permission))
			return true;
		else if (WasabiAuthorizer.authorize(objectNode, ObjectServiceImpl.getName(userNode), permission, s)) {
			WasabiCertificate.setCertificate(userUUID, objectUUID, permission);
			return true;
		} else
			return false;
	}

	public static Vector<WasabiCertificateHandle> listCertificate(int permission) {
		return WasabiCertificate.filterCertificateByPermission(new int[] { permission });
	}

	public static Vector<WasabiCertificateHandle> listCertificatesByObject(String objectUUID, int permission) {
		return WasabiCertificate.filterCertificateByObject(objectUUID, new int[] { permission });
	}

	public static Vector<WasabiCertificateHandle> listCertificatesByUser(String userUUID, int permission) {
		return WasabiCertificate.filterCertificateByUser(userUUID, new int[] { permission });
	}
}
