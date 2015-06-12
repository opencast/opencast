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

/**
 * A marker interface for federation of all {@link UserProvider}s.
 */
public interface UserDirectoryService {

  /**
   * Gets all known users.
   *
   * @return the users
   */
  Iterator<User> getUsers();

  /**
   * Loads a user by username, or returns null if this user is not known to the thread's current organization.
   *
   * @param userName
   *          the username
   * @return the user
   * @throws IllegalStateException
   *           if no organization is set for the current thread
   */
  User loadUser(String userName);

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
   * Discards any cached value for given user name.
   *
   * @param userName
   *          the user name
   */
  void invalidate(String userName);

}
