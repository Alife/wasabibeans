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

package de.wasabibeans.framework.server.core.pipes.filter.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.ContentStore;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

/**
 * This sink stores the content in the Document itself.
 */
@MetaInfServices(Filter.class)
public class DocumentSink extends AnnotationBasedFilter implements ContentStore, Sink {

	@FilterInput(maxConnections = 1)
	public final Input INPUT = new Input(this, "INPUT");

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException {
		Node documentNode = ObjectServiceImpl.get(document.getDocumentNodeId(), s);
		DocumentServiceImpl.setContent(documentNode, buffer, s, false, document.getCallerPrincipal());
		boolean doJcrSave = isAsynchronous() ? true : WasabiConstants.JCR_SAVE_PER_METHOD;
		DocumentServiceImpl.addContentRef(documentNode, this, document.getName(), document.getContentType().toString(),
				(long) buffer.length, true, s, doJcrSave, document.getCallerPrincipal());
	}

	@Override
	public InputStream getContent(Node ref) {
		try {
			return new ByteArrayInputStream((byte[]) DocumentServiceImpl.getContent(DocumentServiceImpl
					.getDocument(ref)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}