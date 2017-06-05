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

package org.opencastproject.userdirectory.utils;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Utility class for common operations.
 * This class is not visible to other OSGI bundles.
 */
public final class UserDirectoryUtils {

  /** Hidden constructor */
  private UserDirectoryUtils() { }

  /**
   * Return false if the current user hasn't an admin role and the roles list contain same role, true otherwise
   *
   * @param securityService the SecurityService
   * @param roles roles list to test
   * @return true if the roles list doesn't contain an admin role
   *            or if the current user is allowed to create, update or delete users or groups with the given roles
   */
  public static boolean isCurrentUserAuthorizedHandleRoles(SecurityService securityService, Set<Role> roles) {
    User user = securityService.getUser();
    if (user == null)
      return false;

    Organization org = user.getOrganization();

    for (Role role : roles) {
      if (StringUtils.equals(SecurityConstants.GLOBAL_ADMIN_ROLE, role.getName()))
        return user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE);

      if (org != null && StringUtils.equals(org.getAdminRole(), role.getName()))
        return user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE)
                || user.hasRole(org.getAdminRole());
    }
    return true;
  }
}
