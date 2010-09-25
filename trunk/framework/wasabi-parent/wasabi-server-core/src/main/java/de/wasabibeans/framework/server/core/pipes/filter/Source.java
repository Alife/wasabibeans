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

public interface Source {
	final class Output implements Serializable {
		public final String name;
		public final Source source;

		public Output(Source source, String name) {
			this.source = source;
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Output output = (Output) o;

			if (name != null ? !name.equals(output.name) : output.name != null)
				return false;
			if (source != null ? !source.equals(output.source) : output.source != null)
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (source != null ? source.hashCode() : 0);
			return result;
		}

		public String getName() {
			return name;
		}

		public Source getSource() {
			return source;
		}
	}

	void connect(Source.Output output, Sink.Input input);
}
