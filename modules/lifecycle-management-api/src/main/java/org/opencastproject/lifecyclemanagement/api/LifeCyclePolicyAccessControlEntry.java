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

/**
 * Represents a single access control entry for a {@link LifeCyclePolicy}
 */
public interface LifeCyclePolicyAccessControlEntry {

  /**
   * Gets the entry's identifier
   *
   * @return the entry identifier
   */
  long getId();

  /**
   * Sets the entry's identifier
   *
   * @param id the entry identifier
   */
  void setId(long id);

  /**
   * Returns whether this entry allows or denies the action
   *
   * @return true if the action is allowed
   */
  boolean isAllow();

  /**
   * Sets whether this entry allows or denies the action
   *
   * @param allow true if the action is allowed
   */
  void setAllow(boolean allow);

  /**
   * Gets the role this entry applies to
   *
   * @return the role
   */
  String getRole();

  /**
   * Sets the role this entry applies to
   *
   * @param role the role
   */
  void setRole(String role);

  /**
   * Gets the action this entry allows or denies
   *
   * @return the action
   */
  String getAction();

  /**
   * Sets the action this entry allows or denies
   *
   * @param action the action
   */
  void setAction(String action);

  /**
   * Converts this entry to an {@link org.opencastproject.security.api.AccessControlEntry}
   *
   * @return the converted access control entry
   */
  org.opencastproject.security.api.AccessControlEntry toAccessControlEntry();

  /**
   * Populates this entry from an {@link org.opencastproject.security.api.AccessControlEntry}
   *
   * @param accessControlEntry the access control entry to populate from
   */
  void fromAccessControlEntry(org.opencastproject.security.api.AccessControlEntry accessControlEntry);
}
