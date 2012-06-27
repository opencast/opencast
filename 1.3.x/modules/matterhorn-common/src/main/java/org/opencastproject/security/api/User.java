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

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "user", namespace = "http://org.opencastproject.security")
public final class User {

  /** The user name */
  protected String userName;

  /** The roles */
  protected String[] roles;

  /** The optional password. Note that this will never be serialized to xml */
  @XmlTransient
  protected String password;

  /** The user's home organization identifier */
  protected String organization;

  /**
   * No-arg constructor needed by JAXB
   */
  public User() {
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles and no password set.
   * 
   * @param userName
   *          the username
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public User(String userName, String organization, String[] roles) throws IllegalArgumentException {
    this(userName, null, organization, roles);
  }

  /**
   * Constructs a user which is a member of the given organization that has the specified roles.
   * 
   * @param userName
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param roles
   *          the set of roles for this user
   * @throws IllegalArgumentException
   *           if <code>userName</code> or <code>organization</code> is <code>null</code>
   */
  public User(String userName, String password, String organization, String[] roles) throws IllegalArgumentException {
    if (StringUtils.isBlank(userName))
      throw new IllegalArgumentException("Username must be set");
    if (StringUtils.isBlank(organization))
      throw new IllegalArgumentException("Organization must be set");
    this.userName = userName;
    this.password = password;
    this.organization = organization;
    if (roles == null) {
      this.roles = new String[0];
    } else {
      Arrays.sort(roles);
      this.roles = roles;
    }
  }

  /**
   * Gets this user's unique account name.
   * 
   * @return the account name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Gets this user's password, if available.
   * 
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the user's organization identifier.
   * 
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Gets the user's roles. For anonymous users, this will return {@link Anonymous}.
   * 
   * @return the user's roles
   */
  public String[] getRoles() {
    return roles;
  }

  /**
   * Returns whether the user is in a specific role.
   * 
   * @param role
   *          the role to check
   * @return whether the role is one of this user's roles
   */
  public boolean hasRole(String role) {
    for (String r : roles) {
      if (r.equals(role))
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
    String organization = other.getOrganization();
    return this.userName.equals(other.userName) && this.organization.equals(organization);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return userName.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(organization).append(":").append(userName);
    return sb.toString();
  }

}
