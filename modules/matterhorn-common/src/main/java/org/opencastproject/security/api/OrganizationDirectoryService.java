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

import org.opencastproject.util.NotFoundException;

import java.net.URL;
import java.util.List;

/**
 * Manages organizations.
 */
public interface OrganizationDirectoryService {

  /**
   * Gets an organization by its identifier.
   *
   * @param id
   *          the identifier
   * @return the organization with this identifier
   */
  Organization getOrganization(String id) throws NotFoundException;

  /**
   * Gets an organization by request URL.
   *
   * @param url
   *          a request URL
   * @return the organization that is mapped to this URL
   */
  Organization getOrganization(URL url) throws NotFoundException;

  /**
   * Gets all registered organizations.
   *
   * @return the organizations
   */
  List<Organization> getOrganizations();
}
