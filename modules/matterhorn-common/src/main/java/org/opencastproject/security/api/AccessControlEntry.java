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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A tuple of role, action, and whether the combination is to be allowed.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ace", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "ace", namespace = "http://org.opencastproject.security")
public final class AccessControlEntry {

  /** The role */
  private String role = null;

  /** The action */
  private String action = null;

  /** Whether this role is allowed to take this action */
  private boolean allow = false;

  /**
   * No-arg constructor needed by JAXB
   */
  public AccessControlEntry() {
  }

  /**
   * Constructs an access control entry for a role, action, and allow tuple
   * 
   * @param role
   *          the role
   * @param action
   *          the action
   * @param allow
   *          Whether this role is allowed to take this action
   */
  public AccessControlEntry(String role, String action, boolean allow) {
    this.role = role;
    this.action = action;
    this.allow = allow;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the action
   */
  public String getAction() {
    return action;
  }

  /**
   * @return the allow
   */
  public boolean isAllow() {
    return allow;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AccessControlEntry) {
      AccessControlEntry other = (AccessControlEntry) obj;
      return this.allow == other.allow && this.role.equals(other.role) && this.action.equals(other.action);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (role + action + Boolean.toString(allow)).hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(role).append(" is ");
    if (!allow)
      sb.append("not ");
    sb.append("allowed to ");
    sb.append(action);
    return sb.toString();
  }

}