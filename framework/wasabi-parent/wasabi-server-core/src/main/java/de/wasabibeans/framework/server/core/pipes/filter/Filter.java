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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jms.JMSException;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.inject.Inject;

import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.FilterServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.util.IOUtil;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public abstract class Filter implements Serializable, Cloneable {
	private final List<Wire> wires = new LinkedList<Wire>();

	private Point point = new Point(0, 0);

	private boolean asynchronous = false;

	@FilterField(name = "asynchronous")
	public boolean isAsynchronous() {
		return asynchronous;
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	public final Map<String, Method> getFilterFields() {
		Map<String, Method> methods = new HashMap<String, Method>();

		for (Method method : getClass().getMethods()) {
			if (method.isAnnotationPresent(FilterField.class))
				methods.put(method.getAnnotation(FilterField.class).name(), method);
		}

		return methods;
	}

	public static class Point implements Serializable {
		private final int x;
		private final int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}
	}

	public static class DocumentInfo implements Serializable, Cloneable {
		private String documentNodeId;
		private MimeType contentType;
		private String name;
		private String callerPrincipal;

		private DocumentInfo(String documentNodeId, String name, MimeType contentType, String callerPrincipal) {
			this.contentType = contentType;
			this.name = name;
			this.documentNodeId = documentNodeId;
			this.callerPrincipal = callerPrincipal;
		}

		@Inject
		public DocumentInfo(Node documentNode, String callerPrincipal) throws UnexpectedInternalProblemException {
			this(ObjectServiceImpl.getUUID(documentNode), ObjectServiceImpl.getName(documentNode),
					getMimeType(documentNode), callerPrincipal);
		}

		public MimeType getContentType() {
			return contentType;
		}

		public void setContentType(MimeType contentType) {
			this.contentType = contentType;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDocumentNodeId() {
			return documentNodeId;
		}

		public String getCallerPrincipal() {
			return callerPrincipal;
		}

		private static MimeType getMimeType(Node documentNode) throws UnexpectedInternalProblemException {
			try {
				// TODO how can this ever work?????????????????????
				return new MimeType(new MimetypesFileTypeMap().getContentType(ObjectServiceImpl.getName(documentNode)));
			} catch (MimeTypeParseException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected DocumentInfo clone() {
			return new DocumentInfo(documentNodeId, name, contentType, callerPrincipal);

		}
	}

	public final Point getPoint() {
		return point;
	}

	public final void setPoint(Point point) {
		this.point = point;
	}

	public List<Wire> getWires() {
		return wires;
	}

	public abstract String getFilterName();

	public abstract String getFilterCategory();

	public abstract void filter(Wire fromWire, Filter.DocumentInfo document, byte[] byteBuffer, Session s,
			JmsConnector jms, SharedFilterBean sharedFilterBean);

	protected final void forward(Source.Output output, DocumentInfo documentInfo, byte[] buffer, Session s,
			JmsConnector jms, SharedFilterBean sharedFilterBean) {
		for (Wire wire : wires) {
			if (wire.getFrom().equals(output)) {
				if (wire.to.sink.isAsynchronous()) {
					try {
						FilterServiceImpl.executeFilterAsynchronous(wire, documentInfo.clone(), buffer, jms,
								sharedFilterBean);
					} catch (JMSException e) {
						throw new RuntimeException(e);
					} catch (UnexpectedInternalProblemException e) {
						throw new RuntimeException(e);
					}
				} else {
					wire.to.sink.filter(wire, documentInfo.clone(), buffer, s, jms, sharedFilterBean);
				}
			}
		}
	}

	public void connect(Source.Output output, Sink.Input input) {
		wires.add(new Wire(output, input));
	}

	public abstract void getModuleDefinition(JSONWriter stringer) throws JSONException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, InstantiationException;

	@Override
	public Object clone() {
		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(IOUtil.convert2Byte(this)));
			return in.readObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

	}
}
