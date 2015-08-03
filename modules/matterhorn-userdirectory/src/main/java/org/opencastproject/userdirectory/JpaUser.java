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

package org.opencastproject.userdirectory;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.util.EqualsUtil;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * JPA-annotated user object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "mh_user", uniqueConstraints = { @UniqueConstraint(columnNames = { "username", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "User.findByQuery", query = "select u from JpaUser u where UPPER(u.username) like :query and u.organization.id = :org"),
        @NamedQuery(name = "User.findByIdAndOrg", query = "select u from JpaUser u where u.id=:id and u.organization.id = :org"),
        @NamedQuery(name = "User.findByUsername", query = "select u from JpaUser u where u.username=:u and u.organization.id = :org"),
        @NamedQuery(name = "User.findAll", query = "select u from JpaUser u where u.organization.id = :org"),
        @NamedQuery(name = "User.countAll", query = "select COUNT(u) from JpaUser u where u.organization.id = :org") })
public class JpaUser implements User {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "username", length = 128)
  protected String username;

  @Column(name = "name")
  protected String name;

  @Column(name = "email")
  protected String email;

  @Transient
  protected String provider;

  @Transient
  protected boolean manageable = true;

  @Lob
  @Column(name = "password", length = 65535)
  protected String password;

  @OneToOne()
  @JoinColumn(name = "organization")
  protected JpaOrganization organization;

  @ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
  @JoinTable(name = "mh_user_role", joinColumns = { @JoinColumn(name = "user_id") }, inverseJoinColumns = { @JoinColumn(name = "role_id") }, uniqueConstraints = { @UniqueConstraint(columnNames = {
          "user_id", "role_id" }) })
  protected Set<JpaRole> roles;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaUser() {
  }

  /**
   * Constructs a user with the specified username, password, name, email and provider.
   *
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param email
   *          the email
   * @param provider
   *          the provider
   * @param manageable
   *          whether the user is manageable
   */
  public JpaUser(String username, String password, JpaOrganization organization, String name, String email,
          String provider, boolean manageable) {
    super();
    this.username = username;
    this.password = password;
    this.organization = organization;
    this.name = name;
    this.email = email;
    this.provider = provider;
    this.manageable = manageable;
    this.roles = new HashSet<JpaRole>();
  }

  /**
   * Constructs a user with the specified username, password, provider and roles.
   *
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param provider
   *          the provider
   * @param manageable
   *          whether the user is manageable
   * @param roles
   *          the roles
   */
  public JpaUser(String username, String password, JpaOrganization organization, String provider, boolean manageable,
          Set<JpaRole> roles) {
    this(username, password, organization, null, null, provider, manageable);
    for (Role role : roles) {
      if (role.getOrganization() == null || !organization.getId().equals(role.getOrganization().getId()))
        throw new IllegalArgumentException("Role " + role + " is not from the same organization!");
    }
    this.roles = roles;
  }

  /**
   * Constructs a user with the specified username, password, name, email, provider and roles.
   *
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param email
   *          the email
   * @param provider
   *          the provider
   * @param manageable
   *          whether the user is manageable
   * @param roles
   *          the roles
   */
  public JpaUser(String username, String password, JpaOrganization organization, String name, String email,
          String provider, boolean manageable, Set<JpaRole> roles) {
    this(username, password, organization, name, email, provider, manageable);
    for (Role role : roles) {
      if (role.getOrganization() == null || !organization.getId().equals(role.getOrganization().getId()))
        throw new IllegalArgumentException("Role " + role + " is not from the same organization!");
    }
    this.roles = roles;
  }

  /**
   * Gets this user's clear text password.
   *
   * @return the user account's password
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * @see org.opencastproject.security.api.User#canLogin()
   */
  @Override
  public boolean canLogin() {
    return true;
  }

  /**
   * @see org.opencastproject.security.api.User#getUsername()
   */
  @Override
  public String getUsername() {
    return username;
  }

  /**
   * @see org.opencastproject.security.api.User#hasRole(String)
   */
  @Override
  public boolean hasRole(String roleName) {
    for (Role role : roles) {
      if (role.getName().equals(roleName))
        return true;
    }
    return false;
  }

  /**
   * @see org.opencastproject.security.api.User#getOrganization()
   */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  /**
   * @see org.opencastproject.security.api.User#getRoles()
   */
  @Override
  public Set<Role> getRoles() {
    return new HashSet<Role>(roles);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof User))
      return false;
    User other = (User) obj;
    return username.equals(other.getUsername()) && organization.equals(other.getOrganization())
            && EqualsUtil.eq(provider, other.getProvider());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(username, organization, provider);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(username).append(":").append(organization).append(":").append(provider).toString();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  @Override
  public boolean isManageable() {
    return manageable;
  }
}
