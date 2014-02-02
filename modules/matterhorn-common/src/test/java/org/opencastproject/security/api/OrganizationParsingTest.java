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
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
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

  private boolean compareXMLattsEqual(String in1, String in2) {
    if (in1.equals(in2)) {
      return  true;
    }
    String[] strings1 = StringUtils.stripEnd(in1, " /").split(" ");
    String[] strings2 = StringUtils.stripEnd(in2, " /").split(" ");
    Arrays.sort(strings1);
    Arrays.sort(strings2);
    assertEquals("Organization XML not formed as expected - error in number of XML attributes", strings1.length, strings2.length);
    for (int i = 0; i < strings1.length; i++) {
      if (!(strings1[i].equals(strings2[i]))) {
        assertEquals("Organization XML not formed as expected - xml-attributes don't match", "<" + in1 + ">","<" + in2 + ">");
      }
    }
    return true;
  }

  private boolean compareXMLTagsEqual(String in1, String in2) {
    if (in1.equals(in2)) {
      return true;
    }
    String[] strings1 = in1.split("><");
    String[] strings2 = in2.split("><");

    Arrays.sort(strings1);
    Arrays.sort(strings2);
    assertEquals("Organization XML not formed as expected - error in number of XML tags", strings1.length, strings2.length);
    for (int i = 0; i < strings1.length; i++) {
      if (!compareXMLattsEqual(strings1[i],strings2[i])) {
        return false;
      }
    }
    return true;
  }

  private void compareOrgs(Organization org, Organization organization) {
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

  @Test
  public void testMarshalOrganization() throws Exception {
    StringWriter writer = new StringWriter();
    Organization org = new DefaultOrganization();
    jaxbContext.createMarshaller().marshal(org, writer);

    String expectedOutput = IOUtils.toString(getClass().getResourceAsStream(ORG_XML_FILE), "UTF-8");
    String producedOutput = writer.toString();

    boolean val = compareXMLTagsEqual(expectedOutput, producedOutput);
    assertTrue("Organization XML not formed as expected",val);

    StreamSource streamSource = new StreamSource(new StringReader(producedOutput));
    JaxbOrganization organization = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbOrganization.class)
            .getValue();
    compareOrgs(org, organization);
  }

  @Test
  public void testUnmarshalOrganization() throws Exception {
    Organization org = new DefaultOrganization();

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(ORG_XML_FILE));
    JaxbOrganization organization = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbOrganization.class)
            .getValue();
    compareOrgs(org, organization);
  }

}
