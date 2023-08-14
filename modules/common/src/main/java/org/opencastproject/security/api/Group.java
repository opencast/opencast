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

import java.util.Set;

/**
 * Represent a group in Opencast
 */
public interface Group {

  /** Prefix of every generated group role */
  String ROLE_PREFIX = "ROLE_GROUP_";

  /**
   * Gets the group identifier.
   *
   * @return the group identifier
   */
  String getGroupId();

  /**
   * Gets the group name.
   *
   * @return the group name
   */
  String getName();

  /**
   * Gets the user's organization.
   *
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Gets the role description.
   *
   * @return the description
   */
  String getDescription();

  /**
   * Gets the group role.
   *
   * @return the group role
   */
  String getRole();

  /**
   * Gets the group members
   *
   * @return the group members
   */
  Set<String> getMembers();

  /**
   * Gets the group's roles.
   *
   * @return the group's roles
   */
  Set<Role> getRoles();

}
