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

package de.wasabibeans.framework.server.core.bean;

import java.io.Serializable;
import java.util.Vector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.local.AttributeServiceLocal;
import de.wasabibeans.framework.server.core.remote.AttributeServiceRemote;

/**
 * Class, that implements the internal access on WasabiAttribute objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "AttributeService")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AttributeService extends ObjectService implements AttributeServiceLocal, AttributeServiceRemote {

	@Override
	public WasabiAttributeDTO create(String name, Serializable value, WasabiObjectDTO affiliation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiAttributeDTO create(String name, WasabiObjectDTO value, WasabiObjectDTO affiliation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiObjectDTO getAffiliation(WasabiAttributeDTO attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiAttributeDTO getAttributeByName(WasabiObjectDTO object, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAttributeType(WasabiAttributeDTO attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiAttributeDTO> getAttributes(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Serializable> T getValue(Class<T> type, WasabiAttributeDTO attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WasabiObjectDTO getWasabiValue(WasabiAttributeDTO attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(WasabiAttributeDTO attribute, WasabiObjectDTO newAffiliation) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WasabiAttributeDTO attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rename(WasabiAttributeDTO attribute, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setValue(WasabiAttributeDTO attribute, Serializable value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWasabiValue(WasabiAttributeDTO attribute, WasabiObjectDTO value) {
		// TODO Auto-generated method stub

	}

}
