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
public class DelayFilter extends AnnotationBasedFilter implements Sink, Source {
	@FilterOutput
	public final Output OUTPUT = new Output(this, "OUTPUT");

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	private long delayTime = 60000;

	@FilterField(name = "delayTime", required = true)
	public long getDelayTime() {
		return delayTime;
	}

	public void setDelayTime(long delayTime) {
		this.delayTime = delayTime;
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) {
		try {
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		forward(OUTPUT, document, buffer, s, jms, sharedFilterBean);
	}

}