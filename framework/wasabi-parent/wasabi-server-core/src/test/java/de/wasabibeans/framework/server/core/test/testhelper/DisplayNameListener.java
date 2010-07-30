package de.wasabibeans.framework.server.core.test.testhelper;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

public class DisplayNameListener implements EventListener {

	@Override
	public void onEvent(EventIterator events) {
		try {
			while (events.hasNext()) {
				Event event = events.nextEvent();
				if (event.getType() == Event.PROPERTY_CHANGED) {
					System.out.println("EVENT!!! " + event.getPath().toString() + " has been changed");
				}
			}
		} catch (RepositoryException re) {
			System.out.println("Problem mit dem SCHEISS EVENT!");
		}
	}

}
