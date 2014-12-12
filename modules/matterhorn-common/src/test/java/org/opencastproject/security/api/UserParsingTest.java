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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests JAXB un/marshalling of the user
 */
public class UserParsingTest {

  private static final DefaultOrganization ORGANIZATION = new DefaultOrganization();

  private static final String USER_XML_FILE = "/user.xml";

  private JAXBContext jaxbContext;

  @Before
  public void setUp() throws Exception {
    jaxbContext = JAXBContext.newInstance(JaxbUser.class);
  }

  @Test
  public void testMarshalUser() throws Exception {
    StringWriter writer  = new StringWriter();

    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole("ROLE_COURSE_ADMIN", ORGANIZATION));
    roles.add(new JaxbRole("ROLE_USER", ORGANIZATION));
    roles.add(new JaxbRole("SERIES_1_ADMIN", ORGANIZATION));

    JaxbUser user = new JaxbUser("admin", "123456", ORGANIZATION, roles);
    jaxbContext.createMarshaller().marshal(user, writer);

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(USER_XML_FILE));
    JaxbUser userFromFile = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbUser.class).getValue();

    streamSource = new StreamSource(new ByteArrayInputStream(writer.toString().getBytes()));
    JaxbUser userFromXML = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbUser.class).getValue();

    assertEquals(userFromXML, userFromFile);
  }

  @Test
  public void testUnmarshalUser() throws Exception {
    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole("ROLE_USER", ORGANIZATION));
    roles.add(new JaxbRole("SERIES_1_ADMIN", ORGANIZATION));
    roles.add(new JaxbRole("ROLE_COURSE_ADMIN", ORGANIZATION));

    JaxbUser expectedUser = new JaxbUser("admin", "123456", ORGANIZATION, roles);

    StreamSource streamSource = new StreamSource(getClass().getResourceAsStream(USER_XML_FILE));
    JaxbUser user = jaxbContext.createUnmarshaller().unmarshal(streamSource, JaxbUser.class).getValue();

    assertEquals(expectedUser.getUsername(), user.getUsername());
    assertNull(user.getPassword());
    assertEquals(expectedUser.getOrganization(), user.getOrganization());
    assertEquals(expectedUser.getRoles(), user.getRoles());
  }

}
