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

import org.opencastproject.util.EqualsUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user", namespace = "http://org.opencastproject.security", propOrder = { "userName", "name", "email",
        "provider", "isManageable", "roles", "organization" })
@XmlRootElement(name = "user", namespace = "http://org.opencastproject.security")
public final class JaxbUser implements User {

  /** The user name */
  @XmlElement(name = "username")
  protected String userName;

  /** The user's name */
  @XmlElement(name = "name")
  protected String name;

  /** The user's email address */
  @XmlElement(name = "email")
  protected String email;

  /** The user's provider */
  @XmlElement(name = "provider")
  protected String provider;

  @XmlElement(name = "manageable")
  protected boolean isManageable = false;

  /** The roles */
  @XmlElement(name = "role")
  @XmlElementWrapper(name = "roles")
  protected Set<JaxbRole> roles;

  /** The optional password. Note that this will never be serialized to xml */
  @XmlTransient
  protected String password;

  /** The user's home organization identifier */
  @XmlElement(name = "organization")
  protected JaxbOrganization organization;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbUser() {
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles and no password set.
   *
   * @param userName
   *          the username
   * @param provider
   *          the provider
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public JaxbUser(String userName, String provider, JaxbOrganization organization, JaxbRole... roles)
          throws IllegalArgumentException {
    this(userName, provider, organization, new HashSet<JaxbRole>(Arrays.asList(roles)));
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles.
   *
   * @param userName
   *          the username
   * @param password
   *          the password
   * @param provider
   *          the provider
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public JaxbUser(String userName, String password, String provider, JaxbOrganization organization, JaxbRole... roles)
          throws IllegalArgumentException {
    this(userName, password, provider, organization, new HashSet<JaxbRole>(Arrays.asList(roles)));
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles.
   *
   * @param userName
   *          the username
   * @param password
   *          the password
   * @param provider
   *          the provider
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public JaxbUser(String userName, String password, String provider, JaxbOrganization organization, Set<JaxbRole> roles)
          throws IllegalArgumentException {
    this(userName, password, null, null, provider, organization, roles);
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles and no password set.
   *
   * @param userName
   *          the username
   * @param provider
   *          the provider
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public JaxbUser(String userName, String provider, JaxbOrganization organization, Set<JaxbRole> roles)
          throws IllegalArgumentException {
    this(userName, null, provider, organization, roles);
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles.
   *
   * @param userName
   *          the username
   * @param password
   *          the password
   * @param name
   *          the name
   * @param email
   *          the email
   * @param provider
   *          the provider
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public JaxbUser(String userName, String password, String name, String email, String provider,
          JaxbOrganization organization, Set<JaxbRole> roles) throws IllegalArgumentException {
    if (StringUtils.isBlank(userName))
      throw new IllegalArgumentException("Username must be set");
    if (organization == null)
      throw new IllegalArgumentException("Organization must be set");
    this.userName = userName;
    this.password = password;
    this.name = name;
    this.email = email;
    this.provider = provider;
    this.organization = organization;
    if (roles == null) {
      this.roles = new HashSet<>();
    } else {
      for (Role role : roles) {
        if (!Objects.equals(organization.getId(), role.getOrganizationId())) {
          throw new IllegalArgumentException("Role " + role + " is not from the same organization!");
        }
      }
      this.roles = roles;
    }
  }

  /**
   * Creates a JAXB user from a regular user object.
   *
   * @param user
   *          the user
   * @return the JAXB user
   */
  public static JaxbUser fromUser(User user) {
    if (user instanceof JaxbUser)
      return (JaxbUser) user;
    return fromUser(user, Collections.<JaxbRole> emptySet());
  }

  /**
   * Creates a JAXB user from a regular user object with an additional set of roles.
   *
   * @param user
   *          the user
   * @return the JAXB user
   */
  public static JaxbUser fromUser(User user, Collection<? extends Role> extraRoles) {
    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    for (Role role : user.getRoles()) {
      roles.add(JaxbRole.fromRole(role));
    }
    for (Role role : extraRoles) {
      roles.add(JaxbRole.fromRole(role));
    }

    JaxbUser jaxbUser = new JaxbUser(user.getUsername(), user.getPassword(), user.getName(), user.getEmail(),
            user.getProvider(), JaxbOrganization.fromOrganization(user.getOrganization()), roles);
    jaxbUser.setManageable(user.isManageable());
    return jaxbUser;
  }

  /**
   * @see org.opencastproject.security.api.User#getUsername()
   */
  @Override
  public String getUsername() {
    return userName;
  }

  /**
   * @see org.opencastproject.security.api.User#getPassword()
   */
  @Override
  public String getPassword() {
    return password;
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
    return new HashSet<>(roles);
  }

  /**
   * @see org.opencastproject.security.api.User#hasRole(java.lang.String)
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
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof User))
      return false;
    User other = (User) obj;
    return userName.equals(other.getUsername()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(userName, organization, provider);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("%s:%s:%s", userName, organization, provider);
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
    return isManageable;
  }

  public void setManageable(boolean isManageable) {
    this.isManageable = isManageable;
  }

}
