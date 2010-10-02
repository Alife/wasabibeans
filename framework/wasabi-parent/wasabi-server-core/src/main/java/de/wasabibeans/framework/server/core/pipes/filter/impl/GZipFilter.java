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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;

import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Source;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterOutput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

@MetaInfServices(Filter.class)
public class GZipFilter extends AnnotationBasedFilter implements Sink, Source {

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	@FilterOutput
	public final Output OUTPUT = new Output(this, "OUTPUT");

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			GZIPOutputStream gos = new GZIPOutputStream(bos);

			gos.write(buffer);

			gos.close();

			document.setName(document.getName() + ".gz");
			document.setContentType(new MimeType("application/x-gzip"));

			forward(OUTPUT, document, bos.toByteArray(), s, jms, sharedFilterBean);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (MimeTypeParseException e) {
			throw new RuntimeException(e);
		}
	}
}
