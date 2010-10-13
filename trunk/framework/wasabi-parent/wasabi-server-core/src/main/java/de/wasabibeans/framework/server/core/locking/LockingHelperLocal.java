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

package de.wasabibeans.framework.server.core.locking;

import javax.ejb.Local;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface LockingHelperLocal {

	public String acquireLock(String nodePath, boolean isDeep) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException;

	public void releaseLock(String nodeId, String lockToken) throws AccessDeniedException, LockException,
			PathNotFoundException, InvalidItemStateException, UnsupportedRepositoryOperationException,
			RepositoryException, UnexpectedInternalProblemException;
}
