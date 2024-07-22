/*
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
package org.opencastproject.lifecyclemanagement.api;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class AccessControlEntry {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "allow", nullable = false)
  private boolean allow;

  @Column(name = "role", nullable = false)
  private String role;

  @Column(name = "action", nullable = false)
  private String action;

  /**
   * Default constructor
   */
  public AccessControlEntry() {

  }

  public AccessControlEntry(boolean allow, String role, String action) {
    this.allow = allow;
    this.role = role;
    this.action = action;
  }

  public AccessControlEntry(long id, boolean allow, String role, String action) {
    this.id = id;
    this.allow = allow;
    this.role = role;
    this.action = action;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public boolean isAllow() {
    return allow;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public org.opencastproject.security.api.AccessControlEntry toAccessControlEntry() {
    return new org.opencastproject.security.api.AccessControlEntry(this.getRole(), this.getAction(), this.isAllow());
  }

  public void fromAccessControlEntry(org.opencastproject.security.api.AccessControlEntry accessControlEntry) {
    this.setAllow(accessControlEntry.isAllow());
    this.setRole(accessControlEntry.getRole());
    this.setAction(accessControlEntry.getAction());
  }
}
