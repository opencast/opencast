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
package org.opencastproject.lifecyclemanagement.impl;

import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.security.api.AccessControlEntry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

// TODO: Reduce duplicate code between this class and the playlist version
//  This is very much a copy of PlaylistAccessControlEntry.java. However, creating a general super class for
//  this and the playlist version has proven difficult. The @MappedSuperClass annotation seems like the go
//  to solution, however it seems to cause errors with out JPAProvider. Still, it would be good to investigate
//  how we can reduce duplicate code here in the future.
/**
 * This has the same fields as an {@link AccessControlEntry}, but since policies purely exist in the database, ACLs
 * need to be persisted alongside them, which is what this is for.
 */
@Entity(name = "LifeCyclePolicyAccessControlEntry")
@Table(name = "oc_lifecyclepolicy_access_control_entry")
public class LifeCyclePolicyAccessControlEntryImpl implements LifeCyclePolicyAccessControlEntry {

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lifecyclepolicy_id", nullable = false)
  private LifeCyclePolicyImpl policy;

  /**
   * Default constructor
   */
  public LifeCyclePolicyAccessControlEntryImpl() {

  }

  public LifeCyclePolicyAccessControlEntryImpl(boolean allow, String role, String action) {
    this.allow = allow;
    this.role = role;
    this.action = action;
  }

  public LifeCyclePolicyAccessControlEntryImpl(long id, boolean allow, String role, String action) {
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

  public LifeCyclePolicy getPolicy() {
    return policy;
  }

  public void setPolicy(LifeCyclePolicy policy) {
    this.policy = (LifeCyclePolicyImpl) policy;
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
