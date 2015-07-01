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

import static org.opencastproject.userdirectory.JpaUserReferenceProvider.PROVIDER_NAME;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.util.EqualsUtil;

import java.util.Date;
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
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * JPA-annotated user reference object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "mh_user_ref", uniqueConstraints = { @UniqueConstraint(columnNames = { "username", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "UserReference.findByQuery", query = "select u from JpaUserReference u where UPPER(u.username) like :query and u.organization.id = :org"),
        @NamedQuery(name = "UserReference.findByUsername", query = "select u from JpaUserReference u where u.username=:u and u.organization.id = :org"),
        @NamedQuery(name = "UserReference.findAll", query = "select u from JpaUserReference u where u.organization.id = :org"),
        @NamedQuery(name = "UserReference.countAll", query = "select COUNT(u) from JpaUserReference u where u.organization.id = :org") })
public class JpaUserReference {
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

  @Column(name = "login_mechanism")
  protected String loginMechanism;

  @Column(name = "last_login")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastLogin;

  @OneToOne()
  @JoinColumn(name = "organization")
  protected JpaOrganization organization;

  @ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
  @JoinTable(name = "mh_user_ref_role", joinColumns = { @JoinColumn(name = "user_id") }, inverseJoinColumns = { @JoinColumn(name = "role_id") }, uniqueConstraints = { @UniqueConstraint(columnNames = {
          "user_id", "role_id" }) })
  protected Set<JpaRole> roles;

  public User toUser() {
    Set<JaxbRole> roleSet = new HashSet<JaxbRole>();
    for (JpaRole role : roles) {
      roleSet.add(JaxbRole.fromRole(role));
    }
    return new JaxbUser(username, null, name, email, PROVIDER_NAME, JaxbOrganization.fromOrganization(organization),
            roleSet);
  }

  /**
   * No-arg constructor needed by JPA
   */
  public JpaUserReference() {
  }

  /**
   * Constructs a user with the specified username, name, email, login mechanism, last login date and organization.
   *
   * @param username
   *          the username
   * @param name
   *          the name
   * @param email
   *          the email address
   * @param loginMechanism
   *          the login mechanism
   * @param lastLogin
   *          the last login date
   * @param organization
   *          the organization
   */
  public JpaUserReference(String username, String name, String email, String loginMechanism, Date lastLogin,
          JpaOrganization organization) {
    super();
    this.username = username;
    this.name = name;
    this.email = email;
    this.loginMechanism = loginMechanism;
    this.lastLogin = lastLogin;
    this.organization = organization;
    this.roles = new HashSet<JpaRole>();
  }

  /**
   * Constructs a user with the specified username, name, email, login mechanism, last login date, organization and
   * roles.
   *
   * @param username
   *          the username
   * @param name
   *          the name
   * @param email
   *          the email address
   * @param loginMechanism
   *          the login mechanism
   * @param lastLogin
   *          the last login date
   * @param organization
   *          the organization
   * @param roles
   *          the roles
   */
  public JpaUserReference(String username, String name, String email, String loginMechanism, Date lastLogin,
          JpaOrganization organization, Set<JpaRole> roles) {
    this(username, name, email, loginMechanism, lastLogin, organization);
    for (Role role : roles) {
      if (role.getOrganization() == null || !organization.getId().equals(role.getOrganization().getId()))
        throw new IllegalArgumentException("Role " + role + " is not from the same organization!");
    }
    this.roles = roles;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void setLoginMechanism(String loginMechanism) {
    this.loginMechanism = loginMechanism;
  }

  public String getLoginMechanism() {
    return loginMechanism;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setRoles(Set<JpaRole> roles) {
    this.roles = roles;
  }

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
    if (!(obj instanceof JpaUserReference))
      return false;
    JpaUserReference other = (JpaUserReference) obj;
    return username.equals(other.getUsername()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(username, organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(username).append(":").append(organization).toString();
  }

}
