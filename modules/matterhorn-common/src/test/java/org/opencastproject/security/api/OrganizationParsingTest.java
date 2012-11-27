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

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

/**
 * Tests the JAXB java to xml conversion of the organization class.
 */
public class OrganizationParsingTest {

  private static final String ORG_XML_FILE = "/organization.xml";

  private JAXBContext jaxbContext;

  @Before
  public void setUp() throws Exception {
    jaxbContext = JAXBContext.newInstance(JaxbOrganization.class);
  }

  @Test
  public void testMarshalOrganization() throws Exception {
    StringWriter writer = new StringWriter();
    Organization org = new DefaultOrganization();
    jaxbContext.createMarshaller().marshal(org, writer);

    String expectedOutput = IOUtils.toString(getClass().getResourceAsStream(ORG_XML_FILE), "UTF-8");

    assertEquals("Organization XML not formed as expected", expectedOutput, writer.toString());
  }

  @Test
  public void testUnmarshalOrganization() throws Exception {
    Organization org = new DefaultOrganization();

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(ORG_XML_FILE));
    JaxbOrganization organization = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbOrganization.class)
            .getValue();

    assertEquals(org.getId(), organization.getId());
    assertEquals(org.getName(), organization.getName());
    assertEquals(org.getAdminRole(), organization.getAdminRole());
    assertEquals(org.getAnonymousRole(), organization.getAnonymousRole());
    assertEquals(org.getServers().size(), organization.getServers().size());
    for (Map.Entry<String, Integer> server : org.getServers().entrySet()) {
      boolean found = false;
      for (Map.Entry<String, Integer> s : organization.getServers().entrySet()) {
        if (server.getKey().equals(s.getKey()) && server.getValue().equals(s.getValue())) {
          found = true;
          break;
        }
      }
     assertTrue(found);
    }
    assertEquals(org.getProperties(), organization.getProperties());
  }

}
