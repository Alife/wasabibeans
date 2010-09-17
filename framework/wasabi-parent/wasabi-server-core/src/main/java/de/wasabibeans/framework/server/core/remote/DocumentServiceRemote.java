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

import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.common.WasabiConstants.SortType;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiUserDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiDocumentDTO objects.
 */
@Remote
public interface DocumentServiceRemote extends ObjectServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException;

	public WasabiValueDTO getContent(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, DocumentContentException;

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByCreationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByCreator(WasabiUserDTO creator, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByModificationDate(WasabiLocationDTO environment, Date startDate,
			Date endDate, int depth) throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsByModifier(WasabiUserDTO modifier, WasabiLocationDTO environment)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiDocumentDTO> getDocumentsOrderedByCreationDate(WasabiLocationDTO location, SortType order)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public WasabiValueDTO getEnvironment(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public boolean hasDocumentsCreatedAfter(WasabiLocationDTO environment, Long timestamp)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public boolean hasDocumentsCreatedBefore(WasabiLocationDTO environment, Long timestamp)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public boolean hasDocumentsModifiedAfter(WasabiLocationDTO environment, Long timestamp)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public boolean hasDocumentsModifiedBefore(WasabiLocationDTO environment, Long timestamp)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public void move(WasabiDocumentDTO document, WasabiLocationDTO newEnvironment, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException;

	public void remove(WasabiDocumentDTO document) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void rename(WasabiDocumentDTO document, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			ConcurrentModificationException;

	public void setContent(WasabiDocumentDTO document, Serializable content, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, DocumentContentException,
			ConcurrentModificationException;

}
