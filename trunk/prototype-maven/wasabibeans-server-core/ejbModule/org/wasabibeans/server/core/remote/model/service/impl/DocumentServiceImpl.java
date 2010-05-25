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

package org.wasabibeans.server.core.remote.model.service.impl;

import java.io.Serializable;
import java.util.Collection;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.wasabibeans.server.core.local.model.service.DocumentServiceLocal;
import org.wasabibeans.server.core.remote.model.service.DocumentService;
import org.wasabibeans.server.core.transfer.model.dto.WasabiDocumentDTO;
import org.wasabibeans.server.core.transfer.model.dto.WasabiLocationDTO;

/**
 * Class, that implements the remote access on WasabiDocumentDTO objects.
 */
@Stateless(name = "DocumentService")
// @WebService(name = "DocumentService")
//@SecurityDomain("wasabi")
// @SOAPBinding(style = SOAPBinding.Style.RPC, parameterStyle =
// ParameterStyle.WRAPPED, use = Use.LITERAL)
// @WebContext(contextRoot = "/wasabibeans/services", authMethod = "BASIC",
// secureWSDLAccess = true)
public class DocumentServiceImpl extends ObjectServiceImpl implements
		DocumentService {

	@EJB
	private DocumentServiceLocal documentService;

	public WasabiDocumentDTO create(String name, WasabiLocationDTO environment) {
		return documentService.create(name, environment);
	}

	public Serializable getContent(WasabiDocumentDTO document) {
		return documentService.getContent(document);
	}

	public Collection<WasabiDocumentDTO> getDocuments(WasabiLocationDTO location) {
		return documentService.getDocuments(location);
	}

	public WasabiLocationDTO getEnvironment(WasabiDocumentDTO document) {
		return documentService.getEnvironment(document);
	}

	public void move(WasabiDocumentDTO document,
			WasabiLocationDTO newEnvironment) {
		documentService.move(document, newEnvironment);
	}

	public void remove(WasabiDocumentDTO document) {
		documentService.remove(document);
	}

	public void rename(WasabiDocumentDTO document, String name) {
		documentService.rename(document, name);
	}

	public void setContent(WasabiDocumentDTO document, Serializable content) {
		documentService.setContent(document, content);
	}

	public WasabiDocumentDTO getDocumentByName(WasabiLocationDTO location,
			String name) {
		return documentService.getDocumentByName(location, name);
	}
}
