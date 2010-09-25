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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.jcr.Session;

import org.json.JSONException;
import org.json.JSONWriter;

import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Source;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class EmbeddedFilter extends Filter implements Source, Sink {

	private final String filterName;

	private final Map<String, NamedSource> sources;
	private final Map<String, NamedSink> sinks;

	public EmbeddedFilter(String filterName, Map<String, NamedSource> sources, Map<String, NamedSink> sinks) {
		this.filterName = filterName;
		this.sources = sources;
		this.sinks = sinks;

		for (NamedSink sink : sinks.values()) {
			sink.setEmbeddedFilter(this);
		}
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] byteBuffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) {
		if (fromWire.to.sink instanceof NamedSink) {
			forward(new Output(this, ((NamedSink) fromWire.to.sink).getName()), document, byteBuffer, s, jms,
					sharedFilterBean);
		} else {
			sources.get(fromWire.to.getName()).filter(fromWire, document, byteBuffer, s, jms, sharedFilterBean);
		}
	}

	@Override
	public void getModuleDefinition(JSONWriter stringer) throws JSONException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, InstantiationException {

		stringer.object();

		stringer.key("name").value(filterName);
		stringer.key("category").value(getFilterCategory());
		stringer.key("container").object();

		stringer.key("xtype").value("WireIt.InOutFormContainer");

		stringer.key("fields").array().endArray();

		stringer.key("inputs").array();
		for (NamedSource source : sources.values()) {
			stringer.object();
			stringer.key("name").value(source.getName());
			stringer.endObject();
		}
		stringer.endArray();

		stringer.key("outputs").array();
		for (NamedSink sink : sinks.values()) {
			stringer.object();
			stringer.key("name").value(sink.getName());
			stringer.endObject();
		}
		stringer.endArray();

		stringer.endObject();// container

		stringer.endObject();
	}

	@Override
	public String getFilterName() {
		return filterName;
	}

	public Map<String, NamedSource> getSources() {
		return sources;
	}

	public Map<String, NamedSink> getSinks() {
		return sinks;
	}

	@Override
	public boolean isAsynchronous() {
		return false;
	}

	@Override
	public String getFilterCategory() {
		return "Embedded";
	}
}