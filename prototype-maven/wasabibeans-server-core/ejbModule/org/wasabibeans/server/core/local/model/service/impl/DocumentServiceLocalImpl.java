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

package org.wasabibeans.server.core.local.model.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jcr.NodeIterator;

import org.wasabibeans.server.core.internal.model.service.DocumentServiceInternal;
import org.wasabibeans.server.core.internal.model.service.ObjectServiceInternal;
import org.wasabibeans.server.core.local.model.service.DocumentServiceLocal;
import org.wasabibeans.server.core.transfer.model.dto.WasabiDocumentDTO;
import org.wasabibeans.server.core.transfer.model.dto.WasabiLocationDTO;

/**
 * Class, that implements the local access on WasabiDocumentDTO objects.
 */
@Stateless(name = "DocumentServiceLocal")
public class DocumentServiceLocalImpl extends ObjectServiceLocalImpl implements
		DocumentServiceLocal {

	@EJB
	private DocumentServiceInternal documentService;

	@EJB
	ObjectServiceInternal objectService;

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment) {
		return convertNode2DTO(documentService.create(name,
				convertDTO2Node(environment)));
	}

	public Serializable getContent(WasabiDocumentDTO document) {
		return documentService.getContent(convertDTO2Node(document));
	}

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document) {
		return (WasabiLocationDTO) convertNode2DTO(documentService
				.getEnvironment(convertDTO2Node(document)));
	}

	public Collection<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location) {
		ArrayList<WasabiDocumentDTO> documents = new ArrayList<WasabiDocumentDTO>();
		NodeIterator iter = documentService
				.getDocuments(convertDTO2Node(location));
		while (iter.hasNext()) {
			documents.add((WasabiDocumentDTO) convertNode2DTO(iter.nextNode()));
		}
		return documents;
	}

	public void move(WasabiDocumentDTO document,
			WasabiLocationDTO newEnvironment) {
		documentService.move(convertDTO2Node(document),
				convertDTO2Node(newEnvironment));
	}

	public void remove(WasabiDocumentDTO document) {
		documentService.remove(convertDTO2Node(document));
	}

	public void rename(WasabiDocumentDTO document, String name) {
		documentService.rename(convertDTO2Node(document), name);
	}

	public void setContent(WasabiDocumentDTO document, Serializable content) {
		documentService.setContent(convertDTO2Node(document), content);
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location,
			String name) {
		return convertNode2DTO(documentService.getDocumentByName(
				convertDTO2Node(location), name));
	}
}
