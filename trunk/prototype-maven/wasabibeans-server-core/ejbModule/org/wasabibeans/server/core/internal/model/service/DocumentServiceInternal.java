/* 
 * Copyright (C) 2007-2009 
 * Thorsten Hampel, Jonas Schulte, Daniel Buese,
 * Andreas Oberhoff, Thomas Bopp, and Robert Hinn
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

package org.wasabibeans.server.core.internal.model.service;

import java.io.Serializable;

import javax.ejb.Local;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Interface, that defines the internal access on WasabiDocument objects.
 */
@Local(org.wasabibeans.server.core.internal.model.service.impl.DocumentServiceInternalImpl.class)
public interface DocumentServiceInternal extends ObjectServiceInternal {

	// /**
	// * Method to copy a WasabiDocument. It does not copy ACL entries.
	// *
	// * @param wasabiDocument WasabiDocument to copy
	// * @param destination destination of the copy
	// */
	// public void copy(WasabiDocument wasabiDocument,
	// WasabiLocation destination);

	public Node create(String name, Node environment);

	public Serializable getContent(Node wasabiDocument);

	public Node getDocumentByName(Node wasabiLocation, String name);

	public NodeIterator getDocuments(Node wasabiLocation);

	// public Collection<WasabiDocument> getDocumentsByCreationDate(
	// WasabiLocation environment, Date startDate, Date endDate);

	// public Collection<WasabiDocument> getDocumentsByCreationDate(
	// WasabiLocation environment, Date startDate, Date endDate, int depth);

	// public Collection<WasabiDocument> getDocumentsByCreator(WasabiUser
	// creator);

	// public Collection<WasabiDocument> getDocumentsByCreator(WasabiUser
	// creator,
	// WasabiLocation environment);

	// public Collection<WasabiDocument> getDocumentsByModificationDate(
	// WasabiLocation environment, Date startDate, Date endDate);

	// public Collection<WasabiDocument> getDocumentsByModificationDate(
	// WasabiLocation environment, Date startDate, Date endDate, int depth);

	// public Collection<WasabiDocument> getDocumentsByModifier(WasabiUser
	// modifier);

	// public Collection<WasabiDocument> getDocumentsByModifier(
	// WasabiUser modifier, WasabiLocation environment);

	// public Collection<WasabiDocument> getDocumentsOrderedByCreationDate(
	// WasabiLocation wasabiLocation, SortType order);

	public Node getEnvironment(Node wasabiDocument);

	// public boolean hasDocumentsCreatedAfter(WasabiLocation environment,
	// Long timestamp);

	// public boolean hasDocumentsCreatedBefore(WasabiLocation environment,
	// Long timestamp);

	// public boolean hasDocumentsModifiedAfter(WasabiLocation environment,
	// Long timestamp);

	// public boolean hasDocumentsModifiedBefore(WasabiLocation environment,
	// Long timestamp);

	public void move(Node wasabiDocument, Node newEnvironment);

	public void remove(Node wasabiDocument);

	public void rename(Node wasabiDocument, String name);

	public void setContent(Node wasabiDocument, Serializable content);
}
