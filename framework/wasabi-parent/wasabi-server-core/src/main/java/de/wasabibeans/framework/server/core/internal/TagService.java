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

package de.wasabibeans.framework.server.core.internal;

import java.util.Map;
import java.util.Vector;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.local.TagServiceLocal;
import de.wasabibeans.framework.server.core.remote.TagServiceRemote;

/**
 * Class, that implements the internal access on Tags of WasabiObject objects.
 */
@SecurityDomain("wasabi")
@Stateless(name = "TagService")
public class TagService extends ObjectService implements TagServiceLocal, TagServiceRemote {

	@Override
	public void addTag(WasabiObjectDTO object, String tag) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearTags(WasabiObjectDTO object) {
		// TODO Auto-generated method stub

	}

	@Override
	public Vector<WasabiDocumentDTO> getDocumentsByTags(WasabiLocationDTO environment, Vector<String> tags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getMostUsedDocumentTags(WasabiLocationDTO environment, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<WasabiObjectDTO> getObjectsByTag(String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<String> getTags(WasabiObjectDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTag(WasabiObjectDTO object, String tag) {
		// TODO Auto-generated method stub

	}

}