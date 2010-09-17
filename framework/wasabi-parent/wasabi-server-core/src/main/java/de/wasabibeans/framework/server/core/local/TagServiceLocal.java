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

import java.util.Map;
import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the local access on Tags of WasabiObjectsDTO.
 */
@Local
public interface TagServiceLocal extends ObjectServiceLocal {

	public void addTag(WasabiObjectDTO object, String tag) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException;

	public void clearTags(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException;

	/**
	 * Parameter {@code limit} does nothing currently.
	 * 
	 * @param environment
	 * @param limit
	 * @return
	 * @throws ObjectDoesNotExistException
	 * @throws UnexpectedInternalProblemException
	 */
	public Map<String, Integer> getMostUsedDocumentTags(WasabiLocationDTO environment, int limit)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException;

	public Vector<WasabiObjectDTO> getObjectsByTag(String tag) throws UnexpectedInternalProblemException;

	public Vector<String> getTags(WasabiObjectDTO object) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException;

	public void removeTag(WasabiObjectDTO object, String tag) throws ObjectDoesNotExistException,
			UnexpectedInternalProblemException, ConcurrentModificationException;

	public Vector<WasabiDocumentDTO> getDocumentsByTags(WasabiLocationDTO environment, Vector<String> tags)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;
}
