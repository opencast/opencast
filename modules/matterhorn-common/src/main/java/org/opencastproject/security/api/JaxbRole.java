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

import org.opencastproject.util.EqualsUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "role", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "role", namespace = "http://org.opencastproject.security")
public final class JaxbRole implements Role {

  /** The role name */
  @XmlElement(name = "name")
  protected String name;

  /** The description */
  @XmlElement(name = "description")
  protected String description;

  /** The description */
  @XmlElement(name = "organization")
  protected JaxbOrganization organization;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbRole() {
  }

  /**
   * Constructs a role with the specified name and organization.
   * 
   * @param name
   *          the name
   * @param organization
   *          the organization
   */
  public JaxbRole(String name, JaxbOrganization organization) throws IllegalArgumentException {
    super();
    this.name = name;
    this.organization = organization;
  }

  /**
   * Constructs a role with the specified name, organization and description.
   * 
   * @param name
   *          the name
   * @param organization
   *          the organization
   * @param description
   *          the description
   */
  public JaxbRole(String name, JaxbOrganization organization, String description) throws IllegalArgumentException {
    this(name, organization);
    this.description = description;
  }

  public static JaxbRole fromRole(Role role) {
    if (role instanceof JaxbRole)
      return (JaxbRole) role;
    JaxbOrganization org = JaxbOrganization.fromOrganization(role.getOrganization());
    return new JaxbRole(role.getName(), org, role.getDescription());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.Role#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.Role#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.Role#getOrganization()
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(name, organization);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Role))
      return false;
    Role other = (Role) obj;
    return name.equals(other.getName()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(name).append(":").append(organization).toString();
  }

}
