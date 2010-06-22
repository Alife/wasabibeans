/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.remote;

import java.io.Serializable;
import java.util.Collection;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;

/**
 * Interface, that defines the remote access on WasabiDocumentDTO objects.
 */
@Remote(de.wasabibeans.framework.server.core.internal.DocumentService.class)
public interface DocumentServiceRemote extends ObjectServiceRemote {

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment);

	public Serializable getContent(WasabiDocumentDTO document);

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location,
			String name);

	public Collection<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location);

//	public Collection<WasabiDocumentDTO> getDocumentsByCreationDate(
//			WasabiLocationDTO environment, Date startDate, Date endDate);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByCreationDate(
//			WasabiLocationDTO environment, Date startDate, Date endDate,
//			int depth);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByCreator(
//			WasabiUserDTO creator);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByCreator(
//			WasabiUserDTO creator, WasabiLocationDTO environment);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByModificationDate(
//			WasabiLocationDTO environment, Date startDate, Date endDate);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByModificationDate(
//			WasabiLocationDTO environment, Date startDate, Date endDate,
//			int depth);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByModifier(
//			WasabiUserDTO modifier);
//
//	public Collection<WasabiDocumentDTO> getDocumentsByModifier(
//			WasabiUserDTO modifier, WasabiLocationDTO environment);
//
//	public Collection<WasabiDocumentDTO> getDocumentsOrderedByCreationDate(
//			WasabiLocationDTO location, SortType order);

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document);

//	public boolean hasDocumentsCreatedAfter(WasabiLocationDTO environment,
//			Long timestamp);
//
//	public boolean hasDocumentsCreatedBefore(WasabiLocationDTO environment,
//			Long timestamp);
//
//	public boolean hasDocumentsModifiedAfter(WasabiLocationDTO environment,
//			Long timestamp);
//
//	public boolean hasDocumentsModifiedBefore(WasabiLocationDTO environment,
//			Long timestamp);

	public void move(WasabiDocumentDTO document,
			WasabiLocationDTO newEnvironment);

	public void remove(WasabiDocumentDTO document);

	public void rename(WasabiDocumentDTO document, String name);

	public void setContent(WasabiDocumentDTO document, Serializable content);

}
