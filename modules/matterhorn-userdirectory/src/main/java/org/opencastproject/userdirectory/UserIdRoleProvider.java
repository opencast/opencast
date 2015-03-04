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
package org.opencastproject.userdirectory;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.SmartIterator;

import com.google.common.base.CharMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The user id role provider assigns the user id role.
 */
public class UserIdRoleProvider implements RoleProvider {

  private static final String ROLE_USER_PREFIX = "ROLE_USER_";

  private static final CharMatcher SAFE_USERNAME = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('0', '9')).negate().precomputed();

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public static final String getUserIdRole(String userName) {
    String safeUserName = SAFE_USERNAME.replaceFrom(userName, "_");
    return ROLE_USER_PREFIX.concat(safeUserName.toUpperCase());
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    List<Role> roles = getRolesForUser(securityService.getUser().getUsername());
    return roles.iterator();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    Organization organization = securityService.getOrganization();
    List<Role> roles = new ArrayList<Role>();
    roles.add(new JaxbRole(getUserIdRole(userName), JaxbOrganization.fromOrganization(organization), "The user id role"));
    return Collections.unmodifiableList(roles);
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query))
        foundRoles.add(role);
    }
    return new SmartIterator<Role>(limit, offset).applyLimitAndOffset(foundRoles).iterator();
  }

  private static boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

}
