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
package org.opencastproject.security.impl.jpa;

import org.opencastproject.security.api.Role;
import org.opencastproject.util.EqualsUtil;

import java.util.Objects;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * JPA-annotated role object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "oc_role", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "organization" }))
@NamedQueries({
        @NamedQuery(name = "Role.findByQuery", query = "select r from JpaRole r where r.organization.id=:org and UPPER(r.name) like :query or UPPER(r.description) like :query"),
        @NamedQuery(name = "Role.findByName", query = "Select r FROM JpaRole r where r.name = :name and r.organization.id = :org"),
        @NamedQuery(name = "Role.findAll", query = "Select r FROM JpaRole r where r.organization.id = :org") })
public final class JpaRole implements Role {
  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "name", length = 128)
  private String name;

  @OneToOne()
  @JoinColumn(name = "organization")
  private JpaOrganization organization;

  @Column(name = "description", nullable = true)
  private String description;

  @Transient
  private Type type = Type.INTERNAL;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaRole() {
  }

  /**
   * Constructs a role with the specified name and organization.
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   */
  public JpaRole(String name, JpaOrganization organization) {
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
  public JpaRole(String name, JpaOrganization organization, String description) {
    this(name, organization);
    this.description = description;
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
   * @param type
   *          the role {@link Type}
   */
  public JpaRole(String name, JpaOrganization organization, String description, Type type) {
    this(name, organization, description);
    this.type = type;
  }

  /**
   * Gets the identifier.
   *
   * @return the identifier
   */
  public Long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description
   *
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getOrganizationId() {
    return organization.getId();
  }

  public JpaOrganization getJpaOrganization() {
    return organization;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(name, getOrganizationId());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Role))
      return false;
    Role other = (Role) obj;
    return name.equals(other.getName())
            && Objects.equals(getOrganizationId(), other.getOrganizationId());
  }

  @Override
  public String toString() {
    return name + ":" + getOrganizationId();
  }

}
