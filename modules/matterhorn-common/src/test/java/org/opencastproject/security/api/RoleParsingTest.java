/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.security.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

/**
 * Tests JAXB un/marshalling of the role
 */
public class RoleParsingTest {

  private static final DefaultOrganization ORGANIZATION = new DefaultOrganization();

  private static final String ROLE_XML_FILE = "/role.xml";

  private JAXBContext jaxbContext;

  @Before
  public void setUp() throws Exception {
    jaxbContext = JAXBContext.newInstance(JaxbRole.class);
  }

  @Test
  public void testMarshalUser() throws Exception {
    StringWriter writer = new StringWriter();

    JaxbRole role = new JaxbRole("ROLE_TEST", ORGANIZATION, "This is a test role");
    jaxbContext.createMarshaller().marshal(role, writer);

    String expectedOutput = IOUtils.toString(getClass().getResourceAsStream(ROLE_XML_FILE), "UTF-8");

    assertTrue("Role XML not formed as expected", XMLUnit.compareXML(expectedOutput, writer.toString()).identical());
  }

  @Test
  public void testUnmarshalUser() throws Exception {
    JaxbRole expectedRole = new JaxbRole("ROLE_TEST", ORGANIZATION, "This is a test role");

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(ROLE_XML_FILE));
    JaxbRole role = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbRole.class).getValue();

    assertEquals(expectedRole.getName(), role.getName());
    assertEquals(expectedRole.getDescription(), role.getDescription());
    assertEquals(expectedRole.getOrganization(), role.getOrganization());
  }

}
