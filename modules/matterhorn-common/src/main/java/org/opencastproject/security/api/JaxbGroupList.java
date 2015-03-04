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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for group collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "groups", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "groups", namespace = "http://org.opencastproject.security")
public class JaxbGroupList {

  /** A list of groups. */
  @XmlElement(name = "group")
  protected List<JaxbGroup> groups = new ArrayList<JaxbGroup>();

  public JaxbGroupList() {
  }

  public JaxbGroupList(JaxbGroup group) {
    groups.add(group);
  }

  public JaxbGroupList(Collection<JaxbGroup> groups) {
    for (JaxbGroup group : groups)
      groups.add(group);
  }

  /**
   * @return the groups
   */
  public List<JaxbGroup> getGroups() {
    return groups;
  }

  /**
   * @param roles
   *          the roles to set
   */
  public void setRoles(List<JaxbGroup> roles) {
    this.groups = roles;
  }

  public void add(Group group) {
    if (group instanceof JaxbGroup) {
      groups.add((JaxbGroup) group);
    } else {
      groups.add(JaxbGroup.fromGroup(group));
    }
  }

}
