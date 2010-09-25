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

package de.wasabibeans.framework.server.core.pipes.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.jcr.Session;

import org.json.JSONException;
import org.json.JSONStringer;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.FilterServiceImpl;

public class FilterRegistry {
	private Map<String, Filter> registeredFilters = new HashMap<String, Filter>();

	private static FilterRegistry instance = new FilterRegistry();

	public FilterRegistry() {
		ServiceLoader<Filter> filterServiceLoader = ServiceLoader.load(Filter.class);
		for (Filter filter : filterServiceLoader) {
			registerFilter(filter);
		}
	}

	public void registerFilter(Filter filter) {
		registeredFilters.put(filter.getFilterName(), filter);
	}

	public Map<String, Filter> getRegisteredFilters(Session s) throws UnexpectedInternalProblemException {
		Map<String, Filter> map = new HashMap<String, Filter>();
		map.putAll(registeredFilters);
		map.putAll(FilterServiceImpl.getEmbeddedFilters(s));
		return map;
	}

	public static Set<Filter> getFilterRec(Filter filter) {
		Set<Filter> set = new HashSet<Filter>();

		set.add(filter);

		for (Wire wire : filter.getWires()) {
			set.addAll(getFilterRec((Filter) wire.to.sink));
		}

		return set;
	}

	public String getModuleDefinitions(Session s) {
		JSONStringer stringer = new JSONStringer();

		try {
			stringer.object();

			stringer.key("languageName").value("wasabiPipelineLanguage");

			stringer.key("propertiesFields").array();
			stringer.object();
			stringer.key("type").value("string");
			stringer.key("inputParams").object().key("name").value("name").key("typeInvite").value("Enter a title")
					.key("label").value("Title").endObject();
			stringer.endObject();

			stringer.object();
			stringer.key("type").value("text");
			stringer.key("inputParams").object().key("name").value("description").key("cols").value(30).key("label")
					.value("Description").endObject();
			stringer.endObject();

			stringer.object();
			stringer.key("type").value("boolean");
			stringer.key("inputParams").object().key("name").value("embeddable").key("value").value(false).key("label")
					.value("Embeddable").endObject();
			stringer.endObject();
			stringer.endArray();

			stringer.key("modules").array();

			for (Filter filter : getRegisteredFilters(s).values()) {
				filter.getModuleDefinition(stringer);
			}

			stringer.endArray();

			stringer.endObject();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (UnexpectedInternalProblemException e) {
			throw new RuntimeException(e);
		}

		return stringer.toString();
	}

	public static void create(FilterRegistry registry) {
		instance = registry;
	}

	public static FilterRegistry instance() {
		return instance;
	}
}
