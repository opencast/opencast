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
 * Provides access to the current user's username and roles, if any.
 */
public interface SecurityService {

  /**
   * Gets the current user, or the local organization's anonymous user if the user has not been authenticated.
   * 
   * @return the user
   */
  User getUser();

  /**
   * Gets the organization associated with the current thread context.
   * 
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Sets the organization for the calling thread.
   * 
   * @param organization
   *          the organization
   */
  void setOrganization(Organization organization);

  /**
   * Sets the current thread's user context to another user. This is useful when spawning new threads that must contain
   * the parent thread's user context.
   * 
   * @param user
   *          the user to set for the current user context
   */
  void setUser(User user);

}
