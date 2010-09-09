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

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Message;

public class EventSubscriptions {

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Message>> subscriptions = new ConcurrentHashMap<String, ConcurrentHashMap<String, Message>>();

	public static Collection<Message> getSubscribers(String objectId) {
		ConcurrentHashMap<String, Message> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			return subscriptionsOfObject.values();
		} else {
			return new Vector<Message>();
		}
	}

	public static void subscribe(String objectId, String username, Message message) {
		ConcurrentHashMap<String, Message> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject == null) {
			subscriptionsOfObject = new ConcurrentHashMap<String, Message>();
			subscriptions.put(objectId, subscriptionsOfObject);
		}
		subscriptionsOfObject.put(username, message);
	}

	public static void unsubscribe(String objectId, String username) {
		ConcurrentHashMap<String, Message> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			subscriptionsOfObject.remove(username);
		}
	}
}
