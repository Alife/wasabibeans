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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.JSONException;
import org.json.JSONWriter;

import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterOutput;

public abstract class AnnotationBasedFilter extends Filter {
	public final void getModuleDefinition(JSONWriter stringer) throws JSONException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, InstantiationException {
		Class<? extends AnnotationBasedFilter> filter = getClass();

		stringer.object();

		stringer.key("name").value(getFilterName());
		stringer.key("category").value(getFilterCategory());
		stringer.key("container").object();

		stringer.key("xtype").value("WireIt.InOutFormContainer");

		stringer.key("fields").array();
		for (Method method : filter.getMethods()) {
			FilterField field = method.getAnnotation(FilterField.class);
			if (field != null) {
				// {"inputParams": {"label": "Firstname", "name": "firstname", "required": true } },
				stringer.object();
				{
					stringer.key("type").value(getType(field, method));

					stringer.key("inputParams").object().key("label").value(field.name()).key("name").value(
							field.name()).key("required").value(field.required());

					// get the default value
					stringer.key("value").value(method.invoke(filter.newInstance()));

					if (method.getReturnType().isEnum()) {
						stringer.key("selectValues").array();
						if (!field.required())
							stringer.value(null);
						for (Enum e : (Enum[]) method.getReturnType().getMethod("values")
								.invoke(method.getReturnType())) {
							stringer.value(e.name());
						}
						stringer.endArray();
					}
					stringer.endObject();
				}
				stringer.endObject();
			}
		}
		stringer.endArray();

		stringer.key("inputs").array();
		for (Field field : filter.getFields()) {
			FilterInput input = field.getAnnotation(FilterInput.class);
			if (input != null) {
				stringer.object();
				stringer.key("name").value(field.getName());
				if (input.maxConnections() != -1)
					stringer.key("nMaxWires").value(input.maxConnections());
				stringer.endObject();
			}
		}
		stringer.endArray();

		stringer.key("outputs").array();
		for (Field field : filter.getFields()) {
			FilterOutput output = field.getAnnotation(FilterOutput.class);
			if (output != null) {
				stringer.object();
				stringer.key("name").value(field.getName());
				stringer.endObject();
			}
		}
		stringer.endArray();

		stringer.endObject();// container

		stringer.endObject();
	}

	private static String getType(FilterField field, Method method) {
		if (field.type().equals("")) {
			if (method.getReturnType().isEnum()) {
				return "select";
			} else {
				return method.getReturnType().getSimpleName().toLowerCase();
			}
		} else {
			return field.type();
		}
	}

	@Override
	public final String getFilterName() {
		return getClass().getSimpleName();
	}

	@Override
	public String getFilterCategory() {
		if (this instanceof ContentStore) {
			return "DataStore";
		} else if (!(this instanceof Sink) || !(this instanceof Source)) {
			return "Start/End";
		} else {
			// no category
			return null;
		}
	}
}
