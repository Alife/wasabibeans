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

import java.util.Date;
import java.util.Vector;

import javax.ejb.EJBException;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.dto.WasabiAttributeDTO;
import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiValueDTO;
import de.wasabibeans.framework.server.core.exception.AttributeValueException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.test.testhelper.TestHelperRemote;

@Run(RunModeType.AS_CLIENT)
public class AttributeServiceRemoteTest extends WasabiRemoteTest {

	private WasabiAttributeDTO attribute1;

	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize test
		TestHelperRemote testhelper = (TestHelperRemote) reWaCon.lookup("TestHelper");
		testhelper.initDatabase();
		rootRoom = testhelper.initRepository();
		testhelper.initTestUser();
		attribute1 = testhelper.initAttributeServiceTest();

		reWaCon.login("user", "user");
	}

	@AfterMethod
	public void tearDownAfterEachMethod() throws Exception {
		reWaCon.logout();
	}

	@Test
	public void get1AttributeByNameTest() throws Exception {
		WasabiAttributeDTO test = attributeService().getAttributeByName(rootRoom, "attribute1");
		AssertJUnit.assertEquals(attribute1, test);

		try {
			attributeService().getAttributeByName(rootRoom, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		AssertJUnit.assertNull(attributeService().getAttributeByName(rootRoom, "doesNotExist"));
	}

	@Test
	public void get1AttributesTest() throws Exception {
		Vector<WasabiAttributeDTO> attributes = attributeService().getAttributes(rootRoom);
		AssertJUnit.assertTrue(attributes.contains(attribute1));
		AssertJUnit.assertEquals(2, attributes.size());
	}

	@Test
	public void get1AffiliationTest() throws Exception {
		WasabiObjectDTO affiliation = attributeService().getAffiliation(attribute1).getValue();
		AssertJUnit.assertEquals(rootRoom, affiliation);
	}

	@Test
	public void get1AttributeTypeTest() throws Exception {
		String attributeType = attributeService().getAttributeType(attribute1);
		AssertJUnit.assertEquals(String.class.getName(), attributeType);
	}

	@Test
	public void get1ValueTest() throws Exception {
		WasabiValueDTO test = attributeService().getValue(String.class, attribute1);
		AssertJUnit.assertEquals("attribute1", test.getValue());

		try {
			attributeService().getValue(null, attribute1);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			attributeService().getValue(Integer.class, attribute1);
			AssertJUnit.fail();
		} catch (AttributeValueException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { ".*get1.*" })
	public void getWasabiValueTest() throws Exception {
		WasabiAttributeDTO attribute2 = attributeService().getAttributeByName(rootRoom, "attribute2");
		WasabiValueDTO test = attributeService().getWasabiValue(attribute2);
		AssertJUnit.assertEquals(rootRoom, test.getValue());

		try {
			attributeService().getWasabiValue(attribute1);
			AssertJUnit.fail();
		} catch (AttributeValueException e) {
			// passed
		}
	}

	@Test(dependsOnMethods = { ".*get.*" })
	public void createTest() throws Exception {
		// create new attribute with primitive value
		WasabiAttributeDTO attribute3 = attributeService().create("attribute3", 3, rootRoom);
		AssertJUnit.assertNotNull(attribute3);
		AssertJUnit.assertEquals(attribute3, attributeService().getAttributeByName(rootRoom, "attribute3"));
		AssertJUnit.assertEquals(3, attributeService().getValue(Integer.class, attribute3).getValue());
		AssertJUnit.assertEquals(Integer.class.getName(), attributeService().getAttributeType(attribute3));

		// create new attribute with wasabi value
		WasabiAttributeDTO attribute4 = attributeService().create("attribute4", attribute3, rootRoom);
		AssertJUnit.assertNotNull(attribute4);
		AssertJUnit.assertEquals(attribute4, attributeService().getAttributeByName(rootRoom, "attribute4"));
		AssertJUnit.assertEquals(attribute3, attributeService().getWasabiValue(attribute4).getValue());
		AssertJUnit.assertEquals(WasabiNodeType.ATTRIBUTE, attributeService().getAttributeType(attribute4));

		try {
			attributeService().create(null, 3, rootRoom);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			attributeService().create("test", 3, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		try {
			attributeService().create("attribute3", 3, rootRoom);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			// passed
		}

		WasabiAttributeDTO attribute5 = attributeService().create("attribute5", (String) null, rootRoom);
		AssertJUnit.assertNull(attributeService().getValue(String.class, attribute5).getValue());
		AssertJUnit.assertNull(attributeService().getWasabiValue(attribute5).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void moveTest() throws Exception {
		WasabiDocumentDTO newAffiliation = documentService().create("newAffiliation", rootRoom);
		attributeService().create("attribute1", 1, newAffiliation);

		try {
			attributeService().move(attribute1, newAffiliation, null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			Vector<WasabiAttributeDTO> attributesOfRoot = attributeService().getAttributes(rootRoom);
			AssertJUnit.assertTrue(attributesOfRoot.contains(attribute1));
			AssertJUnit.assertEquals(2, attributesOfRoot.size());
			AssertJUnit.assertEquals(1, attributeService().getAttributes(newAffiliation).size());
		}

		WasabiAttributeDTO attribute2 = attributeService().getAttributeByName(rootRoom, "attribute2");
		attributeService().move(attribute2, newAffiliation, null);
		Vector<WasabiAttributeDTO> attributesOfRoot = attributeService().getAttributes(rootRoom);
		AssertJUnit.assertFalse(attributesOfRoot.contains(attribute2));
		AssertJUnit.assertEquals(1, attributesOfRoot.size());
		Vector<WasabiAttributeDTO> attributesOfNewAffiliation = attributeService().getAttributes(newAffiliation);
		AssertJUnit.assertTrue(attributesOfNewAffiliation.contains(attribute2));
		AssertJUnit.assertEquals(2, attributesOfNewAffiliation.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void removeTest() throws Exception {
		attributeService().remove(attribute1);
		Vector<WasabiAttributeDTO> attributes = attributeService().getAttributes(rootRoom);
		AssertJUnit.assertFalse(attributes.contains(attribute1));
		AssertJUnit.assertEquals(1, attributes.size());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void renameTest() throws Exception {
		try {
			attributeService().rename(attribute1, "attribute2", null);
			AssertJUnit.fail();
		} catch (ObjectAlreadyExistsException e) {
			AssertJUnit.assertNotNull(attributeService().getAttributeByName(rootRoom, "attribute1"));
			AssertJUnit.assertEquals(2, attributeService().getAttributes(rootRoom).size());
		}

		try {
			attributeService().rename(attribute1, null, null);
			AssertJUnit.fail();
		} catch (EJBException e) {
			if (!(e.getCause() instanceof IllegalArgumentException)) {
				AssertJUnit.fail();
			}
		}

		attributeService().rename(attribute1, "attribute_2", null);
		AssertJUnit.assertEquals("attribute_2", attributeService().getName(attribute1).getValue());
		AssertJUnit.assertNotNull(attributeService().getAttributeByName(rootRoom, "attribute_2"));
		AssertJUnit.assertEquals(2, attributeService().getAttributes(rootRoom).size());
		AssertJUnit.assertNull(attributeService().getAttributeByName(rootRoom, "attribute1"));
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setValueTest() throws Exception {
		// test a primitive value
		attributeService().setValue(attribute1, true, null);
		AssertJUnit.assertEquals(Boolean.class.getName(), attributeService().getAttributeType(attribute1));

		// test a date value
		Date date = new Date();
		attributeService().setValue(attribute1, date, null);
		AssertJUnit.assertEquals(Date.class.getName(), attributeService().getAttributeType(attribute1));
		AssertJUnit.assertEquals(date, attributeService().getValue(Date.class, attribute1).getValue());

		// test a serializable object value
		Vector<String> vector = new Vector<String>();
		vector.add("Look, behind you");
		vector.add("a three-headed monkey");
		attributeService().setValue(attribute1, vector, null);
		AssertJUnit.assertEquals(Vector.class.getName(), attributeService().getAttributeType(attribute1));
		AssertJUnit.assertEquals(vector, attributeService().getValue(Vector.class, attribute1).getValue());

		// test null value
		attributeService().setValue(attribute1, null, null);
		AssertJUnit.assertNull(attributeService().getAttributeType(attribute1));
		AssertJUnit.assertNull(attributeService().getValue(String.class, attribute1).getValue());

		// test whether a new value can be set after null
		attributeService().setValue(attribute1, 3, null);
		AssertJUnit.assertEquals(Integer.class.getName(), attributeService().getAttributeType(attribute1));
		AssertJUnit.assertEquals(3, attributeService().getValue(Integer.class, attribute1).getValue());
	}

	@Test(dependsOnMethods = { "createTest" })
	public void setWasabiValueTest() throws Exception {
		WasabiDocumentDTO document = documentService().create("document", rootRoom);
		attributeService().setWasabiValue(attribute1, document, null);
		AssertJUnit.assertEquals(WasabiNodeType.DOCUMENT, attributeService().getAttributeType(attribute1));
		AssertJUnit.assertEquals(document, attributeService().getWasabiValue(attribute1).getValue());

		// test null value
		attributeService().setWasabiValue(attribute1, null, null);
		AssertJUnit.assertNull(attributeService().getAttributeType(attribute1));
		AssertJUnit.assertNull(attributeService().getWasabiValue(attribute1).getValue());

		// test whether a new value can be set after null
		attributeService().setWasabiValue(attribute1, rootRoom, null);
		AssertJUnit.assertEquals(WasabiNodeType.ROOM, attributeService().getAttributeType(attribute1));
		AssertJUnit.assertEquals(rootRoom, attributeService().getWasabiValue(attribute1).getValue());
	}
}
