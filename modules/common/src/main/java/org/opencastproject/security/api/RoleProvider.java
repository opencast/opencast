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

import java.util.Iterator;
import java.util.List;

/**
 * Mix-in interface for directories that can list roles.
 */
public interface RoleProvider {

  /**
   * Returns the roles for this user or an empty array if no roles are applicable.
   *
   * @param userName
   *          the user id
   * @return the set of roles
   */
  List<Role> getRolesForUser(String userName);

  /**
   * Returns the identifier for the organization that is defining this set of roles.
   *
   * @return the defining organization
   */
  String getOrganization();

  /**
   * Return the found roles as an iterator.
   *
   * @param query
   *          the query. Use the wildcards "_" to match any single character and "%" to match an arbitrary number of
   *          characters (including zero characters).
   * @param offset
   *          the offset
   * @param limit
   *          the limit. 0 means no limit
   * @return an iterator of role's
   * @throws IllegalArgumentException
   *           if the query is <code>null</code>
   */
  Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit);

}
