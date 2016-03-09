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
 * An interface for entities that are interested in changes to the list of registered organizations.
 */
public interface OrganizationDirectoryListener {

  /**
   * This callback notifies listeners about an organization that appeared in the organization directory.
   *
   * @param organization
   *          the organization
   */
  void organizationRegistered(Organization organization);

  /**
   * This callback notifies listeners about an organization that disappeared from the organization directory.
   *
   * @param organization
   *          the organization
   */
  void organizationUnregistered(Organization organization);

  /**
   * This callback notifies listeners about an organization that was updated.
   *
   * @param organization
   *          the organization
   */
  void organizationUpdated(Organization organization);

}
