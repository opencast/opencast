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
package org.opencastproject.userdirectory;

import static org.junit.Assert.assertEquals;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.XMLAssert;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

/**
 * Tests JAXB un/marshalling of the group's
 */
public class GroupParsingTest {

  private static final DefaultOrganization ORGANIZATION = new DefaultOrganization();

  private static final String GROUP_XML_FILE = "/group.xml";

  private JAXBContext jaxbContext;

  @Before
  public void setUp() throws Exception {
    jaxbContext = JAXBContext.newInstance(JaxbGroup.class);
  }

  @Test
  public void testMarshalUser() throws Exception {
    StringWriter writer  = new StringWriter();
    StringWriter writer2 = new StringWriter();

    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole("ROLE_COURSE_ADMIN", ORGANIZATION));
    roles.add(new JaxbRole("ROLE_USER", ORGANIZATION));

    Set<String> members = new HashSet<String>();
    members.add("admin1");
    members.add("admin2");

    JaxbGroup group = new JaxbGroup("admin", ORGANIZATION, "Admin", "Admin group", roles, members);
    jaxbContext.createMarshaller().marshal(group, writer);

    String expectedOutput = IOUtils.toString(getClass().getResourceAsStream(GROUP_XML_FILE), "UTF-8");
    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(GROUP_XML_FILE));
    JaxbGroup groupFromFile = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbGroup.class).getValue();
    jaxbContext.createMarshaller().marshal(groupFromFile, writer2);

    Diff diff = new Diff(writer2.toString(), writer.toString());
    /* We don't care about ordering. */
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  public void testUnmarshalUser() throws Exception {
    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole("ROLE_COURSE_ADMIN", ORGANIZATION));
    roles.add(new JaxbRole("ROLE_USER", ORGANIZATION));

    Set<String> members = new HashSet<String>();
    members.add("admin1");
    members.add("admin2");

    JaxbGroup expectedGroup = new JaxbGroup("admin", ORGANIZATION, "Admin", "Admin group", roles, members);

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(GROUP_XML_FILE));
    JaxbGroup group = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbGroup.class).getValue();

    assertEquals(expectedGroup.getGroupId(), group.getGroupId());
    assertEquals(expectedGroup.getName(), group.getName());
    assertEquals(expectedGroup.getDescription(), group.getDescription());
    assertEquals(expectedGroup.getRole(), group.getRole());
    assertEquals(expectedGroup.getOrganization(), group.getOrganization());
    assertEquals(expectedGroup.getRoles(), group.getRoles());
    assertEquals(expectedGroup.getMembers(), group.getMembers());
  }

}
