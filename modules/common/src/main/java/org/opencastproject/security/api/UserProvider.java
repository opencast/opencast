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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Provides access to users and roles.
 */
public interface UserProvider {

  /** The constant indicating that a provider should be consulted for all organizations */
  String ALL_ORGANIZATIONS = "*";

  /**
   * Gets the provider name
   *
   * @return the provider name
   */
  String getName();

  /**
   * Gets all known users.
   *
   * @return the users
   */
  Iterator<User> getUsers();

  /**
   * Loads a user by username, or returns null if this user is not known to this provider.
   *
   * @param userName
   *          the username
   * @return the user
   */
  User loadUser(String userName);

  /**
   * Returns the number of users in the provider
   *
   * @return the count of users in the provider
   */
  long countUsers();

  /**
   * Returns the identifier for the organization that is associated with this user provider. If equal to
   * {@link #ALL_ORGANIZATIONS}, this provider will always be consulted, regardless of the organization.
   *
   * @return the defining organization
   */
  String getOrganization();

  /**
   * Return the found user's as an iterator.
   *
   * @param query
   *          the query. Use the wildcards "_" to match any single character and "%" to match an arbitrary number of
   *          characters (including zero characters).
   * @param offset
   *          the offset
   * @param limit
   *          the limit. 0 means no limit
   * @return an iterator of user's
   * @throws IllegalArgumentException
   *           if the query is <code>null</code>
   */
  Iterator<User> findUsers(String query, int offset, int limit);

  /**
   * Find a list of users by their user names
   *
   * Note that the default implementation of this might be slow, as it calls <code>loadUser</code> on every single user.
   * @param userNames A list of user names
   * @return A list of resolved user objects
   */
  default Iterator<User> findUsers(Collection<String> userNames) {
    List<User> result = new ArrayList<>(0);
    for (String name : userNames) {
      final User e = loadUser(name);
      if (e != null) {
        result.add(e);
      }
    }
    return result.iterator();
  }

  /**
   * Discards any cached value for given user name.
   *
   * @param userName
   *          the user name
   */
  void invalidate(String userName);

}
