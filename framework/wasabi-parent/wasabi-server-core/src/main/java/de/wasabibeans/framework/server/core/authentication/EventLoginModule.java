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

import java.security.acl.Group;

import javax.security.auth.login.LoginException;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

import de.wasabibeans.framework.server.core.common.WasabiConstants;

public class EventLoginModule extends SqlLoginModule {

	@Override
	protected Group[] getRoleSets() throws LoginException {
		Group[] roleSets = { new SimpleGroup("Roles") };
		if (WasabiConstants.JMS_EVENT_ADMIN.equals(getUsername())) {
			// in case of JMS_EVENT_ADMIN add the corresponding role that allows to interact with the server internal
			// jms queues as admin (e.g. being allowed to read messages from the registrar queue)
			roleSets[0].addMember(new SimplePrincipal(WasabiConstants.JMS_EVENT_ADMIN));
		} else if ("guest".equals(getUsername())) {
			roleSets[0].addMember(new SimplePrincipal("guest"));
		} else {
			// in case of normal user add the corresponding role that restricts the interaction with the server internal
			// jms queues (e.g. not being allowed to read messages from the registrar queue)
			roleSets[0].addMember(new SimplePrincipal(WasabiConstants.JMS_EVENT_USER));
		}
		return roleSets;
	}
}
