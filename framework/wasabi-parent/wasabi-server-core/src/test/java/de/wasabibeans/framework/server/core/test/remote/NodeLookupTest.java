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
package de.wasabibeans.framework.server.core.test.remote;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Run(RunModeType.AS_CLIENT)
public class NodeLookupTest extends WasabiRemoteTest {
	
	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize jcr repository
		testhelper.initRepository();
	}
	
	@Test
	public void idLookup() throws Exception {
		Vector<String> nodeIds = testhelper.createManyNodes(10000);
		Vector<String> result = testhelper.getManyNodesByIdLookup(nodeIds);
		System.out.println(result.size());
		System.out.println(result.lastElement());
	
	}
	
	@Test
	public void idFilter() throws Exception {
		Vector<String> nodeIds = testhelper.createManyNodes(10000);
		Vector<String> result = testhelper.getManyNodesByIdFilter(nodeIds);
		System.out.println(result.size());
		System.out.println(result.lastElement());
	}

}
