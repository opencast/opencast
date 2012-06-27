/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.security.api;

/**
 * A marker interface for federation of all {@link UserProvider}s.
 */
public interface UserDirectoryService {

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

}
