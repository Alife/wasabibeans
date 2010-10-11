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

package de.wasabibeans.framework.server.core.remote;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Remote
public interface AuthorizationServiceRemote {

	public boolean existsCertificate(WasabiObjectDTO wasabiObject, WasabiUserDTO wasabiUser, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException;

	public boolean hasPermission(WasabiObjectDTO wasabiObject, int permission) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException;

	public boolean hasPermission(WasabiObjectDTO wasabiObject, WasabiUserDTO wasabiUser, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException;

	public boolean listCertificates(int permission) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, NoPermissionException;

	public boolean listCertificatesByObject(WasabiUserDTO wasabiUser, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException;

	public boolean listCertificatesByUser(WasabiObjectDTO wasabiObject, int permission)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, NoPermissionException;

	public boolean returnTrue();

}
