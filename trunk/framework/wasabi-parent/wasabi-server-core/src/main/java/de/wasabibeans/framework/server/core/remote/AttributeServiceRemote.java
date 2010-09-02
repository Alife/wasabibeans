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

package de.wasabibeans.framework.server.core.remote;

import java.io.Serializable;
import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.AttributeValueException;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.TargetDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

/**
 * Interface, that defines the remote access on WasabiAttributeDTO objects.
 */
@Remote
public interface AttributeServiceRemote extends ObjectServiceRemote {

	public WasabiAttributeDTO create(String name, Serializable value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			AttributeValueException;

	public WasabiAttributeDTO create(String name, WasabiObjectDTO value, WasabiObjectDTO affiliation)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, ObjectAlreadyExistsException,
			AttributeValueException;

	public WasabiValueDTO getAffiliation(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public WasabiAttributeDTO getAttributeByName(WasabiObjectDTO object, String name)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException;

	public Vector<WasabiAttributeDTO> getAttributes(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public String getAttributeType(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public <T extends Serializable> WasabiValueDTO getValue(Class<T> type, WasabiAttributeDTO attribute)
			throws UnexpectedInternalProblemException, ObjectDoesNotExistException, AttributeValueException;

	public WasabiValueDTO getWasabiValue(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			TargetDoesNotExistException, AttributeValueException, ObjectDoesNotExistException;

	public void move(WasabiAttributeDTO attribute, WasabiObjectDTO newAffiliation, Long optLockId)
			throws UnexpectedInternalProblemException, ObjectAlreadyExistsException, ConcurrentModificationException,
			ObjectDoesNotExistException;

	public void remove(WasabiAttributeDTO attribute) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void rename(WasabiAttributeDTO attribute, String name, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, ObjectAlreadyExistsException,
			ObjectDoesNotExistException;

	public void setValue(WasabiAttributeDTO attribute, Serializable value, Long optLockId)
			throws UnexpectedInternalProblemException, ConcurrentModificationException, AttributeValueException,
			ObjectDoesNotExistException;

	public void setWasabiValue(WasabiAttributeDTO attribute, WasabiObjectDTO value, Long optLockId)
			throws ObjectDoesNotExistException, UnexpectedInternalProblemException, ConcurrentModificationException;

}
