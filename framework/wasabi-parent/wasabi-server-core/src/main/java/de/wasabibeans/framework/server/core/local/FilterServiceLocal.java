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

package de.wasabibeans.framework.server.core.local;

import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiPipelineDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;

/**
 * Interface, that defines the local access on WasabiPipelineDTO objects.
 */
@Local
public interface FilterServiceLocal extends ObjectServiceLocal {

	WasabiPipelineDTO create(String name, Filter filter) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, NoPermissionException, ObjectDoesNotExistException,
			ConcurrentModificationException;

	WasabiPipelineDTO getPipeline(String name) throws UnexpectedInternalProblemException, NoPermissionException;

	Vector<WasabiPipelineDTO> getPipelines() throws UnexpectedInternalProblemException, NoPermissionException;

	void remove(WasabiPipelineDTO pipeline) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			ConcurrentModificationException, NoPermissionException;

	void updateOrCreate(String name, Filter filter) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException, NoPermissionException, ObjectDoesNotExistException,
			ConcurrentModificationException;
}