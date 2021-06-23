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

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.Role;
import org.opencastproject.util.EqualsUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * JPA-annotated group object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "oc_group", uniqueConstraints = { @UniqueConstraint(columnNames = { "group_id", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "Group.findAll", query = "Select g FROM JpaGroup g WHERE g.organization.id = :organization"),
        @NamedQuery(name = "Group.findByUser", query = "Select g FROM JpaGroup g WHERE g.organization.id = :organization AND :username MEMBER OF g.members"),
        @NamedQuery(name = "Group.findById", query = "Select g FROM JpaGroup g WHERE g.groupId = :groupId AND g.organization.id = :organization"),
        @NamedQuery(name = "Group.findByRole", query = "Select g FROM JpaGroup g WHERE g.role = :role AND g.organization.id = :organization") })
public final class JpaGroup implements Group {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "group_id", length = 128)
  private String groupId;

  @Column(name = "name", length = 128)
  private String name;

  @OneToOne()
  @JoinColumn(name = "organization")
  private JpaOrganization organization;

  @Column(name = "description")
  private String description;

  @Column(name = "role")
  private String role;

  @ElementCollection
  @CollectionTable(name = "oc_group_member", joinColumns = {
      @JoinColumn(name = "group_id", nullable = false) })
  @Column(name = "member")
  private Set<String> members;

  @ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
  @JoinTable(name = "oc_group_role", joinColumns = {
      @JoinColumn(name = "group_id")
    }, inverseJoinColumns = {
      @JoinColumn(name = "role_id")
    }, uniqueConstraints = {
      @UniqueConstraint(name = "UNQ_oc_group_role", columnNames = { "group_id", "role_id" }) })
  private Set<JpaRole> roles;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaGroup() {
  }

  /**
   * Constructs a group with the specified groupId, name, description and group role.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @throws IllegalArgumentException
   *           if group id or name is longer than 128 Bytes
   */
  public JpaGroup(String groupId, JpaOrganization organization, String name, String description)
          throws IllegalArgumentException {
    super();
    if (groupId.length() > 128)
      throw new IllegalArgumentException("Group id must not be longer than 128 Bytes");
    if (name.length() > 128)
      throw new IllegalArgumentException("Name must not be longer than 128 Bytes");
    this.groupId = groupId;
    this.organization = organization;
    this.name = name;
    this.description = description;
    this.role = ROLE_PREFIX + groupId.toUpperCase();
    this.roles = new HashSet<JpaRole>();
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   * @throws IllegalArgumentException
   *           if group id or name is longer than 128 Bytes
   */
  public JpaGroup(String groupId, JpaOrganization organization, String name, String description, Set<JpaRole> roles)
          throws IllegalArgumentException {
    this(groupId, organization, name, description);
    this.roles = roles;
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   * @param members
   *          the group members
   * @throws IllegalArgumentException
   *           if group id or name is longer than 128 Bytes
   */
  public JpaGroup(String groupId, JpaOrganization organization, String name, String description, Set<JpaRole> roles,
          Set<String> members) throws IllegalArgumentException {
    this(groupId, organization, name, description, roles);
    this.members = members;
  }

  /**
   * @see org.opencastproject.security.api.Group#getGroupId()
   */
  @Override
  public String getGroupId() {
    return groupId;
  }

  /**
   * @see org.opencastproject.security.api.Group#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the group name
   *
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @see org.opencastproject.security.api.Group#getOrganization()
   */
  @Override
  public JpaOrganization getOrganization() {
    return organization;
  }

  /**
   * @see org.opencastproject.security.api.Group#getDescription()
   */
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
  public String getRole() {
    return role;
  }

  @Override
  public Set<String> getMembers() {
    return members;
  }

  /**
   * Sets the members
   *
   * @param members
   *          the members
   */
  public void setMembers(Set<String> members) {
    this.members = members;
  }

  /**
   * Add a member
   *
   * @param member
   *          The member's name.
   */
  public void addMember(String member) {
    if (members == null) {
      members = new HashSet<>();
    }
    members.add(member);
  }

  /**
   * Remove a member
   *
   * @param member
   *          The member's name.
   */
  public void removeMember(String member) {
    if (members != null) {
      members.remove(member);
    }
  }

  @Override
  public Set<Role> getRoles() {
    return new HashSet<Role>(roles);
  }

  /**
   * Get only the names of the roles
   *
   * @return the role names in a set
   */
  public Set<String> getRoleNames() {
    return roles.stream().map(role -> role.getName()).collect(Collectors.toSet());
  }

  /**
   * Sets the roles
   *
   * @param roles
   *          the roles
   */
  public void setRoles(Set<JpaRole> roles) {
    this.roles = roles;
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, organization);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Group))
      return false;
    Group other = (Group) obj;
    return groupId.equals(other.getGroupId()) && organization.equals(other.getOrganization());
  }

  @Override
  public String toString() {
    return new StringBuilder(groupId).append(":").append(organization).toString();
  }

}
