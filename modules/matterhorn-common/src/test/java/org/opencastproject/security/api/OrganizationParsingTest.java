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

import org.junit.Test;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;

/**
 * Tests the JAXB java to xml conversion of the organization class.
 */
public class OrganizationParsingTest {
  @Test
  public void testOrgParsing() throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance(Organization.class);
    StringWriter writer = new StringWriter();
    Organization org = new DefaultOrganization();
    jaxbContext.createMarshaller().marshal(org, writer);

    String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<ns2:organization id=\"mh_default_org\" xmlns:ns2=\"http://org.opencastproject.security\">"
            + "<name>Opencast Project</name><serverName>http://localhost:8080</serverName><serverPort>80</serverPort>"
            + "<adminRole>ROLE_ADMIN</adminRole><anonymousRole>ANONYMOUS</anonymousRole><properties>"
            + "<property key=\"logo_small\">/img/mh_logos/OpencastLogo.png</property>"
            + "<property key=\"logo_large\">/img/mh_logos/MatterhornLogo_large.png</property>"
            + "</properties></ns2:organization>";

    assertEquals("Organization XML not formed as expected", expectedOutput, writer.toString());
  }
}
