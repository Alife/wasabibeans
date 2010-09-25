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

public class Wire implements Serializable {
	public final Source.Output from;
	public final Sink.Input to;

	Wire(Source.Output from, Sink.Input to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Wire wire = (Wire) o;

		if (!from.equals(wire.from))
			return false;
		if (!to.equals(wire.to))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = from.hashCode();
		result = 31 * result + to.hashCode();
		return result;
	}

	public final Source.Output getFrom() {
		return from;
	}

	public final Sink.Input getTo() {
		return to;
	}
}
