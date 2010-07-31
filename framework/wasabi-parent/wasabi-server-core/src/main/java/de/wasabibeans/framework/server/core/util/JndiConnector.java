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

package de.wasabibeans.framework.server.core.util;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

public class JndiConnector {

	private InitialContext jndiContext;
	private WasabiLogger logger;
	public static JndiConnector getJNDIConnector() {
		return new JndiConnector();
	}

	public JndiConnector() {
		this.logger = WasabiLogger.getLogger(this.getClass());
	}

	public InitialContext getJNDIContext() throws UnexpectedInternalProblemException {
		try {
			if (this.jndiContext == null) {
				this.jndiContext = new InitialContext();
			}
			return this.jndiContext;
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JNDI_NO_CONTEXT, ne);
		}
	}

	public Object lookupLocal(String name) throws UnexpectedInternalProblemException {
		try {
			return getJNDIContext().lookup(name + "/local");
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_LOOKUP, name), ne);
		}
	}

	public Object lookup(String name) throws UnexpectedInternalProblemException {
		try {
			return getJNDIContext().lookup(name);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_LOOKUP, name), ne);
		}
	}

	public void bind(String name, Object o) throws UnexpectedInternalProblemException {
		try {
			getJNDIContext().bind(name, o);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_BIND, name), ne);
		}
	}

	public void unbind(String name) throws UnexpectedInternalProblemException {
		try {
			getJNDIContext().unbind(name);
		} catch (NameNotFoundException nnfe) {
			logger.info(WasabiExceptionMessages.get(WasabiExceptionMessages.JNDI_FAILED_UNBIND_NAME_NOT_BOUND, name),
					nnfe);
		} catch (NamingException ne) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.JNDI_FAILED_UNBIND, name), ne);
		}
	}
}

