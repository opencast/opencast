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
 * A wrapper for role collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "roles", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "roles", namespace = "http://org.opencastproject.security")
public class JaxbRoleList {

  /** A list of roles. */
  @XmlElement(name = "role")
  protected List<JaxbRole> roles = new ArrayList<JaxbRole>();

  public JaxbRoleList() {
  }

  public JaxbRoleList(JaxbRole role) {
    roles.add(role);
  }

  public JaxbRoleList(Collection<JaxbRole> roles) {
    for (JaxbRole role : roles)
      this.roles.add(role);
  }

  /**
   * @return the roles
   */
  public List<JaxbRole> getRoles() {
    return roles;
  }

  /**
   * @param roles
   *          the roles to set
   */
  public void setRoles(List<JaxbRole> roles) {
    this.roles = roles;
  }

  public void add(Role role) {
    if (role instanceof JaxbRole) {
      roles.add((JaxbRole) role);
    } else {
      roles.add(JaxbRole.fromRole(role));
    }
  }

}
