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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import de.wasabibeans.framework.server.core.common.WasabiConstants;

public class EventSubscriptions {

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, SubscriptionInfo>> subscriptions = new ConcurrentHashMap<String, ConcurrentHashMap<String, SubscriptionInfo>>();

	public static Set<Entry<String, SubscriptionInfo>> getSubscribers(String objectId) {
		ConcurrentHashMap<String, SubscriptionInfo> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			return subscriptionsOfObject.entrySet();
		}
		return null;
	}

	public static synchronized void subscribe(String objectId, String username, String jmsDestinationName,
			boolean isQueue) {
		ConcurrentHashMap<String, SubscriptionInfo> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject == null) {
			subscriptionsOfObject = new ConcurrentHashMap<String, SubscriptionInfo>();
			subscriptions.put(objectId, subscriptionsOfObject);
		}
		subscriptionsOfObject.put(username, new SubscriptionInfo(jmsDestinationName, isQueue));
	}

	public static void unsubscribe(String objectId, String username) {
		ConcurrentHashMap<String, SubscriptionInfo> subscriptionsOfObject = subscriptions.get(objectId);
		if (subscriptionsOfObject != null) {
			subscriptionsOfObject.remove(username);
		}
	}

	/**
	 * Inner class encapsulating additional information of a subscription: the name of the temporary destination used by
	 * the client to receive events, whether the temporary destination is a queue, and a timestamp for caching read
	 * permissions
	 * 
	 */
	public static class SubscriptionInfo implements Serializable {

		private static final long serialVersionUID = -1002710476315617258L;
		
		private String jmsDestinationName;
		private boolean isQueue;
		private Calendar timestamp;

		public SubscriptionInfo(String jmsDestinationName, boolean isQueue) {
			this.jmsDestinationName = jmsDestinationName;
			this.isQueue = isQueue;
			this.timestamp = Calendar.getInstance();
		}

		public String getJmsDestinationName() {
			return jmsDestinationName;
		}

		public boolean isQueue() {
			return isQueue;
		}

		public boolean isTimestampStillValid(Calendar actualTime) {
			long actualTimeInMillis = actualTime.getTimeInMillis();
			actualTime.add(Calendar.MINUTE, -WasabiConstants.JMS_PERMISSION_CACHE_TIME);
			if (actualTime.after(timestamp)) {
				timestamp.setTimeInMillis(actualTimeInMillis);
				return false;
			}
			return true;
		}
	}
}
