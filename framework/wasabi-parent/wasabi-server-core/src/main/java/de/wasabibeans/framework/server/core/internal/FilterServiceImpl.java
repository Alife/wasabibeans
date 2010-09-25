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

package de.wasabibeans.framework.server.core.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.gson.Gson;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.FilterRegistry;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Source;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.impl.DocumentSource;
import de.wasabibeans.framework.server.core.pipes.filter.impl.EmbeddedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.impl.NamedSink;
import de.wasabibeans.framework.server.core.pipes.filter.impl.NamedSource;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public class FilterServiceImpl {

	public static Node create(String name, Filter filter, Session s) throws ObjectAlreadyExistsException,
			UnexpectedInternalProblemException {
		try {
			Node rootOfPipelinesNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_USERS_NAME);
			Node pipelineNode = rootOfPipelinesNode.addNode(name, WasabiNodeType.PIPELINE);
			pipelineNode.setProperty(WasabiNodeProperty.EMBEDDABLE, filter instanceof EmbeddedFilter);
			setFilter(pipelineNode, filter, s);

			return pipelineNode;
		} catch (ItemExistsException iee) {
			throw new ObjectAlreadyExistsException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_OBJECT_ALREADY_EXISTS, "pipeline", name), name, iee);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void updateOrCreate(String name, Filter filter, Session s) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		Node pipelineNode = getPipeline(name, s);

		if (pipelineNode != null) {
			setFilter(pipelineNode, filter, s);
		} else {
			create(name, filter, s);
		}
	}

	public static boolean isEmbeddable(Node pipelineNode) throws UnexpectedInternalProblemException {
		try {
			return pipelineNode.getProperty(WasabiNodeProperty.EMBEDDABLE).getBoolean();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setFilter(Node pipelineNode, Filter filter, Session s) throws UnexpectedInternalProblemException {
		try {
			// convert filter to inputstream
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(filter);
			out.flush();
			out.close();
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
			// store the filter
			pipelineNode.setProperty(WasabiNodeProperty.FILTER, s.getValueFactory().createBinary(in));
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (IOException io) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_VALUE_SAVE, "filter"), io);
		}
	}

	public static Filter getFilter(Node pipelineNode) throws UnexpectedInternalProblemException {
		try {
			Binary filter = pipelineNode.getProperty(WasabiNodeProperty.FILTER).getBinary();
			ObjectInputStream oIn = new ObjectInputStream(filter.getStream());
			return (Filter) oIn.readObject();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_VALUE_LOAD, "filter", "pipeline"), e);
		}
	}

	public static Node getPipeline(String name, Session s) throws UnexpectedInternalProblemException {
		try {
			Node rootOfPipelinesNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_PIPELINES);
			return rootOfPipelinesNode.getNode(name);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static NodeIterator getPipelines(Session s) throws UnexpectedInternalProblemException {
		try {
			Node rootOfPipelinesNode = s.getRootNode().getNode(WasabiConstants.JCR_ROOT_FOR_PIPELINES);
			return rootOfPipelinesNode.getNodes();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static Map<String, EmbeddedFilter> getEmbeddedFilters(Session s) throws UnexpectedInternalProblemException {
		Map<String, EmbeddedFilter> map = new HashMap<String, EmbeddedFilter>();

		for (NodeIterator ni = ObjectServiceImpl.getNodeByPropertyBooleanValue(WasabiNodeType.PIPELINE,
				WasabiNodeProperty.EMBEDDABLE, true, s); ni.hasNext();) {
			Node pipelineNode = ni.nextNode();
			map.put(ObjectServiceImpl.getName(pipelineNode), (EmbeddedFilter) getFilter(pipelineNode));
		}

		return map;
	}

	public static void remove(Node pipelineNode) throws UnexpectedInternalProblemException,
			ConcurrentModificationException {
		ObjectServiceImpl.remove(pipelineNode);
	}

	public static void executeFilterAsynchronous(Wire wire, Filter.DocumentInfo info, byte[] data, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws JMSException, UnexpectedInternalProblemException {
		try {
			Connection jmsConnection = jms.getJmsConnection();
			javax.jms.Session jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			MessageProducer jmsProducer = jmsSession.createProducer(jms.getPipelineQueue());
			Message message = jmsSession.createMessage();
			long taskId = sharedFilterBean.createTask(wire, info, data);
			message.setLongProperty("taskId", taskId);
			jmsProducer.send(message);
		} finally {
			jms.close();
		}
	}

	public static void apply(Node pipelineNode, Node documentNode, Serializable content, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean, String callerPrincipal) throws UnexpectedInternalProblemException {
		getFilter(pipelineNode).filter(null, new Filter.DocumentInfo(documentNode, callerPrincipal), (byte[]) content,
				s, jms, sharedFilterBean);
	}

	public static void toJSONString(Node pipelineNode, JSONWriter stringer) {
		try {
			Set<Filter> filterSet = new HashSet();

			if (isEmbeddable(pipelineNode)) {
				for (Filter filter : ((EmbeddedFilter) getFilter(pipelineNode)).getSources().values()) {
					filterSet.addAll(FilterRegistry.getFilterRec(filter));
				}
				for (Filter filter : ((EmbeddedFilter) getFilter(pipelineNode)).getSinks().values()) {
					filterSet.addAll(FilterRegistry.getFilterRec(filter));
				}
			} else {
				filterSet.addAll(FilterRegistry.getFilterRec(getFilter(pipelineNode)));
			}

			List<Filter> filters = new ArrayList<Filter>(filterSet);

			stringer.object(); // s.append("{\n");

			stringer.key("modules").array();// s.append(" \"modules\":[\n");

			Set<Wire> wireSet = new HashSet<Wire>();

			for (Filter filter : filters) {
				wireSet.addAll(filter.getWires());
				stringer.object(); // s.append("  {\n");
				stringer.key("config").object();// s.append("   \"config\":{\n");
				stringer.key("position").array().value(filter.getPoint().getX()).value(filter.getPoint().getY())
						.endArray();// s.append("    \"position\":["+filter.getPoint().getX()+","+filter.getPoint().getY()+"], \n");
				// s.append("    \"xtype\":\"WireIt.InOutFormContainer\" \n");
				stringer.endObject();// s.append("   },\n");
				stringer.key("name").value(filter.getFilterName());// s.append("   \"name\":\""+filter.getClass().getName()+"\",\n");

				stringer.key("value").object();// s.append("   \"value\":{}\n");

				for (Map.Entry<String, Method> entry : filter.getFilterFields().entrySet()) {
					stringer.key(entry.getKey());
					stringer.value(entry.getValue().invoke(filter));
				}

				stringer.endObject();

				stringer.endObject(); // s.append("  },\n");
			}

			stringer.endArray(); // s.append(" ],\n");

			stringer.key("wires").array();

			List<Wire> wires = new ArrayList<Wire>(wireSet);

			for (Wire wire : wires) {
				stringer.object();

				stringer.key("src").object().key("moduleId").value(filters.indexOf(wire.from.source)).key("terminal")
						.value(wire.from.name).endObject();
				stringer.key("tgt").object().key("moduleId").value(filters.indexOf(wire.to.sink)).key("terminal")
						.value(wire.to.name).endObject();

				stringer.endObject();
			}

			stringer.endArray();

			stringer.key("properties").object();
			stringer.key("name").value(ObjectServiceImpl.getName(pipelineNode));
			stringer.key("embeddable").value(isEmbeddable(pipelineNode));
			stringer.endObject();

			stringer.endObject(); // s.append("}\n");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (UnexpectedInternalProblemException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toJSONString(Node pipelineNode) {
		JSONStringer stringer = new JSONStringer();
		toJSONString(pipelineNode, stringer);
		return stringer.toString();
	}

	public static PipelineData fromJSONString(String jsonString, Session s) {
		try {
			JSONObject jsonObject = new JSONObject(new JSONTokener(jsonString));

			List<Filter> filters = getFilter(jsonObject, s);
			Filter source = null;

			if (jsonObject.getJSONObject("properties").getBoolean("embeddable")) {
				Map<String, NamedSource> sources = new HashMap<String, NamedSource>();
				Map<String, NamedSink> sinks = new HashMap<String, NamedSink>();
				for (Filter filter : filters) {
					if (filter instanceof NamedSource) {
						NamedSource namedSource = (NamedSource) filter;
						sources.put(namedSource.getName(), namedSource);
					}
					if (filter instanceof NamedSink) {
						NamedSink namedSink = (NamedSink) filter;
						sinks.put(namedSink.getName(), namedSink);
					}
				}

				source = new EmbeddedFilter(jsonObject.getJSONObject("properties").getString("name"), sources, sinks);
			} else {
				for (Filter filter : filters) {
					if (filter instanceof DocumentSource)
						source = filter;
				}
			}

			return new PipelineData(jsonObject.getJSONObject("properties").getString("name"), source);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (UnexpectedInternalProblemException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<Filter> getFilter(JSONObject jsonObject, Session s) throws JSONException,
			InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			CloneNotSupportedException, UnexpectedInternalProblemException {
		JSONArray filterArray = jsonObject.getJSONArray("modules");
		List<Filter> filters = new ArrayList<Filter>(filterArray.length());
		for (int i = 0; i < filterArray.length(); i++) {
			JSONObject jsonFilter = filterArray.getJSONObject(i);

			Filter filter = (Filter) FilterRegistry.instance().getRegisteredFilters(s)
					.get(jsonFilter.getString("name")).clone();

			JSONObject value = jsonFilter.getJSONObject("value");
			for (Iterator it = value.keys(); it.hasNext();) {
				String key = (String) it.next();
				getSetter(filter.getClass(), filter.getFilterFields().get(key)).invoke(filter,
						convert(filter.getFilterFields().get(key), value.get(key)));
			}

			JSONArray jsonPosition = jsonFilter.getJSONObject("config").getJSONArray("position");
			filter.setPoint(new Filter.Point(jsonPosition.getInt(0), jsonPosition.getInt(1)));

			filters.add(filter);
		}

		JSONArray wireArray = jsonObject.getJSONArray("wires");
		for (int i = 0; i < wireArray.length(); i++) {
			JSONObject jsonWire = wireArray.getJSONObject(i);

			Source from = (Source) filters.get(jsonWire.getJSONObject("src").getInt("moduleId"));
			String fromTerm = jsonWire.getJSONObject("src").getString("terminal");
			Sink to = (Sink) filters.get(jsonWire.getJSONObject("tgt").getInt("moduleId"));
			String toTerm = jsonWire.getJSONObject("tgt").getString("terminal");

			from.connect(new Source.Output(from, fromTerm), new Sink.Input(to, toTerm));
		}

		return filters;
	}

	private static Object convert(Method method, Object s) {
		if (method.getReturnType().isEnum()) {
			return s == null || "null".equals(s) ? null : Enum.valueOf((Class) method.getReturnType(), s.toString());
		} else if (s instanceof String && method.getReturnType() != String.class) {
			return new Gson().fromJson((String) s, method.getReturnType());
		} else {
			return s;
		}
	}

	private static final Method getSetter(Class<?> clazz, Method getter) throws NoSuchMethodException {
		String setterName = "set"
				+ (getter.getName().startsWith("is") ? getter.getName().substring(2) : getter.getName().substring(3));

		return clazz.getMethod(setterName, getter.getReturnType());
	}

	public static class PipelineData implements Serializable {

		private static final long serialVersionUID = -6526635993312333654L;

		private String name;
		private Filter filter;

		public PipelineData(String name, Filter filter) {
			this.name = name;
			this.filter = filter;
		}

		public String getName() {
			return name;
		}

		public Filter getFilter() {
			return filter;
		}
	}
}
