/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.security.api;


import java.util.Objects;

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

  @XmlElement(name = "organization")
  protected JaxbOrganization organization;

  @XmlElement(name = "organization-id")
  protected String organizationId;

  @XmlElement(name = "type")
  protected Type type = Type.INTERNAL;

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
    this.organizationId = organization.getId();
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

  /**
   * Constructs a role with the specified name, organization, description, and persistence settings.
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   * @param description
   *          the description
   * @param type
   *          the role {@link type}
   */
  public JaxbRole(String name, JaxbOrganization organization, String description, Type type) throws IllegalArgumentException {
    this(name, organization, description);
    this.type = type;
  }


  /**
   * Constructs a role with the specified name, organization identifier, description, and persistence settings.
   *
   * @param name
   *          the name
   * @param organizationId
   *          the organization identifier
   * @param description
   *          the description
   * @param type
   *          the role {@link type}
   */
  public JaxbRole(String name, String organizationId, String description, Type type) throws IllegalArgumentException {
    super();
    this.name = name;
    this.organizationId = organizationId;
    this.description = description;
    this.type = type;
  }

  public static JaxbRole fromRole(Role role) {
    if (role instanceof JaxbRole)
      return (JaxbRole) role;
    return new JaxbRole(role.getName(), role.getOrganizationId(), role.getDescription(), role.getType());
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
   */
  public String getOrganizationId() {
    if (organizationId != null) {
      return organizationId;
    }
    if (organization != null) {
      return organization.getId();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.Role#getType()
   */
  public Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(name, getOrganizationId());
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
    return name.equals(other.getName())
            && Objects.equals(getOrganizationId(), other.getOrganizationId());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name + ":" + getOrganizationId();
  }
}
