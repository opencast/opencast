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

/**
 * Represent a role in Opencast
 */
public interface Role {

  /**
   * The type of role:
   *  SYSTEM - A role granted automatically by Opencast, not persisted
   *  INTERNAL - A role indicating an ability that the user has within Opencast, persisted
   *  GROUP - A role indicating membership of an Opencast group, persisted
   *  EXTERNAL - A role granted to a user from an external system, not persisted
   *  EXTERNAL_GROUP - A role indicating membership of an Opencast group from an external system, not persisted
   *  DERIVED - A role which is derived from the user's group membership (a role which the group has), not persisted
   */
  enum Type {
    INTERNAL, SYSTEM, GROUP, EXTERNAL, EXTERNAL_GROUP, DERIVED;
  }

  /**
   * The target (intended purpose) of a set of roles
   *  USER - Roles which are assigned to users and/or groups to provide access to capabilities
   *  ACL  - Roles which are used to manage access to resources (Event, Series) in an ACL
   *  ALL  - All roles
   */
  enum Target {
    USER, ACL, ALL;
  }

  /**
   * Gets the role name
   *
   * @return the role name
   */
  String getName();

  /**
   * Gets the role description
   *
   * @return the role description
   */
  String getDescription();

  /**
   * Returns the role's organization identifier.
   *
   * @return the organization identifier
   */
  String getOrganizationId();

  /**
   * Returns the role's {@link Type}
   * 
   * @return the type
   */
  Type getType();
}
