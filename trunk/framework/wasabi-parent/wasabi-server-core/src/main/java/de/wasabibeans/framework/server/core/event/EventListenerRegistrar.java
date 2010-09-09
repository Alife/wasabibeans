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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jmx.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = WasabiConstants.JMS_QUEUE_REGISTRAR) })
public class EventListenerRegistrar implements MessageListener {

	private static WasabiLogger logger = WasabiLogger.getLogger(EventListenerRegistrar.class);

	@Override
	public void onMessage(Message message) {
		try {
			String objectId = message.getStringProperty(WasabiEventRegistration.WASABI_OBJECT_ID);
			String username = message.getStringProperty(WasabiEventRegistration.USERNAME);
			if (objectId != null && username != null) {
				EventSubscriptions.subscribe(objectId, username, message);
			}
		} catch (JMSException e) {
			logger.warn("Could not register an event listener");
		}
	}
}
