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

import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;

public class EventSubscriptions {

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Destination>> subscriptions = new ConcurrentHashMap<String, ConcurrentHashMap<String, Destination>>();

	public static Set<Entry<String, Destination>> getSubscribers(String objectId) {
		ConcurrentHashMap<String, Destination> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			return subscriptionsOfObject.entrySet();
		}
		return null;
	}

	public static synchronized void subscribe(String objectId, String username, Destination jmsDestination) {
		ConcurrentHashMap<String, Destination> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject == null) {
			subscriptionsOfObject = new ConcurrentHashMap<String, Destination>();
			subscriptions.put(objectId, subscriptionsOfObject);
		}
		subscriptionsOfObject.put(username, jmsDestination);
	}

	public static void unsubscribe(String objectId, String username) {
		ConcurrentHashMap<String, Destination> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			subscriptionsOfObject.remove(username);
		}
	}
}
