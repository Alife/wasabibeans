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

import javax.activation.MimeTypeParseException;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;

import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Source;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterOutput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

@MetaInfServices(Filter.class)
public class MimeFilter extends AnnotationBasedFilter implements Sink, Source {
	private String mimeType;

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	@FilterOutput
	public final Output MATCH = new Output(this, "MATCH");
	@FilterOutput
	public final Output NOMATCH = new Output(this, "NOMATCH");

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) {
		try {
			if (document.getContentType().match(mimeType)) {
				forward(MATCH, document, buffer, s, jms, sharedFilterBean);
			} else {
				forward(NOMATCH, document, buffer, s, jms, sharedFilterBean);
			}
		} catch (MimeTypeParseException e) {
			throw new RuntimeException(e);
		}
	}

	@FilterField(name = "mimeType", required = true)
	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}
