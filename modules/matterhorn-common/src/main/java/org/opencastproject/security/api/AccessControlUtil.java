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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

/**
 * Provides common functions helpful in dealing with {@link AccessControlList}s.
 */
public final class AccessControlUtil {

  /** Disallow construction of this utility class */
  private AccessControlUtil() {
  }

  /**
   * Determines whether the {@link AccessControlList} permits a user to perform an action.
   * 
   * There are three ways a user can be allowed to perform an action:
   * <ol>
   * <li>They have the superuser role</li>
   * <li>They have their local organization's admin role</li>
   * <li>They have a role listed in the series ACL, with write permission</li>
   * </ol>
   * 
   * @param acl
   *          the {@link AccessControlList}
   * @param user
   *          the user
   * @param org
   *          the organization
   * @param action
   *          the action to perform
   * @return whether this action should be allowed
   * @throws IllegalArgumentException
   *           if any of the arguments are null
   */
  public static boolean isAuthorized(AccessControlList acl, User user, Organization org, String action) {
    if (action == null || user == null || acl == null)
      throw new IllegalArgumentException();

    // Check for the global and local admin role
    if (user.hasRole(GLOBAL_ADMIN_ROLE) || user.hasRole(org.getAdminRole()))
      return true;

    String[] userRoles = user.getRoles();
    for (AccessControlEntry entry : acl.getEntries()) {
      if (!action.equals(entry.getAction()))
        continue;

      String aceRole = entry.getRole();
      for (String role : userRoles) {
        if (!role.equals(aceRole))
          continue;

        return entry.isAllow();
      }
    }
    return false;
  }
}
