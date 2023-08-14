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

import java.util.Map;

public interface Organization {

  /**
   * @return the id
   */
  String getId();

  /**
   * Returns the name for the local anonymous role.
   *
   * @return the anonymous role name
   */
  String getAnonymousRole();

  /**
   * Returns the name for the local admin role.
   *
   * @return the admin role name
   */
  String getAdminRole();

  /**
   * @return the name
   */
  String getName();

  /**
   * Returns the organizational properties
   *
   * @return the properties
   */
  Map<String, String> getProperties();

  /**
   * Returns the server names and the corresponding ports that have been registered with this organization.
   *
   * @return the servers
   */
  Map<String, Integer> getServers();

}
