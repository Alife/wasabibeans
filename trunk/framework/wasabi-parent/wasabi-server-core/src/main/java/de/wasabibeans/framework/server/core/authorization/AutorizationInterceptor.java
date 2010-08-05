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

package de.wasabibeans.framework.server.core.authorization;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jcr.Session;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.internal.GroupServiceImpl;
import de.wasabibeans.framework.server.core.internal.UserServiceImpl;
import de.wasabibeans.framework.server.core.util.JcrConnector;

public class AutorizationInterceptor {

	@Resource
	private SessionContext sessionContext;

	protected JcrConnector jcr;

	@AroundInvoke
	public Object filter(InvocationContext invocationContext) throws Exception {
		String principalName = sessionContext.getCallerPrincipal().getName();
		// TODO Falls dieses Interceptor-Ding jemals für irgendwas produktiv genutzt wird, MUSS das Erfragen einer
		// JCR-Session anders gestaltet werden
		Session s = jcr.getJCRSession(WasabiConstants.JCR_USER_INDEPENDENT_SESSION);
		Object object = invocationContext.proceed();

		// if user root, admin or user is a member of group admins access granted
		if (principalName.equals("root")
				|| principalName.equals("admin")
				|| GroupServiceImpl.isMember(GroupServiceImpl.getGroupByName("admins"), UserServiceImpl.getUserByName(
						principalName, s))) {

			return object;
		}

		return null;
	}

}
