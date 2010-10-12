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

package de.wasabibeans.framework.server.core.event;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.Service;

import de.wasabibeans.framework.server.core.authorization.WasabiAuthorizer;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.event.EventSubscriptions.SubscriptionInfo;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.util.JcrConnector;
import de.wasabibeans.framework.server.core.util.JndiConnector;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

@Service
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class EventAuthorizationChecker implements EventAuthorizationCheckerLocal {

	private static WasabiLogger logger = WasabiLogger.getLogger(EventAuthorizationChecker.class);

	@Resource
	private SessionContext ctx;

	@EJB
	private EventSubscriptionsLocal eventSubscriptions;

	public void startEventAuthorizationChecker() {
		ctx.getTimerService().createTimer(WasabiConstants.JMS_PERMISSION_CACHE_TIME * 60 * 1000,
				WasabiConstants.JMS_PERMISSION_CACHE_TIME * 60 * 1000, null);
		logger.info("EventAuthorizationChecker started. Used interval is: " + WasabiConstants.JMS_PERMISSION_CACHE_TIME
				+ " minutes");
	}

	@Timeout
	public void check(Timer timer) {
		JndiConnector jndi = JndiConnector.getJNDIConnector();
		JcrConnector jcr = JcrConnector.getJCRConnector(jndi);
		try {
			Session s = jcr.getJCRSession();
			logger.info("Checking event listener permissions...");

			for (Entry<String, ConcurrentHashMap<String, SubscriptionInfo>> objectSubcriptions : eventSubscriptions
					.getData().entrySet()) {
				String objectId = objectSubcriptions.getKey();
				try {
					Node objectNode = ObjectServiceImpl.get(objectId, s);
					for (String username : objectSubcriptions.getValue().keySet()) {
						if (!checkPermission(objectNode, username, s)) {
							eventSubscriptions.unsubscribe(objectId, username);
						}
					}
				} catch (ObjectDoesNotExistException odnee) {
					// the object has been removed in the meantime -> remove subscriptions
					eventSubscriptions.removeSubscriptions(objectId);
				}
			}

			logger.info("Checking event listener permissions... done.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("Checking permissions of users listening to events failed.", e);
		} finally {
			jcr.cleanup(true);
			jndi.close();
		}
	}

	private boolean checkPermission(Node objectNode, String username, Session s)
			throws UnexpectedInternalProblemException {
		if (WasabiConstants.ACL_CHECK_ENABLE) {
			if (!WasabiAuthorizer.isAdminUser(username, s)) {
				if (!WasabiAuthorizer.authorize(objectNode, username, WasabiPermission.READ, s)) {
					return false;
				}
			}
		}
		return true;
	}
}
