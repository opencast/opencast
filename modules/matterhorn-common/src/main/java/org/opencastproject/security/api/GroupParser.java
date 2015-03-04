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

import org.opencastproject.util.jaxb.JaxbParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** JAXB parser for JAXB DTOs of {@link Group}. */
public final class GroupParser extends JaxbParser {

  /** Instance of GroupParser */
  public static final GroupParser I = new GroupParser("org.opencastproject.security.api");

  private GroupParser(String contextPath) {
    super(contextPath);
  }

  public JaxbGroup parseGroupFromXml(String xml) throws IOException {
    return unmarshal(JaxbGroup.class, new ByteArrayInputStream(xml.getBytes()));
  }

  public JaxbGroupList parseGroupListFromXml(String xml) throws IOException {
    return unmarshal(JaxbGroupList.class, new ByteArrayInputStream(xml.getBytes()));
  }

  public String toXml(JaxbGroup group) throws IOException {
    return marshal(group);
  }

  public String toXml(JaxbGroupList groupList) throws IOException {
    return marshal(groupList);
  }

}
