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

package de.wasabibeans.framework.server.core.authentication;

import java.security.Principal;

import javax.jcr.Node;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.jboss.security.SimplePrincipal;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;

public class FlushLoginCache {

	public static void flushLoginCache(Node userNode) throws UnexpectedInternalProblemException {
		try {
			String username = ObjectServiceImpl.getName(userNode);

			String domain = "jmx-console";
			Principal user = new SimplePrincipal(username);
			ObjectName jaasMgr = new ObjectName("jboss.security:service=JaasSecurityManager");
			Object[] params = { domain, user };
			String[] signature = { "java.lang.String", Principal.class.getName() };
			MBeanServer server = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
			server.invoke(jaasMgr, "flushAuthenticationCache", params, signature);
		} catch (Exception e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JMX_FLUSH_CACHE, e);
		}
	}
}
