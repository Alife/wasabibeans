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

import java.io.Serializable;

import javax.jcr.Session;

import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.JmsConnector;

public interface Sink {
	class Input implements Serializable {
		public final String name;
		public final Sink sink;

		public Input(Sink sink, String name) {
			this.sink = sink;
			this.name = name;
		}

		public final String getName() {
			return name;
		}

		public final Sink getSink() {
			return sink;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Input input = (Input) o;

			if (name != null ? !name.equals(input.name) : input.name != null)
				return false;
			if (sink != null ? !sink.equals(input.sink) : input.sink != null)
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (sink != null ? sink.hashCode() : 0);
			return result;
		}
	}

	void filter(Wire fromWire, Filter.DocumentInfo document, byte[] byteBuffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException;

	boolean isAsynchronous();
}
