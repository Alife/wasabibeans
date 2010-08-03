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
package de.wasabibeans.framework.server.core.test.testhelper;

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Remote
public interface TestHelperRemote {
	
	public void initDatabase();
	
	public WasabiRoomDTO initRepository() throws Exception;
	public void shutdownRepository() throws UnexpectedInternalProblemException;
	
	public WasabiRoomDTO initRoomServiceTest() throws Exception;
	public void initTestUser() throws Exception;
	
	public Vector<String> createManyNodes(int number) throws Exception;
	public Vector<String> getManyNodesByIdLookup(Vector<String> nodeIds) throws Exception;
	public Vector<String> getManyNodesByIdFilter(Vector<String> nodeIds) throws Exception;
}
